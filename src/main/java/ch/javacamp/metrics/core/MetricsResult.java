package ch.javacamp.metrics.core;

public record MetricsResult(String name, long numberOfClasses, long ca, long ce, double abstractness,
                            double instability, double distance, double averageMethodsPerClass,
                            double averagePublicMethodsPerClass, double lcom4, double shareGetterSetters, double shareLocalCallMethods) {
}
