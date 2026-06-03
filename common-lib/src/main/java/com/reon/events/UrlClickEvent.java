package com.reon.events;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record UrlClickEvent(
        String shortCode,
        String urlId,
        String userId,
        String ipAddress,
        String userAgent,
        String referrer,
        LocalDateTime clickedAt
) {
}
