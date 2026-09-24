package com.reon.userservice.dto.response;

import lombok.Builder;

@Builder
public record UserProfile(
        String userId,
        String name,
        String email,
        int urlsCreated
) {
}
