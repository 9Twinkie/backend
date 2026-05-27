package com.monitoring.adapters.outbound.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HTTP-клиент к API Prometheus v1 ({@code /api/v1/...}).
 * PromQL содержит {@code {}} — собираем URL вручную, без UriBuilder (иначе Spring ломает шаблон).
 */
@Component
public class PrometheusHttpClient {

    private final RestClient restClient;
    private final String configuredBaseUrl;

    public PrometheusHttpClient(@Value("${monitoring.prometheus.url}") String prometheusBaseUrl) {
        this.configuredBaseUrl = prometheusBaseUrl.endsWith("/")
                ? prometheusBaseUrl.substring(0, prometheusBaseUrl.length() - 1)
                : prometheusBaseUrl;
        this.restClient = RestClient.builder().build();
    }

    /** URL из конфигурации (для диагностики). */
    public String configuredBaseUrl() {
        return configuredBaseUrl;
    }

    /**
     * Instant-запрос PromQL: {@code GET /query?query=...}
     */
    public Optional<JsonNode> queryInstant(String promql) {
        var query = encodeParam("query", promql);
        return get(buildUri("/query", query));
    }

    /**
     * Range-запрос для графиков: {@code GET /query_range}
     */
    public Optional<JsonNode> queryRange(String promql, long startEpochSec, long endEpochSec, String step) {
        var query = Stream.of(
                encodeParam("query", promql),
                encodeParam("start", Long.toString(startEpochSec)),
                encodeParam("end", Long.toString(endEpochSec)),
                encodeParam("step", step)
        ).collect(Collectors.joining("&"));
        return get(buildUri("/query_range", query));
    }

    /**
     * Правила recording/alerting: {@code GET /rules?type=alert}
     */
    public Optional<JsonNode> rules(String type) {
        var query = encodeParam("type", type);
        return get(buildUri("/rules", query));
    }

    /**
     * Активные scrape-таргеты: {@code GET /targets}
     */
    public Optional<JsonNode> targets() {
        return get(buildUri("/targets", null));
    }

    /**
     * Имена всех известных Prometheus метрик: {@code GET /label/__name__/values}
     */
    public Optional<JsonNode> metricNames() {
        return labelValues("__name__", List.of());
    }

    /**
     * Значения label с опциональными series selector {@code match[]}.
     */
    public Optional<JsonNode> labelValues(String labelName, List<String> matchSelectors) {
        var encodedLabel = URLEncoder.encode(labelName, StandardCharsets.UTF_8);
        var query = buildMatchQuery(matchSelectors);
        return get(buildUri("/label/" + encodedLabel + "/values", query));
    }

    /**
     * Все имена labels в TSDB: {@code GET /labels}
     */
    public Optional<JsonNode> labelNames(List<String> matchSelectors) {
        var query = buildMatchQuery(matchSelectors);
        return get(buildUri("/labels", query));
    }

    private static String buildMatchQuery(List<String> matchSelectors) {
        if (matchSelectors == null || matchSelectors.isEmpty()) {
            return null;
        }
        return matchSelectors.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> encodeParam("match[]", s))
                .collect(Collectors.joining("&"));
    }

    private URI buildUri(String path, String queryString) {
        var sb = new StringBuilder(configuredBaseUrl).append(path);
        if (queryString != null && !queryString.isBlank()) {
            sb.append('?').append(queryString);
        }
        return URI.create(sb.toString());
    }

    private static String encodeParam(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Optional<JsonNode> get(URI uri) {
        try {
            var body = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            return Optional.ofNullable(body);
        } catch (RestClientException ex) {
            return Optional.empty();
        }
    }
}
