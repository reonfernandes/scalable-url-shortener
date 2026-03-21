package com.reon.urlservice.mapper;

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

    private String buildShortUrl(String shortCode){
        return baseUrl + "/" + shortCode;
    }

}
