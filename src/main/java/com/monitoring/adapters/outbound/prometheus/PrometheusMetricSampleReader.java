package com.monitoring.adapters.outbound.prometheus;

import com.monitoring.core.application.ports.out.metrics.MetricSampleReader;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PrometheusMetricSampleReader implements MetricSampleReader {

    private final PrometheusScalarReader scalarReader;

    public PrometheusMetricSampleReader(PrometheusScalarReader scalarReader) {
        this.scalarReader = scalarReader;
    }

    @Override
    public Optional<Double> readInstant(String query) {
        return scalarReader.readScalar(query);
    }
}
