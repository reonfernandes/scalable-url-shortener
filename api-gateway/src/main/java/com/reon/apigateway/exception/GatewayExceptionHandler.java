package com.reon.apigateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@Order(-1)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);
    
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An unexpected error occurred";

        // errors like "no route found" (404) or "service not available" (503) carry their own status
        if (ex instanceof ResponseStatusException statusException) {
            HttpStatus resolved = HttpStatus.resolve(statusException.getStatusCode().value());
            if (resolved != null) {
                status = resolved;
                message = status.getReasonPhrase();
            }
        }

        if (status.is5xxServerError()) {
            log.error("Gateway :: Unhandled exception: {}", ex.getMessage(), ex);
        } else {
            log.warn("Gateway :: Request failed with {}: {}", status.value(), ex.getMessage());
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "timestamp": "%s",
                  "status": %d,
                  "error": "%s",
                  "message": "%s",
                  "path": "%s"
                }
                """.formatted(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value()
        );

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
