package com.reon.userservice.service.impl;

import com.reon.events.RegistrationSuccessEvent;
import com.reon.exception.*;
import com.reon.userservice.dto.LoginRequest;
import com.reon.userservice.dto.RegistrationRequest;
import com.reon.userservice.dto.UpdateProfileRequest;
import com.reon.userservice.dto.response.LoginResponse;
import com.reon.userservice.dto.response.RegistrationResponse;
import com.reon.userservice.dto.response.UserProfile;
import com.reon.userservice.jwt.JwtService;
import com.reon.userservice.mapper.UserMapper;
import com.reon.userservice.model.User;
import com.reon.userservice.model.type.AuthProvider;
import com.reon.userservice.model.type.Role;
import com.reon.userservice.model.type.Tier;
import com.reon.userservice.repository.UserRepository;
import com.reon.userservice.service.CookieService;
import com.reon.userservice.service.OtpCache;
import com.reon.userservice.service.UserService;
import com.reon.userservice.utils.OTPGenerator;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

@Service
public class UserServiceImpl implements UserService {

    private final Long expirationTime;
    private final String registerSuccessTopic;
    private final Long duration;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CookieService cookieService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OtpCache otpCache;

    public UserServiceImpl(
            @Value("${security.jwt.expiration-time}") Long expirationTime,
            @Value("${security.kafka.topic.register}") String registerSuccessTopic,
            @Value("${security.cache.url-ttl-minutes}") Long duration,
            UserRepository userRepository, UserMapper userMapper, PasswordEncoder encoder, JwtService jwtService,
            AuthenticationManager authenticationManager, CookieService cookieService,
            KafkaTemplate<String, Object> kafkaTemplate, OtpCache otpCache
    ) {
        this.expirationTime = expirationTime;
        this.registerSuccessTopic = registerSuccessTopic;
        this.duration = duration;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.cookieService = cookieService;
        this.kafkaTemplate = kafkaTemplate;
        this.otpCache = otpCache;
    }

    @Override
    public RegistrationResponse registerUser(RegistrationRequest registrationRequest) {
        if (userRepository.existsByEmail(registrationRequest.email())) {
            log.warn("User Service :: User already exists");
            throw new EmailAlreadyExistsException("User already exists with this email");
        }

        User user = userMapper.mapToEntity(registrationRequest);
        user.setPassword(encoder.encode(registrationRequest.password()));
        user.setTier(Tier.FREE);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRole(EnumSet.of(Role.USER));

        User saveUser = userRepository.save(user);

        // TODO:: generate otp
        String otp = OTPGenerator.generateOTP();

        // TODO:: save the generated otp in redis cache
        otpCache.storeOtp(otp, saveUser.getEmail(), duration);

        // TODO:: publish event after successful registration: OTP [userId, name, email, otp]
        publishRegistrationEvent(saveUser, otp);

        return userMapper.mapToResponse(saveUser);
    }

    @Override
    @Transactional
    public void verifyOtp(String email, String otp) {
        // fetch user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            log.info("User already verified: {}", email);
            return;
        }

        String storedOtp = otpCache.getOtp(email);

        if (!storedOtp.equals(otp)) {
            log.warn("OTP is invalid.");
            throw new InvalidOtpException("OTP is invalid");
        }

        if (storedOtp == null) {
            log.warn("OTP as expired.");
            throw new OtpExpiredException("OTP as expired");
        }

        userRepository.verifyEmail(user.getUserId());
        userRepository.activateUser(user.getUserId());


        otpCache.deleteOtp(email);

        log.info("Email verified successfully for userId: {}", user.getUserId());
    }

    @Override
    public LoginResponse authenticateUser(LoginRequest loginRequest, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            if (userDetails != null){
                String accessToken = jwtService.generateToken((User) userDetails);

                ResponseCookie accessTokenCookie = cookieService.accessTokenCookie(accessToken);
                response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

                return LoginResponse.builder()
                        .accessToken(accessToken)
                        .expiresIn(expirationTime)
                        .build();
            }
        } catch (DisabledException exception) {
            log.info("UserService :: Disabled exception");
            throw new DisabledException("Account is disabled");
        } catch (AuthenticationException exception) {
            log.info("UserService :: Authentication exception: Invalid username or password");
            throw new BadCredentialsException("Invalid Credentials");
        }

        return LoginResponse.builder()
                .accessToken(null)
                .expiresIn(0)
                .build();
    }

    @Override
    public UserProfile fetchUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null){
            String loggedInUser = authentication.getName();

            User user = userRepository.findByEmail(loggedInUser).orElseThrow(
                    () -> new UserNotFoundException("User not found.")
            );

            if (loggedInUser != null){
                return userMapper.profileResponse(user);
            }
        }
        return null;
    }

    @Override
    public void updateUserProfile(UpdateProfileRequest request) {
        if (request != null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null){
                String loggedInUser = authentication.getName();

                User user = userRepository.findByEmail(loggedInUser).orElseThrow(
                        () -> new UserNotFoundException("User not found.")
                );

                if (request.name() != null && !request.name().isBlank()){
                    user.setName(request.name());
                }

                if (request.newPassword() != null && !request.newPassword().isBlank()) {
                    if (request.currentPassword() == null
                            || !encoder.matches(request.currentPassword(), user.getPassword())) {
                        throw new InvalidCredentialsException("Current password is incorrect");
                    }
                    user.setPassword(encoder.encode(request.newPassword()));
                }

                userRepository.save(user);
                log.info("Profile updated: userId={}", user.getUserId());

            }
        }
    }

    @Override
    public void deleteAccount(String userId) {
        User user = findIfUserIsActive(userId);
        if (user != null) {
            userRepository.delete(user);
            log.info("Account deleted: userId={}", userId);
        }
    }

    @Override
    @Transactional
    public void incrementUrlCountForUser(String userId) {
        User user = findIfUserIsActive(userId);
        if (user != null){
            userRepository.incrementUrlCount(user.getUserId());
        }
    }

    @Override
    @Transactional
    public void decrementUrlCountForUser(String userId) {
        User user = findIfUserIsActive(userId);
        if (user != null){
            userRepository.decrementUserUrlCount(user.getUserId());
        }
    }

    // admin specific methods
    @Override
    @Transactional
    public void deactivateAccount(String userId) {
        User user = findIfUserIsActive(userId);
        if (user != null){
            userRepository.deactivateUser(user.getUserId());
        }
    }

    @Override
    @Transactional
    public void activateAccount(String userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found.")
        );
        if (user != null){
            userRepository.activateUser(user.getUserId());
        }
    }

    @Override
    public Page<UserProfile> fetchAllUsers(int pageNo, int pageSize) {
        log.info("Fetching all users info from page: {}, pageSize: {}", pageNo, pageSize);
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::profileResponse);
    }

    // helper methods
    private User findIfUserIsActive(String userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    // publish event method
    private CompletableFuture<SendResult<String, Object>> publishRegistrationEvent(User user, String otp) {
        RegistrationSuccessEvent eventData = userMapper.publishRegistrationEvent(user, otp);
        CompletableFuture<SendResult<String, Object>> publishEvent =
                kafkaTemplate.send(registerSuccessTopic, user.getUserId(), eventData);

        publishEvent.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Kafka publish failed for userId: {}", user.getUserId(), exception);
            } else {
                log.info("Kafka event published successfully. topic: {}, partition: {}, offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
        return publishEvent;
    }
}
