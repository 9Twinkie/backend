-- Правило: таргет govno недоступен (up == 0). metric_name хранит PromQL.
INSERT INTO alert_rules (metric_name, operator, threshold, severity, is_active)
VALUES ('up{job="govno"}', '==', 0, 'CRITICAL', TRUE);
