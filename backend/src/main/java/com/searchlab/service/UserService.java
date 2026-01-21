package com.searchlab.service;

import com.searchlab.model.entity.User;
import com.searchlab.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for user authentication and management.
 * Used in Step 2 (Device Code Flow) for user login.
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Authenticate a user by username and password
     * @param username Username
     * @param password Plain text password
     * @return User if authentication succeeds, empty otherwise
     */
    public Optional<User> authenticate(String username, String password) {
        logger.debug("Authenticating user: username={}", username);
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("User not found: username={}", username);
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!user.getActive()) {
            logger.warn("User account is inactive: username={}, user_id={}", username, user.getId());
            return Optional.empty();
        }

        boolean passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
        if (passwordMatches) {
            logger.info("User authenticated successfully: username={}, user_id={}", username, user.getId());
            return Optional.of(user);
        } else {
            logger.warn("Password mismatch for user: username={}", username);
        }

        return Optional.empty();
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get user ID as string (for JWT token subject)
     */
    public String getUserIdAsString(User user) {
        return String.valueOf(user.getId());
    }
}
