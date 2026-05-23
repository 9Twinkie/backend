-- RAM HIGH: срабатывание при >= 15% занятой памяти (на VM ~28% — точно триггер).

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
