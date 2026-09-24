package com.reon.userservice.dto.response;

import lombok.Builder;

@Builder
public record LoginResponse(
        long expiresIn
) {}
