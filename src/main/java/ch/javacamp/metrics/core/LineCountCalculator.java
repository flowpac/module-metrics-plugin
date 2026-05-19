package ch.javacamp.metrics.core;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LineCountCalculator {

    public LineCountResult computeModule(ModuleDescriptor descriptor) {

        var stats = new DescriptiveStatistics();
        descriptor.classes()
                .stream()
                .flatMap(x -> x.methods().stream())
                .filter(Predicate.not(MethodDescriptor::isSpecialMethod))
                .map(MethodDescriptor::lines)
                .filter(lines -> lines > 0)
                .forEach(stats::addValue);

        return new LineCountResult(stats);
    }

    public List<PackageLineCount> computePackages(ModuleDescriptor descriptor) {
        Map<String, Long> linesByPackage = descriptor.classes()
                .stream()
                .collect(Collectors.groupingBy(
                        cls -> {
                            String name = cls.className();
                            int dot = name.lastIndexOf('.');
                            return dot >= 0 ? name.substring(0, dot) : "(default)";
                        },
                        Collectors.summingLong(cls -> cls.methods().stream()
                                .mapToLong(MethodDescriptor::lines)
                                .sum())
                ));

        return linesByPackage.entrySet().stream()
                .map(e -> new PackageLineCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PackageLineCount::packageName))
                .collect(Collectors.toList());
    }

    @Getter
    @Accessors(fluent = true)
    public static class LineCountResult {

        private final DescriptiveStatistics stats;

        private LineCountResult(DescriptiveStatistics stats) {
            this.stats = stats;
        }

        public double percentile(double p) {
            return stats.getPercentile(p);
        }

        public double mean() {
            return stats.getMean();
        }

        public double median() {
            return stats.getPercentile(50);
        }
    }
}
