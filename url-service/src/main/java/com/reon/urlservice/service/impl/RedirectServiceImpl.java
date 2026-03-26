package com.reon.urlservice.service.impl;

import com.reon.exception.*;
import com.reon.urlservice.dto.CachedUrlDTO;
import com.reon.urlservice.dto.RedirectRequest;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.urlservice.mapper.UrlMapper;
import com.reon.urlservice.model.UrlMapping;
import com.reon.urlservice.respository.UrlRepository;
import com.reon.urlservice.service.RedirectService;
import com.reon.urlservice.service.UrlCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public RedirectServiceImpl(UrlRepository urlRepository, UrlMapper urlMapper, PasswordEncoder encoder, UrlCacheService urlCacheService) {
        this.urlRepository = urlRepository;
        this.urlMapper = urlMapper;
        this.encoder = encoder;
        this.urlCacheService = urlCacheService;
    }

    @Override
    @Transactional
    public UrlResponse redirectUserToOriginalUrl(RedirectRequest redirectRequest) {
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
        log.info("Redirect Service :: Redirecting shortCode: {}", redirectRequest.shortCode());

        // todo:: publish event - link.clicked

        return urlMapper.urlResponseToUser(url);
    }
}
