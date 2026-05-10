package ch.javacamp.metrics.core;

import lombok.Getter;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        var i = (ca + ce) == 0 ? 0d : (double) ce / (double) (ce + ca);
        var a = module.abstractness();
        var d = Math.abs(a + i - 1);
        var lcom4 = module.averageLCOM4();
        var shareOfGetterSetters = module.shareOfGetterSetters();
        var shareOfMethodsWithLocalCalls = module.shareOfMethodsWithLocalCalls();
        var methodStatistics = computeMethodStatistics(module);

        return new MetricsResult.MetricsResultBuilder()
                .numberOfClasses(module.totalClasses())
                .totalLines(module.totalLines())
                .name(module.name())
                .ca(ca)
                .ce(ce)
                .instability(i)
                .abstractness(a)
                .distance(d)
                .lcom4(lcom4)
                .averageMethodsPerClass(module.averageMethodsPerClass())
                .averagePublicMethodsPerClass(module.averagePublicMethodsPerClass())
                .shareGetterSetters(shareOfGetterSetters)
                .shareLocalCallMethods(shareOfMethodsWithLocalCalls)
                .methodStatistics(methodStatistics)
                .afferentModules(computeAfferentModules(module))
                .efferentModules(computeEfferentModules(module))
                .publicApiSurface(module.publicApiSurface())
                .averageCyclomaticComplexity(module.averageCyclomaticComplexity())
                .maxCyclomaticComplexity(module.maxCyclomaticComplexity())
                .circularDependencies(findCircularDependencies(module))
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

    private List<MetricsResult.ModuleCoupling> computeAfferentModules(ModuleDescriptor module) {
        var classNamesCurrentModule = module.allClassNames();
        List<MetricsResult.ModuleCoupling> result = new ArrayList<>();
        for (var other : otherModules(module)) {
            long count = other.classes().stream()
                    .filter(c -> c.hasDependency(classNamesCurrentModule))
                    .count();
            if (count > 0) {
                result.add(new MetricsResult.ModuleCoupling(other.name(), count));
            }
        }
        return result;
    }

    private List<MetricsResult.ModuleCoupling> computeEfferentModules(ModuleDescriptor module) {
        List<MetricsResult.ModuleCoupling> result = new ArrayList<>();
        for (var other : otherModules(module)) {
            var otherClassNames = other.allClassNames();
            long count = module.classes().stream()
                    .filter(c -> c.hasDependency(otherClassNames))
                    .count();
            if (count > 0) {
                result.add(new MetricsResult.ModuleCoupling(other.name(), count));
            }
        }
        return result;
    }

    private List<List<String>> findCircularDependencies(ModuleDescriptor module) {
        // Build adjacency map for all modules
        Map<String, Set<String>> graph = new HashMap<>();
        for (var mod : modules) {
            Set<String> deps = new HashSet<>();
            for (var other : otherModules(mod)) {
                var otherClassNames = other.allClassNames();
                boolean hasDep = mod.classes().stream().anyMatch(c -> c.hasDependency(otherClassNames));
                if (hasDep) deps.add(other.name());
            }
            graph.put(mod.name(), deps);
        }

        // DFS to find cycles starting from this module
        List<List<String>> cycles = new ArrayList<>();
        findCycles(graph, module.name(), module.name(), new LinkedList<>(), new HashSet<>(), cycles);
        return cycles;
    }

    private void findCycles(Map<String, Set<String>> graph, String start, String current,
                            LinkedList<String> path, Set<String> visited, List<List<String>> cycles) {
        path.add(current);
        visited.add(current);

        for (var neighbor : graph.getOrDefault(current, Set.of())) {
            if (neighbor.equals(start) && path.size() > 1) {
                var cycle = new ArrayList<>(path);
                cycle.add(start);
                cycles.add(cycle);
            } else if (!visited.contains(neighbor)) {
                findCycles(graph, start, neighbor, path, visited, cycles);
            }
        }

        path.removeLast();
        visited.remove(current);
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
