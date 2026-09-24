package com.reon.userservice.controller;

import com.reon.exception.response.ApiResponse;
import com.reon.userservice.dto.LoginRequest;
import com.reon.userservice.dto.RegistrationRequest;
import com.reon.userservice.dto.UpdateProfileRequest;
import com.reon.userservice.dto.response.LoginResponse;
import com.reon.userservice.dto.response.RegistrationResponse;
import com.reon.userservice.dto.response.UserProfile;
import com.reon.userservice.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> otpVerification(@RequestParam(name = "email") String email,
                                                             @RequestParam(name = "otp") String otp) {
        log.info("User Controller :: Incoming request for Otp verification: {}", email);
        userService.verifyOtp(email, otp);
        log.info("User Controller :: Outgoing request: Account verified successfully");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Verification successful"
                ));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestParam(name = "email") String email) {
        log.info("User Controller :: Incoming request for resending Otp: {}", email);
        userService.resendOtp(email);
        log.info("User Controller :: Outgoing request: Otp resent successfully");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "A new OTP has been sent to your email"
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

    @DeleteMapping("/me/delete")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@RequestParam(name = "userId") String userId){
        log.warn("User Controller :: Incoming request for deleting account: {}", userId);
        userService.deleteAccount(userId);
        log.warn("User Controller :: Outgoing request: Account deleted.");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Account deleted successfully"
                ));
    }

    @PostMapping("/url/increase-count")
    public ResponseEntity<ApiResponse<Void>> increaseUrlCount(@RequestParam("userId") String userId) {
        log.info("User Controller :: Incoming request for incrementing url count for user: {}", userId);
        userService.incrementUrlCountForUser(userId);
        log.info("User Controller :: Outgoing request: Url count incremented");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Url count increased"
                ));
    }

    @PostMapping("/url/decrease-count")
    public ResponseEntity<ApiResponse<Void>> decreaseUrlCount(@RequestParam("userId") String userId) {
        log.info("User Controller :: Incoming request for decrementing url count");
        userService.decrementUrlCountForUser(userId);
        log.info("User Controller :: Outgoing request: Url count decremented");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Url count decreased"
                ));
    }
}
