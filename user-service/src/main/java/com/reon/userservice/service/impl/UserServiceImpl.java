package com.reon.userservice.service.impl;

import com.reon.exception.EmailAlreadyExistsException;
import com.reon.exception.InvalidCredentialsException;
import com.reon.exception.UserNotFoundException;
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
import com.reon.userservice.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

@Service
public class UserServiceImpl implements UserService {

    private final Long expirationTime;

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CookieService cookieService;

    public UserServiceImpl(
            @Value("${security.jwt.expiration-time}") Long expirationTime,
            UserRepository userRepository, UserMapper userMapper, PasswordEncoder encoder, JwtService jwtService,
            AuthenticationManager authenticationManager, CookieService cookieService
    ) {
        this.expirationTime = expirationTime;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.cookieService = cookieService;
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
        user.setEmailVerified(true);

        User saveUser = userRepository.save(user);
        return userMapper.mapToResponse(saveUser);
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
    public void deactiveAccount(String userId) {
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

    private User findIfUserIsActive(String userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
