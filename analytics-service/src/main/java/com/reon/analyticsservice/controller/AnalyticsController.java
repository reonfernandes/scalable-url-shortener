package com.reon.analyticsservice.controller;

import com.reon.analyticsservice.dto.UrlStatsResponse;
import com.reon.analyticsservice.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlStatsResponse> getStats(@PathVariable("shortCode") String shortCode) {
        log.info("Analytics Controller :: Fetching stats for shortCode: {}", shortCode);
        return ResponseEntity.ok(analyticsService.getStatsForUrl(shortCode));
    }
}
