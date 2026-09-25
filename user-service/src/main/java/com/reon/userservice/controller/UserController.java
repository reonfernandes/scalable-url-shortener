package com.reon.userservice.controller;

import com.reon.exception.response.ApiResponse;
import com.reon.userservice.dto.LoginRequest;
import com.reon.userservice.dto.RegistrationRequest;
import com.reon.userservice.dto.UpdateProfileRequest;
import com.reon.userservice.dto.response.LoginResponse;
import com.reon.userservice.dto.response.RegistrationResponse;
import com.reon.userservice.dto.response.UserProfile;
import com.reon.userservice.service.CookieService;
import com.reon.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final UserService userService;
    private final CookieService cookieService;

    public UserController(UserService userService, CookieService cookieService) {
        this.userService = userService;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationResponse>> generateNewUser(@Valid @RequestBody RegistrationRequest registrationRequest){
        log.info("User Controller :: Incoming request for generating new user: {}", registrationRequest.email());
        RegistrationResponse response = userService.registerUser(registrationRequest);
        log.info("User Controller :: Outgoing request: Account created success");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "Account created successfully.",
                        response
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> authentication(
            @Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response){
        log.info("User Controller :: Incoming authentication request for user: {}", loginRequest.email());
        LoginResponse userDetails = userService.authenticateUser(loginRequest, response);
        log.info("User Controller :: Outgoing request: Authentication successful");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Authentication successful",
                        userDetails
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        log.info("User Controller :: Incoming request for logout");
        // Clearing the cookie isn't enough: a copied token would still work until it expires.
        cookieService.readAccessToken(request).ifPresent(userService::logout);
        ResponseCookie clearedCookie = cookieService.clearAccessTokenCookie();
        log.info("User Controller :: Outgoing request: Logged out");

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Logged out successfully"
                ));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfile>> profile(){
        log.info("User Controller :: Incoming request for fetching profile");
        UserProfile profile = userService.fetchUserProfile();
        log.info("User Controller :: Outgoing request: Profile fetched successfully");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Success",
                        profile
                        )
                );
    }

    @PatchMapping("/me/update")
    public ResponseEntity<ApiResponse<Void>> updateProfile(@Valid @RequestBody UpdateProfileRequest request){
        log.info("User Controller :: Incoming request for updating profile");
        userService.updateUserProfile(request);
        log.info("User Controller :: Outgoing request: Profile updated successfully");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Profile updated successfully"
                ));
    }

    // deletes the logged-in user's account; the gateway sends their id in X-User-Id
    @DeleteMapping("/me/delete")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(){
        log.warn("User Controller :: Incoming request for deleting account");
        userService.deleteAccount();
        log.warn("User Controller :: Outgoing request: Account deleted.");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Account deleted successfully"
                ));
    }
}
