package com.searchlab.repository;

import com.searchlab.model.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for audit events.
 * Provides query methods for audit log analysis.
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditEvent, Long> {

    /**
     * Find all audit events for a specific user
     */
    Page<AuditEvent> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    /**
     * Find all audit events by event type
     */
    Page<AuditEvent> findByEventTypeOrderByTimestampDesc(String eventType, Pageable pageable);

    /**
     * Find all audit events within a time range
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.timestamp >= :start AND a.timestamp <= :end ORDER BY a.timestamp DESC")
    Page<AuditEvent> findByTimestampBetween(
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );

    /**
     * Find all failed events (for security monitoring)
     */
    Page<AuditEvent> findByStatusOrderByTimestampDesc(String status, Pageable pageable);

    /**
     * Count events by user ID within time range
     */
    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.userId = :userId AND a.timestamp >= :start AND a.timestamp <= :end")
    long countByUserIdAndTimestampBetween(
            @Param("userId") String userId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
