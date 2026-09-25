package com.reon.urlservice.controller;

import com.reon.exception.response.ApiResponse;
import com.reon.exception.response.PageResponse;
import com.reon.urlservice.dto.UpdateUrlRequest;
import com.reon.urlservice.dto.UrlRequest;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.urlservice.service.UrlService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/url")
public class UrlController {
    private final Logger log = LoggerFactory.getLogger(UrlController.class);
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/new")
    public ResponseEntity<ApiResponse<UrlResponse>> generateNewShortUrl(@Valid @RequestBody UrlRequest urlRequest){
        log.info("Url Controller :: Incoming request for generating short url: {}", urlRequest.longUrl());
        UrlResponse shortUrl = urlService.shortenUrl(urlRequest);
        log.info("Url Controller :: Outgoing request: Short url created");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "Short URL created",
                        shortUrl
                ));
    }

    @GetMapping("/my-urls")
    public ResponseEntity<ApiResponse<PageResponse<UrlResponse>>> fetchAllUrls(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        log.info("Url Controller :: Incoming request for fetching urls from page: {} of size: {}", page, size);
        PageResponse<UrlResponse> urlListResponses = urlService.viewAllUrls(page, size);
        log.info("Url Controller :: Outgoing request: Urls fetched");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Success",
                        urlListResponses
                ));
    }

    @GetMapping("/{urlId}")
    public ResponseEntity<ApiResponse<UrlResponse>> fetchUrl(@PathVariable("urlId") Long urlId) {
        log.info("Url Controller :: Incoming request for fetching url: {}", urlId);
        UrlResponse url = urlService.viewUrl(urlId);
        log.info("Url Controller :: Outgoing request: Url fetched");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Success",
                        url
                ));
    }

    @PatchMapping("/update-url")
    public ResponseEntity<ApiResponse<UrlResponse>> updateUrl(@RequestParam(name = "urlId") Long urlId,
                                                              @Valid @RequestBody UpdateUrlRequest updateUrlRequest) {
        log.info("Url Controller :: Incoming request to update url request: {}", urlId);
        urlService.updateShortenedUrl(urlId, updateUrlRequest);
        log.info("Url Controller :: Outgoing request: Url updated");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "URL Updated successfully."
                ));
    }


    @DeleteMapping("/delete-url")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(@RequestParam("urlId") Long urlId) {
        log.info("Url Controller :: Incoming request for deleting url: {}", urlId);
        urlService.deleteUrl(urlId);
        log.info("Url Controller :: Outgoing request: Url deleted");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "URL deleted successfully"
                ));
    }
}
