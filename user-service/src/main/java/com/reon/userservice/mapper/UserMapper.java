package com.reon.userservice.mapper;

import com.reon.userservice.dto.RegistrationRequest;
import com.reon.userservice.dto.response.RegistrationResponse;
import com.reon.userservice.dto.response.UserProfile;
import com.reon.userservice.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User mapToEntity(RegistrationRequest registrationRequest) {
        return User.builder()
                .name(registrationRequest.name())
                .email(registrationRequest.email())
                .password(registrationRequest.password())
                .build();
    }

    public RegistrationResponse mapToResponse(User user) {
        return RegistrationResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .build();
    }

    public UserProfile profileResponse(User user) {
        return UserProfile.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
