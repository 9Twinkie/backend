package com.monitoring.adapters.outbound.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Читает одно числовое значение из ответа Prometheus instant query.
 */
@Component
public class PrometheusScalarReader {

    private final PrometheusHttpClient client;

    public PrometheusScalarReader(PrometheusHttpClient client) {
        this.client = client;
    }

    /**
     * @param promql выражение PromQL
     * @return значение метрики или empty, если запрос неуспешен / нет данных
     */
    public Optional<Double> readScalar(String promql) {
        return client.queryInstant(promql).flatMap(this::extractFirstValue);
    }

    private Optional<Double> extractFirstValue(JsonNode root) {
        if (!root.hasNonNull("status") || !"success".equals(root.get("status").asText())) {
            return Optional.empty();
        }
        var result = root.path("data").path("result");
        if (!result.isArray() || result.isEmpty()) {
            return Optional.empty();
        }
        var valueNode = result.get(0).path("value");
        if (!valueNode.isArray() || valueNode.size() < 2) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(valueNode.get(1).asText()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
