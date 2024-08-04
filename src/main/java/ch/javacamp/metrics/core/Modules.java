package ch.javacamp.metrics.core;

import lombok.Getter;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Getter
public class Modules {

    private final List<ModuleDescriptor> modules = new ArrayList<>();

    public void addModule(ModuleDescriptor moduleDescriptor) {
        if (moduleDescriptor.totalClasses() > 0) {
            this.modules.add(moduleDescriptor);
        }
    }

    public Set<ModuleDescriptor> otherModules(ModuleDescriptor toExclude) {
        return modules.stream().filter(x -> x != toExclude).collect(Collectors.toSet());
    }

    private Set<ClassDescriptor> externalClassesUsingClassesInThisModule(ModuleDescriptor module) {
        var classNamesCurrentModule = module.allClassNames();
        var otherModules = otherModules(module);

        return otherModules
                .stream()
                .flatMap(x -> x.classes().stream())
                .filter(c -> c.hasDependency(classNamesCurrentModule))
                .collect(Collectors.toSet());
    }

    private Set<ClassDescriptor> externalClassesUsedByThisModule(ModuleDescriptor module) {
        Set<ClassDescriptor> result = new HashSet<>();
        var allClassNamesOutsideCurrentModule = otherModules(module).stream()
                .flatMap(x -> x.classes().stream())
                .map(ClassDescriptor::className)
                .collect(Collectors.toSet());

        for (var classDescriptor : module.classes()) {
            for (var dependency : classDescriptor.dependencies()) {
                if (allClassNamesOutsideCurrentModule.contains(dependency)) {
                    result.add(classDescriptor);
                }
            }
        }
        return result;
    }

    public MetricsResult computeMetrics(ModuleDescriptor module) {
        var outsideClassesWithDependenciesToMe = externalClassesUsingClassesInThisModule(module);
        var classesWithForeignDependencies = externalClassesUsedByThisModule(module);

        var ca = outsideClassesWithDependenciesToMe.size();
        var ce = classesWithForeignDependencies.size();
        var i = (double) ce / (double) (ce + ca);
        var a = module.abstractness();
        var d = Math.abs((a + i - 1) / 2.0);
        var lcom4 = module.averageLCOM4();
        var shareOfGetterSetters = module.shareOfGetterSetters();
        var shareOfMethodsWithLocalCalls = module.shareOfMethodsWithLocalCalls();
        var methodStatistics = computeMethodStatistics(module);

        return new MetricsResult.MetricsResultBuilder()
                .numberOfClasses(module.totalClasses())
                .name(module.name())
                .ca(ca)
                .ce(ce)
                .instability(i)
                .abstractness(a)
                .distance(d)
                .lcom4(lcom4)
                .shareGetterSetters(shareOfGetterSetters)
                .shareLocalCallMethods(shareOfMethodsWithLocalCalls)
                .methodStatistics(methodStatistics)
                .build();
    }

    private static MetricsResult.MethodStatistics computeMethodStatistics(ModuleDescriptor module) {
        var lineCountResult = new LineCountCalculator().computeModule(module);
        return MetricsResult.MethodStatistics.builder()
                .median(lineCountResult.median())
                .mean(lineCountResult.mean())
                .percentile25(lineCountResult.percentile(25))
                .percentile75(lineCountResult.percentile(75))
                .percentile80(lineCountResult.percentile(80))
                .percentile90(lineCountResult.percentile(90))
                .percentile95(lineCountResult.percentile(95))
                .percentile99(lineCountResult.percentile(99)).build();
    }

    public List<MetricsResult> computeMetrics() {
        List<MetricsResult> result = new ArrayList<>();
        // new CallFlowCalculator().calculate(modules);
        for (ModuleDescriptor currentModule : getModules()) {
            result.add(computeMetrics(currentModule));
        }
        return result;
    }

}
