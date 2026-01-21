-- Update admin user password with a verified BCrypt hash
-- Password: "password" (BCrypt cost factor 10)
-- This hash was generated using BCryptPasswordEncoder and verified to work
-- To generate a new hash, use: POST /test/password/hash with {"password": "password"}
UPDATE users 
SET password_hash = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi'
WHERE username = 'admin';

-- If admin user doesn't exist, create it
INSERT INTO users (username, password_hash, active)
SELECT 'admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
