package com.reon.urlservice.service;

import com.reon.urlservice.dto.UrlRequest;
import com.reon.urlservice.dto.response.UrlListResponse;
import com.reon.urlservice.dto.response.UrlResponse;
import org.springframework.data.domain.Page;

public interface UrlService {
    UrlResponse shortenUrl(UrlRequest urlRequest);
    void deleteUrl(Long urlId);
    Page<UrlListResponse> viewAllUrls(int page, int size);
}
