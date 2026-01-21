package com.searchlab.config;

import com.searchlab.security.JwtAuthenticationFilter;
import com.searchlab.security.SecurityExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration with JWT token validation.
 * 
 * Public endpoints:
 * - /api/health - Health checks
 * - /oauth/** - OAuth device flow endpoints
 * - /test/** - Test endpoints
 * 
 * Protected endpoints:
 * - /api/search - Requires JWT token with docs:search scope
 * - /api/admin/** - Requires JWT token (future)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize annotations
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityExceptionHandler securityExceptionHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         SecurityExceptionHandler securityExceptionHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityExceptionHandler = securityExceptionHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(securityExceptionHandler)  // 401 Unauthorized
                .accessDeniedHandler(securityExceptionHandler)        // 403 Forbidden
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/oauth/**").permitAll()
                .requestMatchers("/test/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll() // Allow error page access
                
                // Protected endpoints - require fully authenticated (non-anonymous) user
                .requestMatchers("/api/search").fullyAuthenticated()
                .requestMatchers("/api/admin/**").fullyAuthenticated()
                
                // All other requests require authentication
                .anyRequest().fullyAuthenticated()
            );

        return http.build();
    }
}
