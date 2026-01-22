package com.searchlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Search Answer Lab.
 * Provides REST API for traditional and AI-powered search capabilities.
 */
@SpringBootApplication
@EnableScheduling // Enable scheduled tasks (e.g., refresh token cleanup)
public class SearchAnswerLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchAnswerLabApplication.class, args);
    }
}
