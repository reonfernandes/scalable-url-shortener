package com.reon.urlservice.service.impl;

import com.reon.exception.*;
import com.reon.exception.response.PageResponse;
import com.reon.urlservice.common.Base62Encoder;
import com.reon.urlservice.dto.UpdateUrlRequest;
import com.reon.urlservice.dto.UrlRequest;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.urlservice.mapper.UrlMapper;
import com.reon.urlservice.model.UrlMapping;
import com.reon.urlservice.repository.UrlRepository;
import com.reon.urlservice.service.UrlCacheService;
import com.reon.urlservice.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class UrlServiceImpl implements UrlService {
    private static final int RANDOM_CODE_LENGTH = 7;

    private final Logger log = LoggerFactory.getLogger(UrlServiceImpl.class);
    private final UrlRepository urlRepository;
    private final UrlMapper urlMapper;
    private final PasswordEncoder encoder;
    private final UrlCacheService urlCacheService;
    private final HttpServletRequest httpRequest;

    public UrlServiceImpl(
            UrlRepository urlRepository, UrlMapper urlMapper, PasswordEncoder encoder,
            UrlCacheService urlCacheService, HttpServletRequest httpRequest) {
        this.urlRepository = urlRepository;
        this.urlMapper = urlMapper;
        this.encoder = encoder;
        this.urlCacheService = urlCacheService;
        this.httpRequest = httpRequest;
    }

    @Override
    @Transactional
    public UrlResponse shortenUrl(UrlRequest urlRequest) {
        log.info("URL Service :: Processing new short url generation");
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null) throw new UnauthorizedUrlAccessException();

        // check for custom alias
        String requestedAlias = urlRequest.customAlias();
        boolean hasAlias = requestedAlias != null && !requestedAlias.isBlank();
        if (hasAlias && urlRepository.existsByShortCode(requestedAlias)) {
            log.info("Custom Alias: {}, not available", requestedAlias);
            throw new AliasAlreadyTakenException("Custom alias not available.");
        }

        UrlMapping saveUrl = buildAndSaveUrl(urlRequest, userId);

        log.info("URL Service :: Short URL created — shortCode: {}, userId: {}", saveUrl.getShortCode(), userId);
        return urlMapper.urlResponseToUser(saveUrl);
    }

    @Override
    public void deleteUrl(Long urlId) {
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null) throw new UnauthorizedUrlAccessException();

        log.warn("URL Service :: Deleting url with id: {}", urlId);
        UrlMapping url = urlRepository.findById(urlId).orElseThrow(
                () -> new UrlNotFoundException("URL not found with id: " + urlId)
        );

        if (userId.equals(url.getUserId())) {
            urlCacheService.evict(url.getShortCode());
            urlRepository.delete(url);
        } else {
            throw new UnauthorizedUrlAccessException();
        }
        log.warn("URl Service :: Url deleted.");
    }

    @Override
    @Transactional
    public void deleteUserUrls(String userId) {
        evictUserUrlsFromCache(userId);
        urlRepository.deleteUserUrls(userId);
        log.info("URL Service :: Urls deleted for user: {}", userId);
    }

    @Override
    @Transactional
    public void changeUrlState(String userId, boolean state) {
        log.info("URL Service :: Processing Urls state");

        if (state) {
            log.info("URL Service :: Activating all urls for user: {}", userId);
            urlRepository.activateUserUrls(userId);
        } else {
            log.info("URL Service :: Deactivating all urls for user: {}", userId);
            urlRepository.deactivateUserUrls(userId);
        }

        // cached links still have the old active flag, so remove them from redis
        evictUserUrlsFromCache(userId);

    }

    @Override
    public PageResponse<UrlResponse> viewAllUrls(int page, int size) {
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null) throw new UnauthorizedUrlAccessException();

        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("page and size must be 1 or more");
        }
        int pageSize = Math.min(size, PageResponse.MAX_PAGE_SIZE);

        log.info("URL Service :: Fetching urls for userId: {}, page: {}, size: {}", userId, page, pageSize);

        // Spring Data counts pages from 0, our API counts from 1. Newest links first.
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt", "urlId");
        Pageable pageable = PageRequest.of(page - 1, pageSize, newestFirst);
        Page<UrlResponse> urls = urlRepository.findByUserId(userId, pageable)
                .map(urlMapper::urlResponseToUser);

        log.info("Url Service :: Urls data retrieval successful");
        return new PageResponse<>(urls.getContent(), page, pageSize, urls.getTotalElements(), urls.getTotalPages());
    }

    @Override
    public UrlResponse viewUrl(Long urlId) {
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null) throw new UnauthorizedUrlAccessException();

        UrlMapping url = urlRepository.findById(urlId).orElseThrow(
                () -> new UrlNotFoundException("URL not found with id: " + urlId)
        );
        if (!url.getUserId().equals(userId)) {
            throw new UnauthorizedUrlAccessException();
        }
        return urlMapper.urlResponseToUser(url);
    }

    @Override
    public void updateShortenedUrl(Long urlId, UpdateUrlRequest updateUrlRequest) {
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null) throw new UnauthorizedUrlAccessException();

        log.info("URL Service :: Updating url for user: {}", userId);

        UrlMapping urlMapping = urlRepository.findById(urlId).orElseThrow(
                () -> new UrlNotFoundException("URL not found with id: " + urlId)
        );

        if (!urlMapping.getUserId().equals(userId)) {
            throw new UnauthorizedUrlAccessException();
        }

        // the remove flags are optional, so they can be null
        boolean removeExpiry = Boolean.TRUE.equals(updateUrlRequest.removeExpiry());
        boolean removePassword = Boolean.TRUE.equals(updateUrlRequest.removePassword());
        boolean hasNewPassword = updateUrlRequest.password() != null && !updateUrlRequest.password().isBlank();
        if (removeExpiry && updateUrlRequest.expiresAt() != null) {
            throw new IllegalArgumentException("Send either expiresAt or removeExpiry, not both.");
        }
        if (removePassword && hasNewPassword) {
            throw new IllegalArgumentException("Send either password or removePassword, not both.");
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

        if (removeExpiry) {
            urlMapping.setExpiresAt(null);
        } else if (updateUrlRequest.expiresAt() != null) {
            urlMapping.setExpiresAt(updateUrlRequest.expiresAt());
        }

        if (removePassword) {
            urlMapping.setPasswordHash(null);
        } else if (hasNewPassword) {
            String hashed = encoder.encode(updateUrlRequest.password());
            urlMapping.setPasswordHash(hashed);
        }

        urlRepository.save(urlMapping);
        urlCacheService.evict(shortCodeToEvict);

        log.info("URL Service :: Updated URL with id: {}", urlId);
    }

    // helper methods
    private void evictUserUrlsFromCache(String userId) {
        urlRepository.findShortCodesByUserId(userId)
                .forEach(urlCacheService::evict);
    }

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

        // saveAndFlush: a clash with an existing short code fails here, inside this call
        if (urlRequest.customAlias() != null && !urlRequest.customAlias().isBlank()) {
            url.setShortCode(urlRequest.customAlias());
            return urlRepository.saveAndFlush(url);
        } else {
            UrlMapping savedUrl = urlRepository.save(url);
            savedUrl.setShortCode(generateFreeShortCode(savedUrl.getUrlId()));
            return urlRepository.saveAndFlush(savedUrl);
        }
    }

    /**
     * The code for an id is normally its Base62 form (6 characters). Aliases made before the
     * 7-character minimum can already hold that code; then use a random 7-character code instead.
     */
    private String generateFreeShortCode(Long urlId) {
        String shortCode = Base62Encoder.encode(urlId);
        while (urlRepository.existsByShortCode(shortCode)) {
            log.info("URL Service :: Short code {} is already taken, using a random one", shortCode);
            shortCode = Base62Encoder.random(RANDOM_CODE_LENGTH);
        }
        return shortCode;
    }
}
