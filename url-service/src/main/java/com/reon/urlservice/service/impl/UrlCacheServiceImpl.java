package com.reon.urlservice.service.impl;

import com.reon.exception.UrlNotFoundException;
import com.reon.urlservice.dto.CachedUrlDTO;
import com.reon.urlservice.model.UrlMapping;
import com.reon.urlservice.service.UrlCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class UrlCacheServiceImpl implements UrlCacheService {
    private static final String KEY_PREFIX = "url:short:";

    private final Logger log = LoggerFactory.getLogger(UrlCacheServiceImpl.class);
    private final RedisTemplate<String, CachedUrlDTO> redisTemplate;
    private final long ttlMinutes;

    public UrlCacheServiceImpl(RedisTemplate<String, CachedUrlDTO> redisTemplate,
                               @Value("${security.app.cache.url-ttl-minutes}") long ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.ttlMinutes = ttlMinutes;
    }


    @Override
    public CachedUrlDTO getOrLoad(String shortCode, Supplier<Optional<UrlMapping>> dbLoader) {
        String key = buildKey(shortCode);

        CachedUrlDTO cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.info("Cached hit :: ShortCode: {}", shortCode);
            return cached;
        }

        log.info("Cache miss :: ShortCode: {}, querying DB", shortCode);
        UrlMapping urlMapping = dbLoader.get()
                .orElseThrow(() -> new UrlNotFoundException("URL not found"));

        CachedUrlDTO cachedDTO = mapToUrlCacheResponse(urlMapping);
        redisTemplate.opsForValue().set(key, cachedDTO, ttlMinutes, TimeUnit.MINUTES);
        log.info("Cached shortCode: {} | TTL: {} min", shortCode, ttlMinutes);

        return cachedDTO;
    }

    @Override
    public void evict(String shortCode) {
        boolean deleted = redisTemplate.delete(buildKey(shortCode));
        log.info("Cache evicted :: shortCode: {} | deleted: {}", shortCode, deleted);
    }


    private CachedUrlDTO mapToUrlCacheResponse(UrlMapping urlMapping) {
        return CachedUrlDTO.builder()
                .shortCode(urlMapping.getShortCode())
                .longUrl(urlMapping.getLongUrl())
                .passwordHash(urlMapping.getPasswordHash())
                .active(urlMapping.isActive())
                .expiresAt(urlMapping.getExpiresAt())
                .build();
    }

    private String buildKey(String shortCode) {
        return KEY_PREFIX + shortCode;
    }
}
