-- Seed a default test user for development
-- Password: "password" (hashed with BCrypt, cost factor 10)
-- In production, users should be created through a proper registration flow
INSERT INTO users (username, password_hash, active)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- BCrypt hash of "password"
    true
)
ON CONFLICT (username) DO NOTHING;
