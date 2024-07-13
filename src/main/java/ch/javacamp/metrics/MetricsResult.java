package ch.javacamp.metrics;

public record MetricsResult(String name, long numberOfClasses, long ca, long ce, double abstractness, double instability, double distance, double averageMethodsPerClass, double averagePublicMethodsPerClass) {
}
