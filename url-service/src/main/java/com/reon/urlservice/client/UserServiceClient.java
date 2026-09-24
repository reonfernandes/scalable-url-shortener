package com.reon.urlservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// calls user-service; the address is looked up in Eureka by the name "user-service"
@FeignClient(
        name = "user-service",
        path = "/api/v1/user"
)
public interface UserServiceClient {
    @PostMapping("/url/increase-count")
    void increaseUrlCount(@RequestParam("userId") String userId);

    @PostMapping("/url/decrease-count")
    void decreaseUrlCount(@RequestParam("userId") String userId);
}
