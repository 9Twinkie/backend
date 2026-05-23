-- Production-like набор правил: полный PromQL в metric_name, пороги как на типовом prod.
-- Старые демо-правила (cpu_usage / memory_usage / govno из V2) отключаем, не удаляем (FK incidents).

UPDATE alert_rules
SET is_active = FALSE
WHERE id IN (1, 2, 3);

INSERT INTO alert_rules (metric_name, operator, threshold, severity, is_active)
VALUES
    -- Доступность scrape-целей (job как в prometheus.yml на VM)
    ('up{job="node"}', '==', 0, 'CRITICAL', TRUE),
    ('up{job="govno"}', '==', 0, 'CRITICAL', TRUE),
    ('up{job="prometheus"}', '==', 0, 'HIGH', TRUE),

    -- node_exporter: CPU % (idle rate за 5m, агрегация по instance)
    ('100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle",job="node"}[5m])) * 100)', '>', 85, 'HIGH', TRUE),
    ('100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle",job="node"}[5m])) * 100)', '>', 95, 'CRITICAL', TRUE),

    -- Память: доля занятой RAM
    ('(1 - (node_memory_MemAvailable_bytes{job="node"} / node_memory_MemTotal_bytes{job="node"})) * 100', '>=', 85, 'HIGH', TRUE),
    ('(1 - (node_memory_MemAvailable_bytes{job="node"} / node_memory_MemTotal_bytes{job="node"})) * 100', '>=', 95, 'CRITICAL', TRUE),

    -- Диск: корневой mountpoint, без tmpfs/overlay
    ('(1 - (node_filesystem_avail_bytes{job="node",mountpoint="/",fstype!~"tmpfs|overlay|squashfs"} / node_filesystem_size_bytes{job="node",mountpoint="/",fstype!~"tmpfs|overlay|squashfs"})) * 100', '>=', 80, 'HIGH', TRUE),
    ('(1 - (node_filesystem_avail_bytes{job="node",mountpoint="/",fstype!~"tmpfs|overlay|squashfs"} / node_filesystem_size_bytes{job="node",mountpoint="/",fstype!~"tmpfs|overlay|squashfs"})) * 100', '>=', 95, 'CRITICAL', TRUE),

    -- Load average 1m (порог под типичный сервер; при необходимости подстройте)
    ('node_load1{job="node"}', '>', 8, 'MEDIUM', TRUE);
