package com.reon.userservice.dto.response;

import lombok.Builder;

@Builder
public record RegistrationResponse(
        String userId,
        String email
) {
}