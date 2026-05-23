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
                if (alerts.isArray() && !alerts.isEmpty()) {
                    for (var alert : alerts) {
                        if ("firing".equals(alert.path("state").asText())) {
                            addAlert(result, alert.path("labels"), expr, ruleName);
                        }
                    }
                } else if ("firing".equals(rule.path("state").asText())) {
                    addAlert(result, rule.path("labels"), expr, ruleName);
                }
            }
        }
        return result;
    }

    private static void addAlert(
            List<PrometheusFiringAlert> result,
            JsonNode labelsNode,
            String expr,
            String ruleName
    ) {
        var labels = toLabelMap(labelsNode);
        if (!labels.containsKey("alertname")) {
            labels.put("alertname", ruleName);
        }
        var fingerprint = PrometheusAlertFingerprint.fromLabels(labels);
        var alertName = labels.get("alertname");
        var severity = mapSeverity(labels);
        result.add(new PrometheusFiringAlert(fingerprint, alertName, expr, severity, Map.copyOf(labels)));
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
        var raw = labels.getOrDefault("severity", labels.getOrDefault("priority", "high"));
        return switch (raw.toLowerCase()) {
            case "critical", "crit" -> Severity.CRITICAL;
            case "warning", "warn" -> Severity.MEDIUM;
            case "info", "low" -> Severity.LOW;
            case "high" -> Severity.HIGH;
            default -> {
                try {
                    yield Severity.valueOf(raw.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    yield Severity.HIGH;
                }
            }
        };
    }

    private static boolean isSuccess(JsonNode root) {
        return root.hasNonNull("status") && "success".equals(root.get("status").asText());
    }
}
