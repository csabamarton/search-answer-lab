package com.searchlab.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom Authentication token for JWT-based authentication.
 * Contains the JWT token, user ID, and scopes (as authorities).
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;
    private final String userId;
    private final List<String> scopes;

    public JwtAuthenticationToken(String token, String userId, List<String> scopes) {
        super(convertScopesToAuthorities(scopes));
        this.token = token;
        this.userId = userId;
        this.scopes = scopes;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public List<String> getScopes() {
        return scopes;
    }

    /**
     * Convert scopes to Spring Security authorities.
     * Each scope becomes a GrantedAuthority (e.g., "docs:search" -> authority "docs:search")
     */
    private static Collection<? extends GrantedAuthority> convertScopesToAuthorities(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return List.of();
        }
        return scopes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
