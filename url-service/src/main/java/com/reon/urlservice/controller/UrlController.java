package com.reon.urlservice.controller;

import com.reon.exception.response.ApiResponse;
import com.reon.urlservice.dto.UrlRequest;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.urlservice.service.UrlService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/url")
public class UrlController {
    private final Logger log = LoggerFactory.getLogger(UrlController.class);
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/new")
    public ResponseEntity<ApiResponse<UrlResponse>> generateNewShortUrl(
            @Valid @RequestBody UrlRequest urlRequest){
        log.info("Short url creation request: {}", urlRequest.longUrl());
        UrlResponse shortUrl = urlService.shortenUrl(urlRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "Short URL created",
                        shortUrl
                ));
    }
}
