package com.reon.apigateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RemoveUserHeadersFilterTest {

    @Test
    void removesUserHeadersSentByTheClient() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/user/login")
                        .header("X-User-Id", "someone-else")
                        .header("x-user-roles", "ROLE_ADMIN")
                        .header("Accept", "application/json"));

        AtomicReference<HttpHeaders> forwardedHeaders = new AtomicReference<>();
        new RemoveUserHeadersFilter()
                .filter(exchange, forwarded -> {
                    forwardedHeaders.set(forwarded.getRequest().getHeaders());
                    return Mono.empty();
                })
                .block();

        HttpHeaders headers = forwardedHeaders.get();
        assertNull(headers.getFirst("X-User-Id"));
        assertNull(headers.getFirst("X-User-Roles"));
        assertEquals("application/json", headers.getFirst("Accept"));
    }
}
