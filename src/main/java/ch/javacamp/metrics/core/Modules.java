package ch.javacamp.metrics.core;

import lombok.Getter;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
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
            List<String> coupledClassNames = other.classes().stream()
                    .filter(c -> c.hasDependency(classNamesCurrentModule))
                    .map(ClassDescriptor::className)
                    .collect(Collectors.toList());
            if (!coupledClassNames.isEmpty()) {
                result.add(new MetricsResult.ModuleCoupling(other.name(), coupledClassNames.size(), coupledClassNames, List.of(), List.of()));
            }
        }
        return result;
    }

    private List<MetricsResult.ModuleCoupling> computeEfferentModules(ModuleDescriptor module) {
        List<MetricsResult.ModuleCoupling> result = new ArrayList<>();
        for (var other : otherModules(module)) {
            var otherClassNames = other.allClassNames();
            List<String> coupledClassNames = module.classes().stream()
                    .filter(c -> c.hasDependency(otherClassNames))
                    .map(ClassDescriptor::className)
                    .collect(Collectors.toList());
            if (!coupledClassNames.isEmpty()) {
                // Collect referenced class names from target module
                List<String> referencedClassNames = module.classes().stream()
                        .flatMap(c -> c.dependencies().stream())
                        .filter(otherClassNames::contains)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
                // Build per-class dependency details
                List<MetricsResult.ClassDependencyDetail> classDependencies = module.classes().stream()
                        .filter(c -> c.hasDependency(otherClassNames))
                        .map(c -> {
                            List<String> refs = c.dependencies().stream()
                                    .filter(otherClassNames::contains)
                                    .sorted()
                                    .collect(Collectors.toList());
                            return new MetricsResult.ClassDependencyDetail(c.className(), refs);
                        })
                        .sorted(Comparator.comparing(MetricsResult.ClassDependencyDetail::className))
                        .collect(Collectors.toList());
                result.add(new MetricsResult.ModuleCoupling(other.name(), coupledClassNames.size(), coupledClassNames, referencedClassNames, classDependencies));
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
        // Build class abstractness index across all modules (for DIP)
        Map<String, Boolean> classAbstractnessIndex = new HashMap<>();
        for (var mod : modules) {
            for (var cls : mod.classes()) {
                classAbstractnessIndex.put(cls.className(), cls.isAbstract());
            }
        }

        // First pass: compute instability for all modules
        Map<String, Double> instabilities = new HashMap<>();
        for (var mod : modules) {
            var ca = externalClassesUsingClassesInThisModule(mod).size();
            var ce = externalClassesUsedByThisModule(mod).size();
            instabilities.put(mod.name(), (ca + ce) == 0 ? 0d : (double) ce / (double) (ce + ca));
        }

        // Second pass: compute full metrics with SDP violations and SOLID metrics
        List<MetricsResult> result = new ArrayList<>();
        for (ModuleDescriptor currentModule : getModules()) {
            var metrics = computeMetrics(currentModule);
            var sdpViolations = computeSdpViolations(currentModule, instabilities);
            result.add(MetricsResult.builder()
                    .name(metrics.name())
                    .numberOfClasses(metrics.numberOfClasses())
                    .totalLines(metrics.totalLines())
                    .ca(metrics.ca())
                    .ce(metrics.ce())
                    .instability(metrics.instability())
                    .abstractness(metrics.abstractness())
                    .distance(metrics.distance())
                    .lcom4(metrics.lcom4())
                    .averageMethodsPerClass(metrics.averageMethodsPerClass())
                    .averagePublicMethodsPerClass(metrics.averagePublicMethodsPerClass())
                    .shareGetterSetters(metrics.shareGetterSetters())
                    .shareLocalCallMethods(metrics.shareLocalCallMethods())
                    .methodStatistics(metrics.methodStatistics())
                    .afferentModules(metrics.afferentModules())
                    .efferentModules(metrics.efferentModules())
                    .publicApiSurface(metrics.publicApiSurface())
                    .averageCyclomaticComplexity(metrics.averageCyclomaticComplexity())
                    .maxCyclomaticComplexity(metrics.maxCyclomaticComplexity())
                    .circularDependencies(metrics.circularDependencies())
                    .sdpViolations(sdpViolations)
                    .dependencyInversionRatio(currentModule.dependencyInversionRatio(classAbstractnessIndex))
                    .averageMethodsPerInterface(currentModule.averageMethodsPerInterface())
                    .maxMethodsOnInterface(currentModule.maxMethodsOnInterface())
                    .build());
        }
        return result;
    }

    private List<MetricsResult.StabilityViolation> computeSdpViolations(ModuleDescriptor module, Map<String, Double> instabilities) {
        var myInstability = instabilities.getOrDefault(module.name(), 0d);
        List<MetricsResult.StabilityViolation> violations = new ArrayList<>();

        for (var other : otherModules(module)) {
            var otherClassNames = other.allClassNames();
            boolean dependsOn = module.classes().stream().anyMatch(c -> c.hasDependency(otherClassNames));
            if (dependsOn) {
                var otherInstability = instabilities.getOrDefault(other.name(), 0d);
                // SDP violation: I depend on a module that is MORE unstable than me
                if (otherInstability > myInstability + 0.01) {
                    violations.add(new MetricsResult.StabilityViolation(other.name(), myInstability, otherInstability));
                }
            }
        }
        return violations;
    }

}
