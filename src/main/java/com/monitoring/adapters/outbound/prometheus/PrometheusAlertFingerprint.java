package com.monitoring.adapters.outbound.prometheus;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class PrometheusAlertFingerprint {

    private PrometheusAlertFingerprint() {
    }

    public static String fromLabels(Map<String, String> labels) {
        var sorted = new TreeMap<>(labels);
        var body = sorted.entrySet().stream()
                .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                .collect(Collectors.joining(","));
        var name = sorted.getOrDefault("alertname", "alert");
        return name + "{" + body + "}";
    }
}
