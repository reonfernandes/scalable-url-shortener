package com.reon.analyticsservice.dto;

import lombok.Builder;
import java.util.Map;

@Builder
public record UrlStatsResponse(
        Long urlId,
        long totalClicks,
        Map<String, Long> clicksByBrowser,
        Map<String, Long> clicksByOs,
        Map<String, Long> clicksByCountry
) {
}
