package com.reon.urlservice.controller;

import com.reon.exception.response.ApiResponse;
import com.reon.urlservice.dto.UpdateUrlRequest;
import com.reon.urlservice.dto.UrlRequest;
import com.reon.urlservice.dto.response.UrlListResponse;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.urlservice.service.UrlService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
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

    @GetMapping("/my-urls")
    public ResponseEntity<ApiResponse<Page<UrlListResponse>>> fetchAllUrls(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Page<UrlListResponse> urlListResponses = urlService.viewAllUrls(page, size);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Success",
                        urlListResponses
                ));
    }

    @PatchMapping("/update-url")
    public ResponseEntity<ApiResponse<UrlResponse>> updateUrl(@RequestParam(name = "urlId") Long urlId,
                                                              @Valid @RequestBody UpdateUrlRequest updateUrlRequest) {
        log.info("Update url request: {}", urlId);
        urlService.updateShortenedUrl(urlId, updateUrlRequest);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "URL Updated successfully."
                ));
    }


    @DeleteMapping("/delete-url")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(@RequestParam("urlId") Long urlId) {
        log.info("Request for deleting url: {}", urlId);
        urlService.deleteUrl(urlId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.of(
                        HttpStatus.NO_CONTENT,
                        "URL deleted successfully"
                ));
    }

}
