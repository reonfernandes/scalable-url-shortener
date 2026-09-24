package com.reon.userservice.mapper;

import com.reon.userservice.dto.RegistrationRequest;
import com.reon.userservice.dto.response.RegistrationResponse;
import com.reon.userservice.dto.response.UserProfile;
import com.reon.userservice.model.User;
import com.reon.userservice.model.type.Tier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    private final int freeTierLimit;
    private final int premiumTierLimit;

    public UserMapper(@Value("${security.quota.free-tier-limit}") int freeTierLimit,
                      @Value("${security.quota.premium-tier-limit}") int premiumTierLimit) {
        this.freeTierLimit = freeTierLimit;
        this.premiumTierLimit = premiumTierLimit;
    }

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

    private Integer getUrlCreationLimit(Tier tier) {
        return switch (tier) {
            case FREE -> freeTierLimit;
            case PREMIUM -> premiumTierLimit;
        };
    }
}
