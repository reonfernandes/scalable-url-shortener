package com.reon.userservice.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record UserProfile(
        String userId,
        String name,
        String email,
        // e.g. ["USER"] or ["ADMIN", "USER"]; the UI shows admin pages only to admins
        List<String> roles,
        boolean active,
        LocalDateTime createdAt
) {
}
