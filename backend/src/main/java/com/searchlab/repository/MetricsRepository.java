package com.searchlab.repository;

import com.searchlab.model.entity.SearchMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for SearchMetric entity.
 * Stores and retrieves search performance and analytics data.
 */
@Repository
public interface MetricsRepository extends JpaRepository<SearchMetric, Long> {

    // TODO: Add custom query methods
    // - Find metrics by query hash
    // - Aggregate statistics
    // - Filter by date range and search mode
}
