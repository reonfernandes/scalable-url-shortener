package com.reon.userservice.controller;

import com.reon.exception.response.ApiResponse;
import com.reon.userservice.dto.response.UserProfile;
import com.reon.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@RequestParam(name = "userId") String userId){
        log.info("Deactivating account");
        userService.deactiveAccount(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Account deactivated successfully"
                ));
    }

    @PutMapping("/account/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@RequestParam(name = "userId") String userId){
        log.info("Activating account");
        userService.activateAccount(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(
                        HttpStatus.OK,
                        "Account activated successfully"
                ));
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserProfile>> getAccounts(@RequestParam(name = "page", defaultValue = "0") int pageNo,
                                                         @RequestParam(name = "size", defaultValue = "10") int pageSize)
    {
        log.info("Admin Controller :: Fetch all users → page = {}, size = {}", pageNo, pageSize);
        Page<UserProfile> userProfiles = userService.fetchAllUsers(pageNo, pageSize);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userProfiles);
    }
}