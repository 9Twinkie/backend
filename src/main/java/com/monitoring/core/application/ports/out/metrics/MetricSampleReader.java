package com.monitoring.core.application.ports.out.metrics;

import java.util.Optional;

/**
 * Чтение текущего значения метрики (instant query).
 */
public interface MetricSampleReader {

    Optional<Double> readInstant(String query);
}
