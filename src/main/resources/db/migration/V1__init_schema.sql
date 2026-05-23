-- Инициализация схемы мониторинга: инженеры, правила, инциденты, уведомления

CREATE TABLE engineers (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ENGINEER', 'ADMIN')),
    phone VARCHAR(50),
    notification_prefs VARCHAR(255)
);

CREATE TABLE alert_rules (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(255) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    threshold DOUBLE PRECISION NOT NULL,
    severity VARCHAR(50) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_alertrules_metric_active ON alert_rules (metric_name, is_active);

CREATE TABLE incidents (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL REFERENCES alert_rules (id),
    timestamp TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('NEW', 'CONFIRMED', 'CLOSED')),
    assigned_engineer_id BIGINT REFERENCES engineers (id),
    resolved_at TIMESTAMP
);

CREATE INDEX idx_incidents_engineer_status ON incidents (assigned_engineer_id, status);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents (id),
    engineer_id BIGINT NOT NULL REFERENCES engineers (id),
    channel VARCHAR(50) NOT NULL,
    delivered BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP
);

CREATE INDEX idx_notifications_engineer ON notifications (engineer_id);

-- Сиды: пароли захешированы BCrypt (стоимость 10) через bcryptjs; admin=admin, engineer1=eng1
INSERT INTO engineers (id, username, password_hash, role, phone, notification_prefs)
VALUES (1, 'admin', '$2b$10$jPxn0EAaaGEegUFPD0rVGO1El6CRDNsOJ83N9EDRhiD5EpLK.nCm.', 'ADMIN', '+70000000001', 'email,sms'),
       (2, 'engineer1', '$2b$10$oTfG.JdBTH5lHmYkLm8.2OYqEOGkFNQ2n.gXxXkHzYA6edV0T4Ory', 'ENGINEER', '+70000000002', 'email');

INSERT INTO alert_rules (id, metric_name, operator, threshold, severity, is_active)
VALUES (1, 'cpu_usage', '>', 85.0, 'HIGH', TRUE),
       (2, 'memory_usage', '>=', 90.0, 'CRITICAL', TRUE);

INSERT INTO incidents (id, rule_id, timestamp, status, assigned_engineer_id, resolved_at)
VALUES (1, 1, CURRENT_TIMESTAMP, 'NEW', NULL, NULL);

INSERT INTO notifications (id, incident_id, engineer_id, channel, delivered, read_at)
VALUES (1, 1, 2, 'email', FALSE, NULL);

SELECT setval(pg_get_serial_sequence('engineers', 'id'), COALESCE((SELECT MAX(id) FROM engineers), 1));
SELECT setval(pg_get_serial_sequence('alert_rules', 'id'), COALESCE((SELECT MAX(id) FROM alert_rules), 1));
SELECT setval(pg_get_serial_sequence('incidents', 'id'), COALESCE((SELECT MAX(id) FROM incidents), 1));
SELECT setval(pg_get_serial_sequence('notifications', 'id'), COALESCE((SELECT MAX(id) FROM notifications), 1));
