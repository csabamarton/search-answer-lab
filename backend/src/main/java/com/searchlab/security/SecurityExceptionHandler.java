package com.searchlab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Custom exception handlers for authentication and authorization errors.
 * Provides clear JSON error responses instead of default HTML error pages.
 */
@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(SecurityExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handles authentication failures (401 Unauthorized).
     * Called when a request requires authentication but no valid token is provided.
     */
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        logger.warn("Authentication failed: {} - {}", request.getRequestURI(), authException.getMessage());
        
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> error = new HashMap<>();
        error.put("error", "unauthorized");
        error.put("error_description", "Authentication required. Please provide a valid JWT token in the Authorization header.");
        error.put("path", request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    /**
     * Handles authorization failures (403 Forbidden).
     * Called when a request has a valid token but lacks required permissions/scopes.
     */
    @Override
    public void handle(HttpServletRequest request,
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException {
        
        logger.warn("Access denied: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());
        
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> error = new HashMap<>();
        error.put("error", "forbidden");
        error.put("error_description", "Insufficient permissions. Required scope: docs:search");
        error.put("path", request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
