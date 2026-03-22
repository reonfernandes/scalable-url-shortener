package com.reon.urlservice.service.impl;

import com.reon.exception.AliasAlreadyTakenException;
import com.reon.exception.UrlNotFoundException;
import com.reon.exception.UrlQuotaExceededException;
import com.reon.urlservice.common.Base62Encoder;
import com.reon.urlservice.dto.UrlRequest;
import com.reon.urlservice.dto.response.UrlListResponse;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.urlservice.jwt.JwtService;
import com.reon.urlservice.mapper.UrlMapper;
import com.reon.urlservice.model.UrlMapping;
import com.reon.urlservice.respository.UrlRepository;
import com.reon.urlservice.service.UrlClient;
import com.reon.urlservice.service.UrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UrlServiceImpl implements UrlService {

    private final int freeTierLimit;
    private final int premiumTierLimit;


    private final Logger log = LoggerFactory.getLogger(UrlServiceImpl.class);
    private final UrlRepository urlRepository;
    private final UrlMapper urlMapper;
    private final JwtService jwtService;
    private final PasswordEncoder encoder;
    private final UrlClient urlClient;

    public UrlServiceImpl(
            @Value("${security.app.quota.free-tier-limit}") int freeTierLimit,
            @Value("${security.app.quota.premium-tier-limit}") int premiumTierLimit,
            UrlRepository urlRepository, UrlMapper urlMapper, JwtService jwtService, PasswordEncoder encoder, UrlClient urlClient) {
        this.freeTierLimit = freeTierLimit;
        this.premiumTierLimit = premiumTierLimit;
        this.urlRepository = urlRepository;
        this.urlMapper = urlMapper;
        this.jwtService = jwtService;
        this.encoder = encoder;
        this.urlClient = urlClient;
    }

    @Override
    public UrlResponse shortenUrl(UrlRequest urlRequest) {
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
        log.info("URL Service :: Deleting url with id: {}", urlId);
        UrlMapping url = urlRepository.findById(String.valueOf(urlId)).orElseThrow(
                () -> new UrlNotFoundException("URL not found with id: " + urlId)
        );

        if (url != null) {
            urlRepository.delete(url);
            urlClient.decreaseUrlCount(url.getUserId());
        }
    }

    @Override
    public Page<UrlListResponse> viewAllUrls(int page, int size) {
        // got the userId from security context: to display urls only for currently loggedIn user.
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

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

        return new PageImpl<>(List.of(urlListResponse), pageable, mappings.getTotalElements());
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
        String userId;
        String tier = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            userId = (String) authentication.getPrincipal();
            String token = (String) authentication.getCredentials();

            if (token != null) {
                tier = jwtService.getTier(token);
            }
            return Map.of(
                    "userId", userId,
                    "tier", tier
            );
        }
        return Map.of();
    }
}
