-- Create audit_events table for tracking all security-related operations
-- Records who did what, when, and the outcome

CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    tool_name VARCHAR(100),
    action VARCHAR(50),
    event_data TEXT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    duration_ms BIGINT,
    request_id VARCHAR(100),
    ip_address VARCHAR(50),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_events(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_events(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_status ON audit_events(status);

-- Add comment to table
COMMENT ON TABLE audit_events IS 'Audit log for all security-related operations including search requests, authentication events, and token operations';
