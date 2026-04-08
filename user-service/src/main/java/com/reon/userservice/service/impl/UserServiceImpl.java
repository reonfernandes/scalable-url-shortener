package com.reon.userservice.service.impl;

import com.reon.events.RegistrationSuccessEvent;
import com.reon.events.UserAccountDeletedEvent;
import com.reon.exception.*;
import com.reon.userservice.dto.LoginRequest;
import com.reon.userservice.dto.RegistrationRequest;
import com.reon.userservice.dto.UpdateProfileRequest;
import com.reon.userservice.dto.response.LoginResponse;
import com.reon.userservice.dto.response.RegistrationResponse;
import com.reon.userservice.dto.response.UserListResponse;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class UserServiceImpl implements UserService {

    private final Long expirationTime;
    private final Long duration;

    private final String registerSuccessTopic;
    private final String userAccountDeleteTopic;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CookieService cookieService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OtpCache otpCache;
    private final HttpServletRequest httpRequest;

    public UserServiceImpl(
            @Value("${security.jwt.expiration-time}") Long expirationTime,
            @Value("${security.cache.url-ttl-minutes}") Long duration,
            @Value("${security.kafka.topic.register}") String registerSuccessTopic,
            @Value("${security.kafka.topic.deleted}") String userAccountDeleteTopic,
            UserRepository userRepository, UserMapper userMapper, PasswordEncoder encoder, JwtService jwtService,
            AuthenticationManager authenticationManager, CookieService cookieService,
            KafkaTemplate<String, Object> kafkaTemplate, OtpCache otpCache, HttpServletRequest httpRequest
    ) {
        this.expirationTime = expirationTime;
        this.registerSuccessTopic = registerSuccessTopic;
        this.duration = duration;
        this.userAccountDeleteTopic = userAccountDeleteTopic;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.cookieService = cookieService;
        this.kafkaTemplate = kafkaTemplate;
        this.otpCache = otpCache;
        this.httpRequest = httpRequest;
    }

    @Override
    public RegistrationResponse registerUser(RegistrationRequest registrationRequest) {
        if (userRepository.existsByEmail(registrationRequest.email())) {
            log.warn("User Service :: User already exists");
            throw new EmailAlreadyExistsException("User already exists with this email");
        }

        log.info("User Service :: Creating new user:{} profile", registrationRequest.email());

        User user = userMapper.mapToEntity(registrationRequest);
        user.setPassword(encoder.encode(registrationRequest.password()));
        user.setTier(Tier.FREE);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRole(EnumSet.of(Role.USER));

        User saveUser = userRepository.save(user);
        log.info("User Service :: Newly created user profile saved successfully: {}", registrationRequest.email());

        // generate otp
        String otp = OTPGenerator.generateOTP();

        // save the generated otp in redis cache
        otpCache.storeOtp(otp, saveUser.getEmail(), duration);

        log.info("User Service :: Publishing event for successful user registration: {}", registrationRequest.email());
        // publish event after successful registration: OTP [userId, name, email, otp]
        publishRegistrationEvent(saveUser, otp);
        log.info("User Service :: Event published for successful user registration: {}", registrationRequest.email());

        return userMapper.mapToResponse(saveUser);
    }

    @Override
    @Transactional
    public void verifyOtp(String email, String otp) {

        log.info("User Service :: Verifying Otp for user: {}", email);

        // fetch user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            log.info("User already verified: {}", email);
            throw new UserAlreadyVerifiedException("User is already verified");
        }

        String storedOtp = otpCache.getOtp(email);

        if (storedOtp == null) {
            log.warn("OTP not found or expired for email: {}", email);
            throw new OtpExpiredException("OTP has expired or does not exist");
        }

        if (!encoder.matches(otp, storedOtp)) {
            log.warn("Invalid OTP attempt for email: {}", email);
            throw new InvalidOtpException("OTP is invalid");
        }

        userRepository.verifyEmail(user.getUserId());
        userRepository.activateUser(user.getUserId());

        otpCache.deleteOtp(email);

        log.info("User Service : Email verified successfully for userId: {}", user.getUserId());
    }

    @Override
    public LoginResponse authenticateUser(LoginRequest loginRequest, HttpServletResponse response) {
        try {
            log.info("User Service :: Authenticating User: {}", loginRequest.email());

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

            log.info("User Service :: User authenticated successfully");

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
        log.info("User Service :: Fetching User Profile");

        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null) throw new UserNotFoundException("User not found.");

        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found.")
        );

        log.info("User profile fetched");
        return userMapper.profileResponse(user);
    }

    @Override
    public void updateUserProfile(UpdateProfileRequest request) {
        if (request == null) return;

        String userId = httpRequest.getHeader("X-User-Id");

        log.info("User Service :: Updating user profile: {}", userId);
        if (userId == null) throw new UserNotFoundException("User not found.");

        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found.")
        );

        if (request.name() != null && !request.name().isBlank()) {
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
        log.info("User Service :: Profile updated: userId={}", userId);
    }

    @Override
    public void deleteAccount(String userId) {
        log.warn("User Service :: Deleting user profile: Id: {}", userId);

        String headerUserId = httpRequest.getHeader("X-User-Id");
        if (headerUserId.equals(userId)) {
            User user = findIfUserIsActive(userId);
            if (user != null) {
                userRepository.delete(user);
                log.info("Account deleted: userId={}", userId);

                // publish event
                publishUserAccountDeletionEvent(userId);
                log.info("User Service :: Event for user account deletion successful: {}", userId);
            }
            log.warn("User Service :: Profile deleted");
        } else {
            throw new IllegalArgumentException("You cannot perform this operation.");
        }
    }

    @Override
    @Transactional
    public void incrementUrlCountForUser(String userId) {
        log.info("User Service :: Feign call: Incrementing Url count for user: {}", userId);
        User user = findIfUserIsActive(userId);
        if (user != null){
            userRepository.incrementUrlCount(user.getUserId());
        }
        log.info("User Service :: Url count incremented");
    }

    @Override
    @Transactional
    public void decrementUrlCountForUser(String userId) {
        log.info("User Service :: Feign call: Decrementing Url count for user: {}", userId);

        User user = findIfUserIsActive(userId);
        if (user != null){
            userRepository.decrementUserUrlCount(user.getUserId());
        }
        log.info("User Service :: Url count decremented");
    }

    // admin specific methods
    @Override
    @Transactional
    public void deactivateAccount(String userId) {
        log.info("User Service :: Deactivating user account: {}", userId);
        User user = findIfUserIsActive(userId);
        if (user != null){
            userRepository.deactivateUser(user.getUserId());
        }
        log.info("User Service :: Account deactivated");
    }

    @Override
    @Transactional
    public void activateAccount(String userId) {
        log.info("User Service :: Activating user account: {}", userId);
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found.")
        );
        if (user != null){
            userRepository.activateUser(user.getUserId());
        }
        log.info("User Service :: Account Activated");
    }

    @Override
    public Page<UserListResponse> viewAllUsers(int pageNo, int pageSize) {
        log.info("User Service :: Retrieving users info from page:{} of size:{}", pageNo, pageSize);
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        Page<User> users = userRepository.findAll(pageable);

        List<UserProfile> userResponse = users.getContent()
                .stream()
                .map(userMapper::profileResponse)
                .toList();

        UserListResponse userListResponse = UserListResponse.builder()
                .total((int) users.getTotalElements())
                .userProfileList(userResponse)
                .build();

        log.info("User Service :: Users data retrieval successful");
        return new PageImpl<>(List.of(userListResponse), pageable, userListResponse.total());
    }

    // helper methods
    private User findIfUserIsActive(String userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    // publish event method
    private void publishRegistrationEvent(User user, String otp) {
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
    }

    private void publishUserAccountDeletionEvent(String userId) {
        UserAccountDeletedEvent deletedEvent = UserAccountDeletedEvent.builder()
                .userId(userId)
                .build();
        CompletableFuture<SendResult<String, Object>> publishEvent =
                kafkaTemplate.send(userAccountDeleteTopic, deletedEvent);

        publishEvent.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Kafka publish failed for userId: {}", userId, exception);
            } else {
                log.info("Kafka event[delete] published successfully. topic: {}, partition: {}, offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
