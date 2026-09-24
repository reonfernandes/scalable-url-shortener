package com.reon.apigateway.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayExceptionHandlerTest {
    private final GatewayExceptionHandler handler = new GatewayExceptionHandler();

    @Test
    void keepsStatusOfResponseStatusException() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/unknown/path"));

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND)).block();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    }

    @Test
    void returns500ForUnexpectedErrors() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/url/new"));

        handler.handle(exchange, new IllegalStateException("boom")).block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
    }
}
