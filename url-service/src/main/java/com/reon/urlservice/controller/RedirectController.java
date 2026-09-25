package com.reon.urlservice.controller;

import com.reon.exception.PasswordRequiredException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/redirect")
public class RedirectController {
    private final Logger log = LoggerFactory.getLogger(RedirectController.class);
    private final RedirectService redirectService;
    private final String uiBaseUrl;

    public RedirectController(RedirectService redirectService,
                              @Value("${security.app.ui.base-url}") String uiBaseUrl) {
        this.redirectService = redirectService;
        this.uiBaseUrl = uiBaseUrl;
    }

    // normal links: the browser follows the 302 straight to the long URL
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectUser(@PathVariable(name = "shortCode") String shortCode,
                                             HttpServletRequest request) {
        log.info("Redirect Controller :: Incoming request for redirecting shortUrl: {}", shortCode);

        URI destination;
        try {
            UrlResponse urlResponse = redirectService.redirectUserToOriginalUrl(buildRedirectRequest(shortCode, null, request));
            destination = URI.create(urlResponse.longUrl());
            log.info("Redirect Controller :: Outgoing request: Redirecting shortUrl: {}", shortCode);
        } catch (PasswordRequiredException exception) {
            // A browser can't show a password prompt for a JSON error, so send the visitor
            // to the UI's password page, which unlocks the link with POST below.
            destination = URI.create(uiBaseUrl + "/unlock/" + URLEncoder.encode(shortCode, StandardCharsets.UTF_8));
            log.info("Redirect Controller :: shortUrl: {} is password protected, sending visitor to the unlock page", shortCode);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(destination);

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
