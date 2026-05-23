-- Инциденты из firing-алертов Prometheus (правила в prometheus.yml, не в alert_rules).

ALTER TABLE incidents
    ALTER COLUMN rule_id DROP NOT NULL;

ALTER TABLE incidents
    ADD COLUMN prometheus_fingerprint VARCHAR(512),
    ADD COLUMN prometheus_alert_name VARCHAR(255),
    ADD COLUMN prometheus_expr TEXT,
    ADD COLUMN prometheus_severity VARCHAR(50);

CREATE INDEX idx_incidents_prometheus_fingerprint ON incidents (prometheus_fingerprint);

-- На бэкенде правила больше не оцениваем — только Prometheus.
UPDATE alert_rules SET is_active = FALSE;
