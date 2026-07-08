package com.apigateway.tenant.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Step 1: Get Authorization header
            String authHeader = request.getHeader("Authorization");

            log.debug("Authorization header: {}",
                    authHeader != null ?
                            authHeader.substring(0, Math.min(20, authHeader.length()))
                            : "NULL");

            // Step 2: Check header exists and starts with Bearer
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("No Bearer token found, skipping JWT filter");
                filterChain.doFilter(request, response);
                return;
            }

            // Step 3: Extract token (remove "Bearer " prefix - 7 characters)
            String token = authHeader.substring(7);
            log.debug("Token extracted, length: {}", token.length());

            // Step 4: Validate token
            if (!jwtTokenProvider.validateToken(token)) {
                log.debug("Token validation failed");
                filterChain.doFilter(request, response);
                return;
            }

            // Step 5: Extract email from token
            String email = jwtTokenProvider.extractEmail(token);
            log.debug("Email extracted from token: {}", email);

            // Step 6: Only authenticate if not already authenticated
            if (email != null && SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                // Step 7: Load user from database
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                // Step 8: Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request));

                // Step 9: Set authentication in SecurityContext
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

                log.debug("Successfully authenticated user: {}", email);
            }

        } catch (Exception e) {
            log.error("JWT authentication error: {}", e.getMessage());
        }

        // Step 10: Always continue filter chain
        filterChain.doFilter(request, response);
    }
}