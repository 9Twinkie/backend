package com.monitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monitoring.alert-evaluation")
public class AlertEvaluationProperties {

    private boolean enabled = true;
    private long intervalMs = 30_000L;
    private long defaultNotifyEngineerId = 2L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
