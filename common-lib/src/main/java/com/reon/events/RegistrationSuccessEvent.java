package com.reon.events;

import lombok.Builder;

@Builder
public record RegistrationSuccessEvent(
        String userId,
        String name,
        String email,
        String otp
) {
}
