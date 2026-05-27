package com.monitoring.adapters.outbound.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import com.monitoring.core.application.model.PrometheusFiringAlert;
import com.monitoring.core.domain.Severity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class PrometheusRulesAlertParser {

    private PrometheusRulesAlertParser() {
    }

    public static List<PrometheusFiringAlert> parse(JsonNode root) {
        var result = new ArrayList<PrometheusFiringAlert>();
        if (root == null || !isSuccess(root)) {
            return result;
        }
        var groups = root.path("data").path("groups");
        if (!groups.isArray()) {
            return result;
        }
        for (var group : groups) {
            var rules = group.path("rules");
            if (!rules.isArray()) {
                continue;
            }
            for (var rule : rules) {
                if (!"alerting".equals(rule.path("type").asText())) {
                    continue;
                }
                var expr = rule.path("query").asText(null);
                var ruleName = rule.path("name").asText("alert");
                var alerts = rule.path("alerts");
                var ruleLabels = toLabelMap(rule.path("labels"));
                var ruleAnnotations = toLabelMap(rule.path("annotations"));
                if (alerts.isArray() && !alerts.isEmpty()) {
                    for (var alert : alerts) {
                        if ("firing".equals(alert.path("state").asText())) {
                            var mergedLabels = mergeLabels(ruleLabels, alert.path("labels"));
                            var mergedAnnotations = mergeLabels(ruleAnnotations, alert.path("annotations"));
                            addAlert(result, mergedLabels, mergedAnnotations, expr, ruleName);
                        }
                    }
                } else if ("firing".equals(rule.path("state").asText())) {
                    addAlert(result, ruleLabels, ruleAnnotations, expr, ruleName);
                }
            }
        }
        return result;
    }

    private static void addAlert(
            List<PrometheusFiringAlert> result,
            Map<String, String> labels,
            Map<String, String> annotations,
            String expr,
            String ruleName
    ) {
        if (!labels.containsKey("alertname")) {
            labels.put("alertname", ruleName);
        }
        var fingerprint = PrometheusAlertFingerprint.fromLabels(labels);
        var alertName = labels.get("alertname");
        var severity = mapSeverity(labels, annotations);
        var summary = trimToNull(PrometheusAnnotationRenderer.render(annotations.get("summary"), labels));
        var description = trimToNull(PrometheusAnnotationRenderer.pickDisplayText(annotations, labels));
        result.add(new PrometheusFiringAlert(
                fingerprint,
                alertName,
                expr,
                severity,
                summary,
                description,
                Map.copyOf(labels)
        ));
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Map<String, String> mergeLabels(Map<String, String> base, JsonNode overrideNode) {
        var merged = new HashMap<>(base);
        merged.putAll(toLabelMap(overrideNode));
        return merged;
    }

    private static Map<String, String> toLabelMap(JsonNode labelsNode) {
        var labels = new HashMap<String, String>();
        if (labelsNode == null || !labelsNode.isObject()) {
            return labels;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = labelsNode.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            labels.put(entry.getKey(), entry.getValue().asText());
        }
        return labels;
    }

    static Severity mapSeverity(Map<String, String> labels) {
        return mapSeverity(labels, Map.of());
    }

    static Severity mapSeverity(Map<String, String> labels, Map<String, String> annotations) {
        var raw = resolveRawSeverity(labels, annotations);
        var normalized = raw.toLowerCase().trim();
        return switch (normalized) {
            case "critical", "crit", "fatal", "emergency" -> Severity.CRITICAL;
            case "warning", "warn" -> Severity.MEDIUM;
            case "info", "information", "informational", "inform", "notice", "low", "minor" -> Severity.LOW;
            case "high", "major", "page" -> Severity.HIGH;
            default -> {
                try {
                    yield Severity.valueOf(normalized.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    yield inferFromText(normalized);
                }
            }
        };
    }

    private static String resolveRawSeverity(Map<String, String> labels, Map<String, String> annotations) {
        for (var key : List.of("severity", "priority", "level")) {
            var fromLabels = labels.get(key);
            if (fromLabels != null && !fromLabels.isBlank()) {
                return fromLabels.trim();
            }
            var fromAnnotations = annotations.get(key);
            if (fromAnnotations != null && !fromAnnotations.isBlank()) {
                return fromAnnotations.trim();
            }
        }
        return "high";
    }

    private static Severity inferFromText(String normalized) {
        if (normalized.contains("info") || normalized.contains("notice")) {
            return Severity.LOW;
        }
        if (normalized.contains("warn")) {
            return Severity.MEDIUM;
        }
        if (normalized.contains("crit") || normalized.contains("fatal")) {
            return Severity.CRITICAL;
        }
        if (normalized.contains("high") || normalized.contains("major") || normalized.contains("page")) {
            return Severity.HIGH;
        }
        return Severity.LOW;
    }

    private static boolean isSuccess(JsonNode root) {
        return root.hasNonNull("status") && "success".equals(root.get("status").asText());
    }
}
