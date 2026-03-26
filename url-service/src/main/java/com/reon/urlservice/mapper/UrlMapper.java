package com.reon.urlservice.mapper;

import com.reon.urlservice.dto.CachedUrlDTO;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.urlservice.model.UrlMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UrlMapper {

    private final String baseUrl;

    public UrlMapper(@Value("${security.app.url.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public UrlResponse urlResponseToUser(UrlMapping urlMapping) {
        return UrlResponse.builder()
                .urlId(urlMapping.getUrlId())
                .userId(urlMapping.getUserId())
                .title(urlMapping.getTitle())
                .shortCode(urlMapping.getShortCode())
                .shortUrl(buildShortUrl(urlMapping.getShortCode()))
                .longUrl(urlMapping.getLongUrl())
                .clickCount(urlMapping.getClickCount())
                .isActive(urlMapping.isActive())
                .isPasswordProtected(urlMapping.getPasswordHash() != null)
                .createdAt(urlMapping.getCreatedAt())
                .expiresOn(urlMapping.getExpiresAt())
                .build();
    }

    // useful while caching
    public UrlResponse urlResponseToUser(CachedUrlDTO urlMapping) {
        return UrlResponse.builder()
                .shortCode(urlMapping.shortCode())
                .longUrl(urlMapping.longUrl())
                .isActive(urlMapping.active())
                .expiresOn(urlMapping.expiresAt())
                .isPasswordProtected(urlMapping.passwordHash() != null)
                .build();
    }

    private String buildShortUrl(String shortCode){
        return baseUrl + "/" + shortCode;
    }

}
