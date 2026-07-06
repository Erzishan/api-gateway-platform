package com.apigateway.tenant.security;

import com.apigateway.tenant.constants.AppConstants;
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
import org.springframework.util.StringUtils;
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

        // Step 1: Extract the JWT from the Authorization header
        String token = extractTokenFromRequest(request);

        // Step 2: If token exists and is valid, authenticate the user
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

            // Step 3: Get the email from the token
            String email = jwtTokenProvider.extractEmail(token);

            // Step 4: Load the full user from database
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            // Step 5: Create an authentication object
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // Step 6: Add request details to authentication
            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request));

            // Step 7: Store authentication in SecurityContext
            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            log.debug("Authenticated user: {}", email);
        }

        // Step 8: Always continue to the next filter
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(
                AppConstants.JWT_HEADER_NAME);

        if (StringUtils.hasText(bearerToken)
                && bearerToken.startsWith(
                AppConstants.JWT_TOKEN_PREFIX)) {
            // Remove "Bearer " prefix, return only the token
            return bearerToken.substring(7);
        }

        return null;
    }
}
