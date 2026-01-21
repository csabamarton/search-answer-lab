package com.searchlab.security;

import com.searchlab.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter to extract and validate JWT tokens from Authorization header.
 * Creates JwtAuthenticationToken and sets it in Spring Security context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // No token provided - clear any existing authentication and let Spring Security handle it
            logger.debug("No Authorization header found for request: {}", request.getRequestURI());
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            // Validate token and extract claims
            String userId = jwtService.extractUserId(token);
            List<String> scopes = jwtService.extractScopes(token);
            
            // Check if token is expired
            if (jwtService.isTokenExpired(token)) {
                logger.warn("Expired token provided: userId={}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            // Create authentication token
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(token, userId, scopes);
            
            // Set in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            logger.debug("JWT authentication successful: userId={}, scopes={}", userId, scopes);
            
        } catch (Exception e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            // Clear security context on validation failure
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
