-- Пороги RAM для теста на VM (~28% занято при 1.07/3.79 GiB на host-128).
-- Формула: (1 - MemAvailable/MemTotal) * 100  →  HIGH сработает при >= 15%.

UPDATE alert_rules
SET threshold = 15
WHERE is_active = TRUE
  AND operator IN ('>=', 'GE')
  AND severity = 'HIGH'
  AND metric_name LIKE '%node_memory_MemAvailable_bytes%';

UPDATE alert_rules
SET threshold = 30
WHERE is_active = TRUE
  AND operator IN ('>=', 'GE')
  AND severity = 'CRITICAL'
  AND metric_name LIKE '%node_memory_MemAvailable_bytes%';
