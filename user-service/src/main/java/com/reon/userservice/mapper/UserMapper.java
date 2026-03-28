package com.reon.userservice.mapper;

import com.reon.events.RegistrationSuccessEvent;
import com.reon.userservice.dto.RegistrationRequest;
import com.reon.userservice.dto.response.RegistrationResponse;
import com.reon.userservice.dto.response.UserProfile;
import com.reon.userservice.model.User;
import com.reon.userservice.model.type.Tier;
import com.reon.userservice.utils.OTPGenerator;
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
                .tier(user.getTier())
                .build();
    }

    public UserProfile profileResponse(User user) {
        return UserProfile.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .tier(user.getTier())
                .urlsCreated(user.getUrlCount())
                .urlCreationLimit(getUrlCreationLimit(user.getTier()))
                .build();
    }

    public RegistrationSuccessEvent publishRegistrationEvent(User user, String otp) {
        return RegistrationSuccessEvent.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .otp(otp)
                .build();
    }

    private Integer getUrlCreationLimit(Tier tier) {
        return switch (tier) {
            case FREE -> 50;
            case PREMIUM -> null;
        };
    }
}
