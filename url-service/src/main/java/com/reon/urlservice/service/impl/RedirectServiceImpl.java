package com.reon.urlservice.service.impl;

import com.reon.events.UrlClickEvent;
import com.reon.exception.InvalidUrlPasswordException;
import com.reon.exception.PasswordRequiredException;
import com.reon.exception.UrlExpiredException;
import com.reon.exception.UrlNotActiveException;
import com.reon.urlservice.dto.CachedUrlDTO;
import com.reon.urlservice.dto.RedirectRequest;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.urlservice.mapper.UrlMapper;
import com.reon.urlservice.respository.UrlRepository;
import com.reon.urlservice.service.RedirectService;
import com.reon.urlservice.service.UrlCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RedirectServiceImpl implements RedirectService {

    private final Logger log = LoggerFactory.getLogger(RedirectServiceImpl.class);
    private final UrlRepository urlRepository;
    private final UrlMapper urlMapper;
    private final PasswordEncoder encoder;
    private final UrlCacheService urlCacheService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RedirectServiceImpl(UrlRepository urlRepository, UrlMapper urlMapper, PasswordEncoder encoder,
                               UrlCacheService urlCacheService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.urlRepository = urlRepository;
        this.urlMapper = urlMapper;
        this.encoder = encoder;
        this.urlCacheService = urlCacheService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional
    public UrlResponse redirectUserToOriginalUrl(RedirectRequest redirectRequest) {
        log.info("Redirect Service :: Redirecting user to original url: {}", redirectRequest.shortCode());
        CachedUrlDTO url = urlCacheService.getOrLoad(
                redirectRequest.shortCode(),
                () -> urlRepository.findByShortCode(redirectRequest.shortCode())
        );

        if (!url.active()) {
            throw new UrlNotActiveException();
        }

        if (url.expiresAt() != null && url.expiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException();
        }

        boolean isUrlPasswordProtected = url.passwordHash() != null;
        if (isUrlPasswordProtected) {
            if (redirectRequest.password() == null || redirectRequest.password().isBlank()) {
                throw new PasswordRequiredException();
            }

            if (!encoder.matches(redirectRequest.password(), url.passwordHash())) {
                throw new InvalidUrlPasswordException();
            }
        }

        urlRepository.incrementClickCount(redirectRequest.shortCode());
        log.info("Redirect Service :: Redirected to original url: shortCode: {}", redirectRequest.shortCode());

        publishClickEvent(redirectRequest, url);

        return urlMapper.urlResponseToUser(url);
    }

    private void publishClickEvent(RedirectRequest request, CachedUrlDTO url) {
        UrlClickEvent event = UrlClickEvent.builder()
                .shortCode(url.shortCode())
                .urlId(String.valueOf(url.urlId()))
                .userId(url.userId())
                .ipAddress(request.ipAddress())
                .userAgent(request.userAgent())
                .referrer(request.referrer())
                .clickedAt(LocalDateTime.now())
                .build();

        try {
            kafkaTemplate.send("url-clicked", event);
            log.info("Redirect Service :: Published UrlClickEvent for shortCode: {}", url.shortCode());
        } catch (Exception e) {
            log.error("Redirect Service :: Failed to publish UrlClickEvent for shortCode: {}", url.shortCode(), e);
        }
    }
}
