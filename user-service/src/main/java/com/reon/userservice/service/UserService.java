package com.reon.userservice.service;

import com.reon.userservice.dto.LoginRequest;
import com.reon.userservice.dto.RegistrationRequest;
import com.reon.userservice.dto.UpdateProfileRequest;
import com.reon.userservice.dto.response.LoginResponse;
import com.reon.userservice.dto.response.RegistrationResponse;
import com.reon.userservice.dto.response.UserProfile;
import jakarta.servlet.http.HttpServletResponse;
import com.reon.exception.response.PageResponse;

public interface UserService {
    RegistrationResponse registerUser(RegistrationRequest registrationRequest);
    LoginResponse authenticateUser(LoginRequest loginRequest, HttpServletResponse response);

    UserProfile fetchUserProfile();
    void updateUserProfile(UpdateProfileRequest request);

    void deleteAccount(String userId);
    /** Cancels the given token so it can't be used again, even before it expires. */
    void logout(String accessToken);



    void deactivateAccount(String userId);
    void activateAccount(String userId);
    PageResponse<UserProfile> viewAllUsers(int pageNo, int pageSize);
}
