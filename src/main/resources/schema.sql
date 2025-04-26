-- ======================
-- Таблица пользователей
-- ======================

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('ADMIN', 'USER'))
);

-- ======================
-- Таблица OTP-кодов
-- ======================

CREATE TABLE IF NOT EXISTS otp_codes (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    operation_id VARCHAR(255) NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED')),
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Индекс для ускорения поиска по user_id и operation_id
CREATE INDEX IF NOT EXISTS idx_otp_user_operation ON otp_codes (user_id, operation_id);

-- ======================
-- Таблица конфигурации OTP
-- ======================

CREATE TABLE IF NOT EXISTS otp_config (
    id SERIAL PRIMARY KEY,
    code_length INT NOT NULL DEFAULT 6,
    ttl_seconds INT NOT NULL DEFAULT 300
);

-- Добавление начальной строки конфигурации, если таблица пуста
INSERT INTO otp_config (code_length, ttl_seconds)
SELECT 6, 300
WHERE NOT EXISTS (SELECT 1 FROM otp_config);
