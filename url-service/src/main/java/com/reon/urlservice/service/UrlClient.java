package com.reon.urlservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "user-service",
        url = "http://localhost:8100/api/v1/user"
)
public interface UrlClient {
    @PostMapping("/url/increase-count")
    void increaseUrlCount(@RequestParam("userId") String userId);
}
