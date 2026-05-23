package com.monitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monitoring.alert-evaluation")
public class AlertEvaluationProperties {

    private boolean enabled = true;
    /** prometheus — правила в Prometheus; database — legacy alert_rules в БД */
    private String source = "prometheus";
    private long intervalMs = 30_000L;
    private long defaultNotifyEngineerId = 2L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isPrometheusSource() {
        return source == null || source.isBlank() || "prometheus".equalsIgnoreCase(source);
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public long getDefaultNotifyEngineerId() {
        return defaultNotifyEngineerId;
    }

    public void setDefaultNotifyEngineerId(long defaultNotifyEngineerId) {
        this.defaultNotifyEngineerId = defaultNotifyEngineerId;
    }
}
