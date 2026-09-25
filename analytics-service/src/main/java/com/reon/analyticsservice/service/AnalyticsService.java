package com.reon.analyticsservice.service;

import com.reon.analyticsservice.dto.StatEntry;
import com.reon.analyticsservice.dto.UrlStatsResponse;
import com.reon.analyticsservice.repository.AnalyticsRepository;
import com.reon.exception.response.PageResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
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

    // total clicks per link, e.g. { 1: 12, 2: 0 }; links without clicks get 0
    public Map<Long, Long> getClickCounts(List<Long> urlIds, String userId) {
        if (urlIds.size() > PageResponse.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("At most " + PageResponse.MAX_PAGE_SIZE + " urlIds per request");
        }

        Map<Long, Long> counts = new LinkedHashMap<>();
        urlIds.forEach(urlId -> counts.put(urlId, 0L));

        List<String> ids = urlIds.stream().map(String::valueOf).toList();
        analyticsRepository.countClicksPerUrl(ids, userId)
                .forEach(entry -> counts.put(Long.valueOf(entry.getKey()), entry.getValue()));
        return counts;
    }

    private Map<String, Long> toMap(List<StatEntry> entries) {
        return entries.stream().collect(Collectors.toMap(
                entry -> entry.getKey() != null ? entry.getKey() : "Unknown",
                StatEntry::getValue,
                Long::sum
        ));
    }
}
