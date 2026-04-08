package com.reon.urlservice.service;

import com.reon.urlservice.dto.UpdateUrlRequest;
import com.reon.urlservice.dto.UrlRequest;
import com.reon.urlservice.dto.response.UrlListResponse;
import com.reon.urlservice.dto.response.UrlResponse;
import org.springframework.data.domain.Page;

public interface UrlService {
    UrlResponse shortenUrl(UrlRequest urlRequest);
    Page<UrlListResponse> viewAllUrls(int page, int size);
    void updateShortenedUrl(Long urlId, UpdateUrlRequest updateUrlRequest);
    void deleteUrl(Long urlId);

    void deleteUserUrls(String userId);
}
