package com.searchlab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Database configuration for JPA repositories.
 * 
 * Note: The @EnableJpaRepositories annotation is optional in Spring Boot,
 * as auto-configuration will discover repositories in the base package.
 * However, it's included here for explicit configuration clarity.
 * 
 * Connection pooling, transaction management, and JPA settings are
 * configured in application.yml (HikariCP, Hibernate dialect, etc.).
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.searchlab.repository")
public class DatabaseConfig {
}
