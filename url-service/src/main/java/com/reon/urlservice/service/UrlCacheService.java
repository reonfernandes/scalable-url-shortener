package com.reon.urlservice.service;

import com.reon.urlservice.dto.CachedUrlDTO;
import com.reon.urlservice.model.UrlMapping;

import java.util.Optional;
import java.util.function.Supplier;

public interface UrlCacheService {
    CachedUrlDTO getOrLoad(String shortCode, Supplier<Optional<UrlMapping>> dbLoader);
    void evict(String shortCode);
}