package com.reon.userservice.service.impl;

import com.reon.events.AdminUserStateControlEvent;
import com.reon.events.UserAccountDeletedEvent;
import com.reon.exception.*;
import com.reon.exception.response.PageResponse;
import com.reon.userservice.dto.LoginRequest;
import com.reon.userservice.dto.RegistrationRequest;
import com.reon.userservice.dto.UpdateProfileRequest;
import com.reon.userservice.dto.response.LoginResponse;
import com.reon.userservice.dto.response.RegistrationResponse;
import com.reon.userservice.dto.response.UserProfile;
import com.reon.userservice.jwt.JwtService;
import com.reon.userservice.mapper.UserMapper;
import com.reon.userservice.model.User;
import com.reon.userservice.model.type.Role;
import com.reon.userservice.repository.UserRepository;
import com.reon.userservice.service.CookieService;
import com.reon.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
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

    private final String userAccountDeleteTopic;

    private final String adminStateTopic;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CookieService cookieService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final HttpServletRequest httpRequest;

    public UserServiceImpl(
            @Value("${security.jwt.expiration-time}") Long expirationTime,
            @Value("${security.kafka.topic.deleted}") String userAccountDeleteTopic,
            @Value("${security.kafka.topic.admin.userState}") String adminStateTopic,
            UserRepository userRepository, UserMapper userMapper, PasswordEncoder encoder, JwtService jwtService,
            AuthenticationManager authenticationManager, CookieService cookieService,
            KafkaTemplate<String, Object> kafkaTemplate, HttpServletRequest httpRequest
    ) {
        this.expirationTime = expirationTime;
        this.userAccountDeleteTopic = userAccountDeleteTopic;
        this.adminStateTopic = adminStateTopic;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.cookieService = cookieService;
        this.kafkaTemplate = kafkaTemplate;
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
        user.setRole(EnumSet.of(Role.USER));

        User saveUser = userRepository.save(user);
        log.info("User Service :: Newly created user profile saved successfully: {}", registrationRequest.email());

        return userMapper.mapToResponse(saveUser);
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

                // the token is only sent in the HttpOnly cookie, so page JavaScript can never read it
                return LoginResponse.builder()
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

        // userId comes from the request param, so it is never null; the header can be missing
        String headerUserId = httpRequest.getHeader("X-User-Id");
        if (userId.equals(headerUserId)) {
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
            throw new ForbiddenOperationException("You can only delete your own account.");
        }
    }

    // admin specific methods
    @Override
    @Transactional
    public void deactivateAccount(String userId) {
        log.info("User Service :: Deactivating user account: {}", userId);
        User user = findIfUserIsActive(userId);
        if (user != null){
            userRepository.deactivateUser(user.getUserId());
            publishUserStateEvent(userId, false);
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
            publishUserStateEvent(userId, true);
        }
        log.info("User Service :: Account Activated");
    }

    @Override
    public PageResponse<UserProfile> viewAllUsers(int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1) {
            throw new IllegalArgumentException("page and size must be 1 or more");
        }
        int size = Math.min(pageSize, PageResponse.MAX_PAGE_SIZE);

        log.info("User Service :: Retrieving users info from page:{} of size:{}", pageNo, size);
        // Spring Data counts pages from 0, our API counts from 1
        Pageable pageable = PageRequest.of(pageNo - 1, size);
        Page<UserProfile> users = userRepository.findAll(pageable)
                .map(userMapper::profileResponse);

        log.info("User Service :: Users data retrieval successful");
        return new PageResponse<>(users.getContent(), pageNo, size, users.getTotalElements(), users.getTotalPages());
    }

    // helper methods
    private User findIfUserIsActive(String userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    // publish event methods
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

    private void publishUserStateEvent(String userId, boolean state) {
        AdminUserStateControlEvent adminStateEvent = AdminUserStateControlEvent.builder()
                .userId(userId)
                .state(state)
                .build();
        CompletableFuture<SendResult<String, Object>> adminPublishedEvent =
                kafkaTemplate.send(adminStateTopic, userId, adminStateEvent);

        adminPublishedEvent.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Kafka publish failed for userId: {}", userId, exception);
            } else {
                log.info("Kafka admin event published successfully. topic: {}, partition: {}, offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
