package com.searchlab.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchlab.model.entity.AuditEvent;
import com.searchlab.model.dto.SearchRequest;
import com.searchlab.model.dto.SearchResponse;
import com.searchlab.repository.AuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP Aspect for automatic audit logging.
 * Logs all operations on methods annotated with @PreAuthorize.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);
    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Audit all methods annotated with @PreAuthorize (protected endpoints)
     */
    @Around("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public Object auditSecuredMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
        
        // Get request info
        String requestId = getRequestId();
        String ipAddress = getIpAddress();
        
        // Extract method arguments for audit data
        Object[] args = joinPoint.getArgs();
        Map<String, Object> eventData = extractEventData(methodName, args);
        
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setUserId(userId);
        auditEvent.setEventType("api_request");
        auditEvent.setToolName(methodName);
        auditEvent.setAction("EXECUTE");
        auditEvent.setRequestId(requestId);
        auditEvent.setIpAddress(ipAddress);
        
        try {
            // Execute the method
            Object result = joinPoint.proceed();
            
            // Calculate duration
            long duration = Duration.between(start, Instant.now()).toMillis();
            
            // Extract result metadata
            if (result instanceof SearchResponse) {
                SearchResponse searchResponse = (SearchResponse) result;
                eventData.put("resultCount", searchResponse.getMetadata().getTotalResults());
                eventData.put("durationMs", searchResponse.getMetadata().getDurationMs());
                eventData.put("searchMode", searchResponse.getMetadata().getSearchMode());
            }
            
            // Log success
            auditEvent.setStatus("SUCCESS");
            auditEvent.setDurationMs(duration);
            auditEvent.setEventData(toJson(eventData));
            
            auditRepository.save(auditEvent);
            logger.debug("Audit logged: {} by user {} in {}ms", methodName, userId, duration);
            
            return result;
            
        } catch (Throwable e) {
            // Log failure
            long duration = Duration.between(start, Instant.now()).toMillis();
            
            auditEvent.setStatus("FAILURE");
            auditEvent.setDurationMs(duration);
            auditEvent.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
            eventData.put("error", e.getClass().getSimpleName());
            auditEvent.setEventData(toJson(eventData));
            
            auditRepository.save(auditEvent);
            logger.debug("Audit logged failure: {} by user {} - {}", methodName, userId, e.getMessage());
            
            throw e;
        }
    }

    /**
     * Extract event data from method arguments
     */
    private Map<String, Object> extractEventData(String methodName, Object[] args) {
        Map<String, Object> eventData = new HashMap<>();
        
        if ("search".equals(methodName)) {
            // SearchController.search(SearchRequest)
            for (Object arg : args) {
                if (arg instanceof SearchRequest) {
                    SearchRequest request = (SearchRequest) arg;
                    eventData.put("query", request.getQuery());
                    eventData.put("mode", request.getMode() != null ? request.getMode() : "traditional");
                    eventData.put("page", request.getPage() != null ? request.getPage() : 0);
                    eventData.put("pageSize", request.getPageSize() != null ? request.getPageSize() : 10);
                    if (request.getTraceId() != null) {
                        eventData.put("traceId", request.getTraceId());
                    }
                    break;
                }
            }
        }
        
        return eventData;
    }

    /**
     * Get request ID from header or generate one
     */
    private String getRequestId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String requestId = request.getHeader("X-Request-Id");
                if (requestId != null && !requestId.isEmpty()) {
                    return requestId;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get request ID from header", e);
        }
        return null;
    }

    /**
     * Get IP address (privacy-conscious - first 3 octets only)
     */
    private String getIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getRemoteAddr();
                // For privacy: show first 3 octets only (192.168.1.xxx)
                if (ip != null && ip.contains(".")) {
                    String[] parts = ip.split("\\.");
                    if (parts.length == 4) {
                        return parts[0] + "." + parts[1] + "." + parts[2] + ".xxx";
                    }
                }
                return ip;
            }
        } catch (Exception e) {
            logger.debug("Could not get IP address", e);
        }
        return null;
    }

    /**
     * Convert event data map to JSON string
     */
    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            logger.warn("Failed to serialize event data to JSON", e);
            return data.toString();
        }
    }
}
