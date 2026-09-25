package com.reon.analyticsservice.service;

import com.reon.analyticsservice.dto.StatEntry;
import com.reon.analyticsservice.dto.UrlStatsResponse;
import com.reon.analyticsservice.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    // only counts clicks on links owned by this user, so nobody can read other users' stats
    public UrlStatsResponse getStatsForUrl(Long urlId, String userId) {
        // click events store the urlId as text
        String id = String.valueOf(urlId);

        return UrlStatsResponse.builder()
                .urlId(urlId)
                .totalClicks(analyticsRepository.countByUrlIdAndUserId(id, userId))
                .clicksByBrowser(toMap(analyticsRepository.getBrowserStats(id, userId)))
                .clicksByOs(toMap(analyticsRepository.getOsStats(id, userId)))
                .clicksByCountry(toMap(analyticsRepository.getCountryStats(id, userId)))
                .build();
    }

    private Map<String, Long> toMap(List<StatEntry> entries) {
        return entries.stream().collect(Collectors.toMap(
                entry -> entry.getKey() != null ? entry.getKey() : "Unknown",
                StatEntry::getValue,
                Long::sum
        ));
    }
}
