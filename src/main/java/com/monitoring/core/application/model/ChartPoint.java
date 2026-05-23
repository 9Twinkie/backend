package com.monitoring.core.application.model;

/**
 * Одна точка временного ряда для графика (timestamp — Unix epoch, секунды).
 */
public record ChartPoint(long timestamp, double value) {
}
