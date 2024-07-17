package ch.javacamp.metrics.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Builder()
@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MetricsResult {

    private final String name;
    private final long numberOfClasses;
    private final long ca;
    private final long ce;
    private final double abstractness;
    private final double instability;
    private final double distance;
    private final double averageMethodsPerClass;
    private final double averagePublicMethodsPerClass;
    private final double lcom4;
    private final double shareGetterSetters;
    private final double shareLocalCallMethods;
    private final double linesPerMethod;

}
