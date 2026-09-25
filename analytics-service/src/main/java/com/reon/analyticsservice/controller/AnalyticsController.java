package com.reon.analyticsservice.controller;

import com.reon.analyticsservice.dto.UrlStatsResponse;
import com.reon.analyticsservice.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // total clicks for several links in one call (the dashboard list): ?urlIds=1,2,3
    @GetMapping("/clicks")
    public ResponseEntity<Map<Long, Long>> getClickCounts(@RequestParam("urlIds") List<Long> urlIds,
                                                         @RequestHeader("X-User-Id") String userId) {
        log.info("Analytics Controller :: Fetching click counts for {} links, userId: {}", urlIds.size(), userId);
        return ResponseEntity.ok(analyticsService.getClickCounts(urlIds, userId));
    }

    @GetMapping("/{urlId}")
    public ResponseEntity<UrlStatsResponse> getStats(@PathVariable("urlId") Long urlId,
                                                     @RequestHeader("X-User-Id") String userId) {
        log.info("Analytics Controller :: Fetching stats for urlId: {}, userId: {}", urlId, userId);
        return ResponseEntity.ok(analyticsService.getStatsForUrl(urlId, userId));
    }
}
