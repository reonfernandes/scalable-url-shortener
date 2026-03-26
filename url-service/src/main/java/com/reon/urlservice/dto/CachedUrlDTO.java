package com.reon.urlservice.dto;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
public record CachedUrlDTO(
        String shortCode,
        String longUrl,
        String passwordHash,
        boolean active,
        LocalDateTime expiresAt
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
