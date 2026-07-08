package com.apigateway.tenant.util;

import com.apigateway.tenant.entity.User;
import com.apigateway.tenant.exception.ResourceNotFoundException;
import com.apigateway.tenant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    // Returns the currently logged-in user's email
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        return authentication.getName();
    }

    // Returns the full User entity of the currently logged-in user
    public User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmailWithTenant(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Current user not found: " + email));
    }
}
