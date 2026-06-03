package com.reon.urlservice.controller;

import com.reon.urlservice.dto.RedirectRequest;
import com.reon.urlservice.dto.response.UrlResponse;
import com.reon.urlservice.service.RedirectService;
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

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectUser(@PathVariable(name = "shortCode") String shortCode,
                                             @RequestParam(name = "password", required = false) String password,
                                             jakarta.servlet.http.HttpServletRequest request) {
        log.info("Redirect Controller :: Incoming request for redirecting shortUrl: {}", shortCode);

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        RedirectRequest redirectRequest = RedirectRequest.builder()
                .shortCode(shortCode)
                .password(password)
                .ipAddress(ipAddress)
                .userAgent(request.getHeader("User-Agent"))
                .referrer(request.getHeader("Referer"))
                .build();

        UrlResponse urlResponse = redirectService.redirectUserToOriginalUrl(redirectRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(urlResponse.longUrl()));
        log.info("Redirect Controller :: Outgoing request: Redirecting shortUrl: {}", shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .headers(headers)
                .build();
    }
}
