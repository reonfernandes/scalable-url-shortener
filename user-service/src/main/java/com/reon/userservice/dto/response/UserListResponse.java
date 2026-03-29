package com.reon.userservice.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record UserListResponse(
        int total,
        List<UserProfile> userProfileList
) {
}
