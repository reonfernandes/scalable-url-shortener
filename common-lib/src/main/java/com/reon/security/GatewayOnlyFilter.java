package com.reon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * The services trust X-User-Id to know who is calling, so they must only accept requests
 * that came through the API gateway. The gateway adds a shared secret (X-Gateway-Secret);
 * anything without it is rejected with 403, even if a service port is reachable directly.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayOnlyFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Gateway-Secret";

    private final Logger log = LoggerFactory.getLogger(GatewayOnlyFilter.class);
    private final byte[] expectedSecret;

    public GatewayOnlyFilter(@Value("${security.gateway.secret}") String gatewaySecret) {
        this.expectedSecret = gatewaySecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String secret = request.getHeader(HEADER);
        // MessageDigest.isEqual takes the same time for any input, so the secret can't be guessed byte by byte.
        if (secret == null || !MessageDigest.isEqual(expectedSecret, secret.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Rejected request that did not come through the API gateway: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":403,\"error\":\"FORBIDDEN\",\"message\":\"Requests must go through the API gateway.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
