package com.reon.userservice.service.impl;

import com.reon.userservice.model.User;
import com.reon.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        log.info("CustomUserDetailsService :: Loading user by username(email)");
        User user = userRepository.findByEmail(username)
                // UsernameNotFoundException is what Spring Security expects; it hides it as bad credentials
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        log.info("CustomUserDetailsService :: User loaded by username(email)");
        return user;
    }
}
