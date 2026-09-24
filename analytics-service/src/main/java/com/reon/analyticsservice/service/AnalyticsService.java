package com.reon.analyticsservice.service;

import com.reon.analyticsservice.dto.StatEntry;
import com.reon.analyticsservice.dto.UrlStatsResponse;
import com.reon.analyticsservice.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    // only counts clicks on links owned by this user, so nobody can read other users' stats
    public UrlStatsResponse getStatsForUrl(String shortCode, String userId) {
        long totalClicks = analyticsRepository.countByShortCodeAndUserId(shortCode, userId);
        
        Map<String, Long> clicksByBrowser = analyticsRepository.getBrowserStats(shortCode, userId)
                .stream().collect(Collectors.toMap(
                        entry -> entry.getKey() != null ? entry.getKey() : "Unknown",
                        StatEntry::getValue,
                        Long::sum
                ));

        Map<String, Long> clicksByOs = analyticsRepository.getOsStats(shortCode, userId)
                .stream().collect(Collectors.toMap(
                        entry -> entry.getKey() != null ? entry.getKey() : "Unknown",
                        StatEntry::getValue,
                        Long::sum
                ));

        Map<String, Long> clicksByCountry = analyticsRepository.getCountryStats(shortCode, userId)
                .stream().collect(Collectors.toMap(
                        entry -> entry.getKey() != null ? entry.getKey() : "Unknown",
                        StatEntry::getValue,
                        Long::sum
                ));

        return UrlStatsResponse.builder()
                .shortCode(shortCode)
                .totalClicks(totalClicks)
                .clicksByBrowser(clicksByBrowser)
                .clicksByOs(clicksByOs)
                .clicksByCountry(clicksByCountry)
                .build();
    }
}
