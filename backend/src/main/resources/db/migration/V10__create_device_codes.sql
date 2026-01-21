-- Create device_codes table for OAuth Device Code Flow
CREATE TABLE device_codes (
    id BIGSERIAL PRIMARY KEY,
    device_code VARCHAR(100) NOT NULL UNIQUE,
    user_code VARCHAR(20) NOT NULL UNIQUE,
    verification_uri VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    interval_seconds INTEGER NOT NULL DEFAULT 5,
    user_id BIGINT,
    authorized_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_code_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for fast lookups
CREATE INDEX idx_device_codes_device_code ON device_codes(device_code);
CREATE INDEX idx_device_codes_user_code ON device_codes(user_code);
CREATE INDEX idx_device_codes_expires_at ON device_codes(expires_at);

-- Create index on user_id for authorization lookups
CREATE INDEX idx_device_codes_user_id ON device_codes(user_id);
