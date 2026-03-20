package com.reon.userservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,                                // null = do not update

        String currentPassword,                     // Required only when changing password

        @Size(min = 8, message = "New password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).{8,}$",
                message = "New password must contain at least one uppercase letter, one digit, and one special character"
        )
        String newPassword                          // null = do not update

) {}