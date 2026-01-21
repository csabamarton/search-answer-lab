package com.searchlab.controller;

import com.searchlab.model.entity.AuditEvent;
import com.searchlab.repository.AuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for querying audit events.
 * Allows viewing audit logs for security monitoring and debugging.
 */
@RestController
@RequestMapping("/api/admin/audit")
@CrossOrigin(origins = "http://localhost:3000")
public class AuditController {

    private final AuditRepository auditRepository;

    public AuditController(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * GET /api/admin/audit/events
     * Get paginated audit events (most recent first)
     * 
     * Query parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20, max: 100)
     * - userId: Filter by user ID (optional)
     * - eventType: Filter by event type (optional)
     * - status: Filter by status - SUCCESS or FAILURE (optional)
     */
    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> getAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status) {
        
        // Limit page size
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditEvent> events;
        
        // Apply filters
        if (userId != null && !userId.isEmpty()) {
            events = auditRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        } else if (eventType != null && !eventType.isEmpty()) {
            events = auditRepository.findByEventTypeOrderByTimestampDesc(eventType, pageable);
        } else if (status != null && !status.isEmpty()) {
            events = auditRepository.findByStatusOrderByTimestampDesc(status, pageable);
        } else {
            // Get all events
            events = auditRepository.findAll(pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("events", events.getContent());
        response.put("totalElements", events.getTotalElements());
        response.put("totalPages", events.getTotalPages());
        response.put("currentPage", page);
        response.put("pageSize", size);
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/audit/stats
     * Get audit statistics (event counts by type, status, etc.)
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAuditStats() {
        // Get recent events for stats (last 24 hours)
        Instant yesterday = Instant.now().minusSeconds(24 * 60 * 60);
        Instant now = Instant.now();
        
        Pageable pageable = PageRequest.of(0, 1000);
        Page<AuditEvent> recentEvents = auditRepository.findByTimestampBetween(yesterday, now, pageable);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", recentEvents.getTotalElements());
        stats.put("period", "last 24 hours");
        
        // Count by event type
        Map<String, Long> eventTypeCounts = new HashMap<>();
        Map<String, Long> statusCounts = new HashMap<>();
        
        for (AuditEvent event : recentEvents.getContent()) {
            eventTypeCounts.merge(event.getEventType(), 1L, Long::sum);
            statusCounts.merge(event.getStatus(), 1L, Long::sum);
        }
        
        stats.put("byEventType", eventTypeCounts);
        stats.put("byStatus", statusCounts);
        
        return ResponseEntity.ok(stats);
    }
}
