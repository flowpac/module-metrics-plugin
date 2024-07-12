package ch.javacamp.metrics;

public record MetricsResult(long numberOfClasses, long ca, long ce, double abstractness, double instability, double distance) {
}
