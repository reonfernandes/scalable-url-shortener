package com.reon.urlservice.service;

import com.reon.urlservice.dto.UrlRequest;
import com.reon.urlservice.dto.response.UrlResponse;

public interface UrlService {
    UrlResponse shortenUrl(UrlRequest urlRequest);
}
