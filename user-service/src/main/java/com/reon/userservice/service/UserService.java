package com.reon.userservice.service;

import com.reon.userservice.dto.LoginRequest;
import com.reon.userservice.dto.RegistrationRequest;
import com.reon.userservice.dto.UpdateProfileRequest;
import com.reon.userservice.dto.response.LoginResponse;
import com.reon.userservice.dto.response.RegistrationResponse;
import com.reon.userservice.dto.response.UserListResponse;
import com.reon.userservice.dto.response.UserProfile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;

public interface UserService {
    RegistrationResponse registerUser(RegistrationRequest registrationRequest);
    LoginResponse authenticateUser(LoginRequest loginRequest, HttpServletResponse response);

    UserProfile fetchUserProfile();
    void updateUserProfile(UpdateProfileRequest request);

    void deleteAccount(String userId);

    void verifyOtp(String email, String otp);

    void incrementUrlCountForUser(String userId);
    void decrementUrlCountForUser(String userId);

    void deactivateAccount(String userId);
    void activateAccount(String userId);
    Page<UserListResponse> viewAllUsers(int pageNo, int pageSize);
}
