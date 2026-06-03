package com.reon.urlservice.dto;

import lombok.Builder;

@Builder
public record RedirectRequest(
        String shortCode,
        String password,
        String ipAddress,
        String userAgent,
        String referrer
) {}
