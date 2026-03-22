package com.reon.urlservice.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UrlResponse(
        Long urlId,
        String userId,
        String title,
        String shortCode,
        String shortUrl,
        String longUrl,
        Long clickCount,
        boolean isActive,
        boolean isPasswordProtected,
        LocalDateTime createdAt,
        LocalDateTime expiresOn
) {
}