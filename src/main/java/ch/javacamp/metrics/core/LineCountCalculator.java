package ch.javacamp.metrics.core;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.function.Predicate;

public class LineCountCalculator {

    public LineCountResult computeModule(ModuleDescriptor descriptor) {

        var stats = new DescriptiveStatistics();
        descriptor.classes()
                .stream()
                .flatMap(x -> x.methods().stream())
                .filter(Predicate.not(MethodDescriptor::isSpecialMethod))
                .map(MethodDescriptor::lines)
                .forEach(stats::addValue);

        return new LineCountResult(stats);
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
