package com.reon.urlservice.service;

import com.reon.urlservice.dto.UpdateUrlRequest;
import com.reon.urlservice.dto.UrlRequest;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.exception.response.PageResponse;

public interface UrlService {
    // basic ops
    UrlResponse shortenUrl(UrlRequest urlRequest);
    PageResponse<UrlResponse> viewAllUrls(int page, int size);
    UrlResponse viewUrl(Long urlId);
    void updateShortenedUrl(Long urlId, UpdateUrlRequest updateUrlRequest);
    void deleteUrl(Long urlId);

    // url specific



    // kafka events
    void deleteUserUrls(String userId);
    void changeUrlState(String userId, boolean state);
}
