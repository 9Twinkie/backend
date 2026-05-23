package com.monitoring.core.application.usecases;

import com.monitoring.core.domain.AlertRule;

/**
 * metric_name в БД: либо устаревший псевдоним (cpu_usage), либо полный PromQL (как в prod).
 */
public final class PromqlResolver {

    /** CPU % по node_exporter, job=node. */
    public static final String NODE_CPU_PERCENT =
            "100 - (avg by (instance) (rate(node_cpu_seconds_total{mode=\"idle\",job=\"node\"}[5m])) * 100)";

    /** Занятая RAM % по node_exporter. */
    public static final String NODE_MEMORY_PERCENT =
            "(1 - (node_memory_MemAvailable_bytes{job=\"node\"} / node_memory_MemTotal_bytes{job=\"node\"})) * 100";

    /** Занятое место на / % по node_exporter. */
    public static final String NODE_DISK_ROOT_PERCENT =
            "(1 - (node_filesystem_avail_bytes{job=\"node\",mountpoint=\"/\",fstype!~\"tmpfs|overlay|squashfs\"} "
                    + "/ node_filesystem_size_bytes{job=\"node\",mountpoint=\"/\",fstype!~\"tmpfs|overlay|squashfs\"})) * 100";

    private PromqlResolver() {
    }

    public static String toPromql(AlertRule rule) {
        return switch (rule.metricName()) {
            case "cpu_usage" -> NODE_CPU_PERCENT;
            case "memory_usage" -> NODE_MEMORY_PERCENT;
            default -> rule.metricName();
        };
    }
}
