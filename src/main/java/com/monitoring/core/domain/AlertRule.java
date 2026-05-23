package com.monitoring.core.domain;

import java.util.Objects;

/**
 * Правило оповещения по метрике. Оператор сравнивается с порогом при оценке фактического значения.
 */
public record AlertRule(
        Long id,
        String metricName,
        String operator,
        Double threshold,
        Severity severity,
        Boolean isActive
) {
    public AlertRule {
        Objects.requireNonNull(metricName, "metricName обязателен");
        Objects.requireNonNull(operator, "operator обязателен");
        Objects.requireNonNull(threshold, "threshold обязателен");
        Objects.requireNonNull(severity, "severity обязателен");
        Objects.requireNonNull(isActive, "isActive обязателен");
    }

    /**
     * Проверяет, сработало ли правило для переданного значения метрики.
     *
     * @param actualValue фактическое значение метрики
     * @return true, если условие выполнено
     */
    public boolean evaluate(double actualValue) {
        var op = operator.trim();
        return switch (op) {
            case ">", "GT" -> actualValue > threshold;
            case ">=", "GE" -> actualValue >= threshold;
            case "<", "LT" -> actualValue < threshold;
            case "<=", "LE" -> actualValue <= threshold;
            case "==", "EQ" -> Double.compare(actualValue, threshold) == 0;
            case "!=", "NE" -> Double.compare(actualValue, threshold) != 0;
            default -> throw new IllegalStateException("Неизвестный оператор сравнения: " + operator);
        };
    }
}
