package com.reon.userservice.controller;

import com.reon.exception.response.ApiResponse;
import com.reon.exception.response.PageResponse;
import com.reon.userservice.dto.response.UserProfile;
import com.reon.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UserService userService;
    private final Logger log = LoggerFactory.getLogger(AdminController.class);

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/account/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@RequestParam(name = "userId") String userId){
        log.warn("Admin Controller :: Incoming request for deactivating account: {}", userId);
        userService.deactivateAccount(userId);
        log.warn("Admin Controller :: Outgoing request: Account deactivated");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Account deactivated successfully"
                ));
    }

    @PutMapping("/account/activate")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@RequestParam(name = "userId") String userId){
        log.warn("Admin Controller :: Incoming request for Activating account: {}", userId);
        userService.activateAccount(userId);
        log.warn("Admin Controller :: Outgoing request: Account Activated");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Account activated successfully"
                ));
    }

    @GetMapping(value = "/accounts")
    public ResponseEntity<ApiResponse<PageResponse<UserProfile>>> getAccounts(@RequestParam(name = "page", defaultValue = "1") int pageNo,
                                                         @RequestParam(name = "size", defaultValue = "10") int pageSize,
                                                         // optional: part of a name or email
                                                         @RequestParam(name = "search", required = false) String search,
                                                         // optional: all (default), active or deactivated
                                                         @RequestParam(name = "status", defaultValue = "all") String status) {
        log.info("Admin Controller :: Incoming request for fetching users → page = {}, size = {}, status = {}", pageNo, pageSize, status);
        PageResponse<UserProfile> userProfiles = userService.viewAllUsers(pageNo, pageSize, search, status);
        log.info("Admin Controller :: Outgoing request: Users info fetched");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Success",
                        userProfiles
                ));
    }
}