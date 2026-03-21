package com.reon.urlservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Builder
public record UrlRequest(
        @Size(max = 50, message = "Title cannot exceed 50 characters")
        String title,

        @NotBlank(message = "Long URL must not be blank")
        @URL(message = "Must be a valid URL")
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        String longUrl,

        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]{3,30}$",
                message = "Custom alias must be 3–30 characters: letters, numbers, or hyphens only"
        )
        String customAlias,

        @Future(message = "Expiry date must be in the future")
        LocalDateTime expiresAt,

        @Size(min = 4, max = 72, message = "Password must be between 4 and 72 characters")
        String password
) {
}
