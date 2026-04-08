package com.reon.urlservice.service.impl;

import com.reon.exception.*;
import com.reon.urlservice.common.Base62Encoder;
import com.reon.urlservice.dto.UpdateUrlRequest;
import com.reon.urlservice.dto.UrlRequest;
import com.reon.urlservice.dto.response.UrlListResponse;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.urlservice.mapper.UrlMapper;
import com.reon.urlservice.model.UrlMapping;
import com.reon.urlservice.respository.UrlRepository;
import com.reon.urlservice.service.UrlCacheService;
import com.reon.urlservice.service.UrlClient;
import com.reon.urlservice.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UrlServiceImpl implements UrlService {

    private final int freeTierLimit;
    private final int premiumTierLimit;


    private final Logger log = LoggerFactory.getLogger(UrlServiceImpl.class);
    private final UrlRepository urlRepository;
    private final UrlMapper urlMapper;
    private final PasswordEncoder encoder;
    private final UrlClient urlClient;
    private final UrlCacheService urlCacheService;
    private final HttpServletRequest httpRequest;

    public UrlServiceImpl(
            @Value("${security.app.quota.free-tier-limit}") int freeTierLimit,
            @Value("${security.app.quota.premium-tier-limit}") int premiumTierLimit,
            UrlRepository urlRepository, UrlMapper urlMapper, PasswordEncoder encoder,
            UrlClient urlClient, UrlCacheService urlCacheService, HttpServletRequest httpRequest) {
        this.freeTierLimit = freeTierLimit;
        this.premiumTierLimit = premiumTierLimit;
        this.urlRepository = urlRepository;
        this.urlMapper = urlMapper;
        this.encoder = encoder;
        this.urlClient = urlClient;
        this.urlCacheService = urlCacheService;
        this.httpRequest = httpRequest;
    }

    @Override
    public UrlResponse shortenUrl(UrlRequest urlRequest) {
        log.info("URL Service :: Processing new short url generation");
        Map<String, String> userTier = checkForUserTier();

        String userId = userTier.get("userId");
        String tier = userTier.get("tier");


        // Check quota based on tier: limit
        long currentCount = urlRepository.countByUserIdAndActiveTrue(userId);
        int limit = tier.equals("PREMIUM") ? premiumTierLimit : freeTierLimit;
        if (currentCount >= limit) {
            // publish quota.exceeded Kafka event → Notification Service sends email
            log.info("Tier: {}, URL limit exceeded: {}",tier, limit);
            throw new UrlQuotaExceededException("URL limit reached for your plan");
        }


        // check for custom alias
        String requestedAlias = urlRequest.customAlias();
        if (urlRepository.existsByShortCode(requestedAlias)) {
            log.info("Custom Alias: {}, not available", requestedAlias);
            throw new AliasAlreadyTakenException("Custom alias not available.");
        }

        UrlMapping saveUrl = buildAndSaveUrl(urlRequest, userId);

        // feign call to user service to update the url count field.
        urlClient.increaseUrlCount(userId);

        log.info("URL Service :: Short URL created — shortCode: {}, userId: {}", saveUrl.getShortCode(), userId);
        return urlMapper.urlResponseToUser(saveUrl);
    }

    @Override
    public void deleteUrl(Long urlId) {
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null) throw new UnauthorizedUrlAccessException();

        log.warn("URL Service :: Deleting url with id: {}", urlId);
        UrlMapping url = urlRepository.findById(String.valueOf(urlId)).orElseThrow(
                () -> new UrlNotFoundException("URL not found with id: " + urlId)
        );

        if (userId.equals(url.getUserId())) {
            urlCacheService.evict(url.getShortCode());
            urlRepository.delete(url);
            urlClient.decreaseUrlCount(url.getUserId());
        } else {
            throw new UnauthorizedUrlAccessException();
        }
        log.warn("URl Service :: Url deleted.");
    }

    @Override
    @Transactional
    public void deleteUserUrls(String userId) {
        urlRepository.deleteUserUrls(userId);
        log.info("URL Service :: Urls deleted for user: {}", userId);
    }

    @Override
    public Page<UrlListResponse> viewAllUrls(int page, int size) {
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null) throw new UnauthorizedUrlAccessException();

        log.info("URL Service :: Fetching urls for userId: {}, page: {}, size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<UrlMapping> mappings = urlRepository.findByUserId(userId, pageable);

        List<UrlResponse> urlResponses = mappings.getContent()
                .stream()
                .map(urlMapper::urlResponseToUser)
                .toList();

        UrlListResponse urlListResponse = UrlListResponse.builder()
                .total((int) mappings.getTotalElements())
                .urlResponseList(urlResponses)
                .build();

        log.info("Url Service :: Urls data retrieval successful");
        return new PageImpl<>(List.of(urlListResponse), pageable, mappings.getTotalElements());
    }

    @Override
    public void updateShortenedUrl(Long urlId, UpdateUrlRequest updateUrlRequest) {
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null) throw new UnauthorizedUrlAccessException();

        log.info("URL Service :: Updating url for user: {}", userId);

        UrlMapping urlMapping = urlRepository.findById(String.valueOf(urlId)).orElseThrow(
                () -> new UrlNotFoundException("URL not found with id: " + urlId)
        );

        if (!urlMapping.getUserId().equals(userId)) {
            throw new UnauthorizedUrlAccessException();
        }

        // capture the shortCode before any changes made to alias - old key in redis
        String shortCodeToEvict = urlMapping.getShortCode();

        if (updateUrlRequest.title() != null && !updateUrlRequest.title().isBlank()) {
            urlMapping.setTitle(updateUrlRequest.title());
        }

        if (updateUrlRequest.longUrl() != null && !updateUrlRequest.longUrl().isBlank()) {
            urlMapping.setLongUrl(updateUrlRequest.longUrl());
        }

        if (updateUrlRequest.customAlias() != null && !updateUrlRequest.customAlias().isBlank()) {
            String alias = updateUrlRequest.customAlias();

            if (!alias.equals(urlMapping.getShortCode()) && urlRepository.existsByShortCode(alias)) {
                log.info("Custom Alias: {} already taken", alias);
                throw new AliasAlreadyTakenException("Custom alias not available.");
            }
            urlMapping.setShortCode(alias);
        }

        if (updateUrlRequest.expiresAt() != null) {
            urlMapping.setExpiresAt(updateUrlRequest.expiresAt());
        }

        if (updateUrlRequest.password() != null && !updateUrlRequest.password().isBlank()) {
            String hashed = encoder.encode(updateUrlRequest.password());
            urlMapping.setPasswordHash(hashed);
        }

        urlRepository.save(urlMapping);
        urlCacheService.evict(shortCodeToEvict);

        log.info("URL Service :: Updated URL with id: {}", urlId);
    }

    // helper methods
    private UrlMapping buildAndSaveUrl(UrlRequest urlRequest, String userId) {
        boolean isUrlPasswordProtected = urlRequest.password() != null && !urlRequest.password().isBlank();
        String hashedUrl = null;
        if (isUrlPasswordProtected) {
            hashedUrl = encoder.encode(urlRequest.password());
        }

        UrlMapping url = UrlMapping.builder()
                .userId(userId)
                .title(urlRequest.title())
                .longUrl(urlRequest.longUrl())
                .passwordHash(hashedUrl)
                .expiresAt(urlRequest.expiresAt())
                .build();

        if (urlRequest.customAlias() != null && !urlRequest.customAlias().isBlank()) {
            url.setShortCode(urlRequest.customAlias());
            return urlRepository.save(url);
        } else {
            UrlMapping savedUrl = urlRepository.save(url);
            savedUrl.setShortCode(Base62Encoder.encode(savedUrl.getUrlId()));
            return urlRepository.save(savedUrl);
        }
    }

    private Map<String, String> checkForUserTier() {
        String userId = httpRequest.getHeader("X-User-Id");
        String tier = httpRequest.getHeader("X-User-Tier");

        if (userId == null || tier == null) {
            throw new UnauthorizedUrlAccessException();
        }
        return Map.of(
                "userId", userId,
                "tier", tier
        );
    }
}
