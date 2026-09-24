package com.reon.urlservice.controller;

import com.reon.exception.response.ApiResponse;
import com.reon.urlservice.dto.RedirectRequest;
import com.reon.urlservice.dto.UnlockUrlRequest;
import com.reon.urlservice.dto.response.UnlockUrlResponse;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.urlservice.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/redirect")
public class RedirectController {
    private final Logger log = LoggerFactory.getLogger(RedirectController.class);
    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    // normal links: the browser follows the 302 straight to the long URL
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectUser(@PathVariable(name = "shortCode") String shortCode,
                                             HttpServletRequest request) {
        log.info("Redirect Controller :: Incoming request for redirecting shortUrl: {}", shortCode);

        UrlResponse urlResponse = redirectService.redirectUserToOriginalUrl(buildRedirectRequest(shortCode, null, request));

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(urlResponse.longUrl()));
        log.info("Redirect Controller :: Outgoing request: Redirecting shortUrl: {}", shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .headers(headers)
                .build();
    }

    // password-protected links: the password is sent in the body (not the URL) and the long URL is returned
    @PostMapping("/{shortCode}")
    public ResponseEntity<ApiResponse<UnlockUrlResponse>> unlockProtectedUrl(@PathVariable(name = "shortCode") String shortCode,
                                                                             @Valid @RequestBody UnlockUrlRequest unlockRequest,
                                                                             HttpServletRequest request) {
        log.info("Redirect Controller :: Incoming request for unlocking shortUrl: {}", shortCode);

        UrlResponse urlResponse = redirectService.redirectUserToOriginalUrl(
                buildRedirectRequest(shortCode, unlockRequest.password(), request));
        log.info("Redirect Controller :: Outgoing request: Unlocked shortUrl: {}", shortCode);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Success",
                        new UnlockUrlResponse(urlResponse.longUrl())
                ));
    }

    private RedirectRequest buildRedirectRequest(String shortCode, String password, HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        return RedirectRequest.builder()
                .shortCode(shortCode)
                .password(password)
                .ipAddress(ipAddress)
                .userAgent(request.getHeader("User-Agent"))
                .referrer(request.getHeader("Referer"))
                .build();
    }
}
