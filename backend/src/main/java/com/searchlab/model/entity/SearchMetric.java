package com.searchlab.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity for tracking search metrics and performance.
 * Records query details, execution time, and results for analytics.
 */
@Entity
@Table(name = "search_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "trace_id")
    private String traceId;

    @Column(nullable = false)
    private String query;

    @Column(name = "query_hash", nullable = false)
    private String queryHash;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "results_count", nullable = false)
    private Integer resultsCount;

    @Column(name = "search_mode", nullable = false)
    private String searchMode;

    @Column(name = "fallback_used", nullable = false)
    private Boolean fallbackUsed;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
