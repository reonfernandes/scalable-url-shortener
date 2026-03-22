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
import org.springframework.security.access.prepost.PreAuthorize;
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
        RegistrationResponse response = userService.registerUser(registrationRequest);
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
        log.info("Authentication request for: {}", loginRequest.email());
        LoginResponse userDetails = userService.authenticateUser(loginRequest, response);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Authentication successful",
                        userDetails
                ));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfile>> profile(){
        log.info("Fetching profile");
        UserProfile profile = userService.fetchUserProfile();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Success",
                        profile
                        )
                );
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PatchMapping("/me/update")
    public ResponseEntity<ApiResponse<Void>> updateProfile(@Valid @RequestBody UpdateProfileRequest request){
        log.info("Updating profile");
        userService.updateUserProfile(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Profile updated successfully"
                ));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/me/delete")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@RequestParam(name = "userId") String userId){
        log.info("Deleting account");
        userService.deleteAccount(userId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.of(
                        HttpStatus.NO_CONTENT,
                        "Account deleted successfully"
                ));
    }

    @PostMapping("/url/increase-count")
    public ResponseEntity<ApiResponse<Void>> increaseUrlCount(@RequestParam("userId") String userId) {
        log.info("Incrementing url count");
        userService.incrementUrlCountForUser(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Url count increased"
                ));
    }

    @PostMapping("/url/decrease-count")
    public ResponseEntity<ApiResponse<Void>> decreaseUrlCount(@RequestParam("userId") String userId) {
        log.info("Decrementing url count");
        userService.decrementUrlCountForUser(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Url count decreased"
                ));
    }
}
