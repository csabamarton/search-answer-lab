-- Create search_metrics table for tracking search performance
CREATE TABLE search_metrics (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(100) NOT NULL,
    trace_id VARCHAR(100),
    query TEXT NOT NULL,
    query_hash VARCHAR(64) NOT NULL,
    duration_ms BIGINT NOT NULL,
    results_count INTEGER NOT NULL,
    search_mode VARCHAR(50) NOT NULL,
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common query patterns
CREATE INDEX idx_metrics_request_id ON search_metrics(request_id);
CREATE INDEX idx_metrics_trace_id ON search_metrics(trace_id);
CREATE INDEX idx_metrics_query_hash ON search_metrics(query_hash);
CREATE INDEX idx_metrics_search_mode ON search_metrics(search_mode);
CREATE INDEX idx_metrics_created_at ON search_metrics(created_at DESC);

-- Create composite index for analytics queries
CREATE INDEX idx_metrics_mode_status_created ON search_metrics(search_mode, status, created_at DESC);
