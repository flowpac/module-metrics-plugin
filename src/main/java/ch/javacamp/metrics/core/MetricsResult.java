package ch.javacamp.metrics.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Builder()
@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MetricsResult {

    private final String name;
    private final long numberOfClasses;
    private final long totalLines;
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
    private final MethodStatistics methodStatistics;
    private final List<ModuleCoupling> afferentModules;
    private final List<ModuleCoupling> efferentModules;
    private final long publicApiSurface;
    private final double averageCyclomaticComplexity;
    private final int maxCyclomaticComplexity;
    private final List<List<String>> circularDependencies;

    @Builder
    @Getter
    @Accessors(fluent = true)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MethodStatistics{
        double mean, median, percentile25, percentile75, percentile80, percentile90, percentile95, percentile99;
    }

    @Builder
    @Getter
    @Accessors(fluent = true)
    @AllArgsConstructor
    public static class ModuleCoupling {
        private final String moduleName;
        private final long classCount;
    }
}
