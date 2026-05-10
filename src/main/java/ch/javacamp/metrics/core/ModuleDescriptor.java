package ch.javacamp.metrics.core;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record ModuleDescriptor(String name, Set<ClassDescriptor> classes) {


    public long abstractClasses() {
        return classes.stream().filter(ClassDescriptor::isAbstract).count();
    }

    public long totalClasses() {
        return classes.size();
    }

    public Set<String> allClassNames() {
        return classes.stream().map(ClassDescriptor::className).collect(Collectors.toSet());
    }

    public double abstractness() {
        return (double) abstractClasses() / (double) totalClasses();
    }

    public double averageLCOM4(){
        var relevantClasses = classes.stream()
                .filter(c -> !c.getFilteredMethods().isEmpty())
                .toList();
        if (relevantClasses.isEmpty()) {
            return 0d;
        }
        var sumLCOM4 = relevantClasses.stream().map(ClassDescriptor::lcom4).reduce(Integer::sum).orElse(0);
        return (double) sumLCOM4 / (double) relevantClasses.size();
    }

    public double shareOfGetterSetters(){
        var sumGetterSetters = classes.stream().map(ClassDescriptor::countMethodsWithOnlyOneInvolvedField).reduce(Integer::sum).orElse(0);
        var allMethods = classes.stream().map(ClassDescriptor::countMethodsInClass).reduce(Integer::sum).orElse(0);
        return (double)sumGetterSetters / (double) allMethods;
    }

    public double shareOfMethodsWithLocalCalls(){
        var sumGetterSetters = classes.stream().map(ClassDescriptor::countMethodsWithLocalCalls).reduce(Integer::sum).orElse(0);
        var allMethods = classes.stream().map(ClassDescriptor::countMethodsInClass).reduce(Integer::sum).orElse(0);
        return (double)sumGetterSetters / (double) allMethods;
    }

    public double averageMethodsPerClass() {
        var methods = classes.stream().map(ClassDescriptor::countMethodsInClass).reduce(Integer::sum).orElse(0);
        return (double) methods / (double) totalClasses();
    }

    public double averagePublicMethodsPerClass() {
        var methods = classes.stream().map(ClassDescriptor::countPublicMethodsInClass).reduce(Integer::sum).orElse(0);
        return (double) methods / (double) totalClasses();
    }

    public long totalLines() {
        return classes.stream()
                .flatMap(c -> c.methods().stream())
                .mapToLong(MethodDescriptor::lines)
                .sum();
    }

    public long publicApiSurface() {
        return classes.stream()
                .filter(c -> c.visibility() == Visibility.PUBLIC)
                .flatMap(c -> c.methods().stream())
                .filter(m -> !m.isConstructor() && m.isPublic())
                .count();
    }

    public double averageCyclomaticComplexity() {
        var methods = classes.stream()
                .flatMap(c -> c.methods().stream())
                .filter(m -> !m.isSpecialMethod())
                .filter(m -> m.lines() > 0)
                .toList();
        if (methods.isEmpty()) return 0d;
        var sum = methods.stream().mapToInt(MethodDescriptor::cyclomaticComplexity).sum();
        return (double) sum / methods.size();
    }

    public int maxCyclomaticComplexity() {
        return classes.stream()
                .flatMap(c -> c.methods().stream())
                .filter(m -> !m.isSpecialMethod())
                .mapToInt(MethodDescriptor::cyclomaticComplexity)
                .max().orElse(0);
    }

    /**
     * DIP: ratio of dependencies pointing to abstract types vs. all resolved dependencies.
     * Only counts dependencies that can be resolved within the known modules.
     */
    public double dependencyInversionRatio(Map<String, Boolean> classAbstractnessIndex) {
        long abstractDeps = 0, totalDeps = 0;
        for (var cls : classes) {
            for (var dep : cls.dependencies()) {
                if (classAbstractnessIndex.containsKey(dep)) {
                    totalDeps++;
                    if (classAbstractnessIndex.get(dep)) {
                        abstractDeps++;
                    }
                }
            }
        }
        return totalDeps == 0 ? 0d : (double) abstractDeps / (double) totalDeps;
    }

    /**
     * ISP: average number of methods declared on interfaces in this module.
     */
    public double averageMethodsPerInterface() {
        var interfaces = classes.stream()
                .filter(ClassDescriptor::isAbstract)
                .filter(c -> c.countMethodsInClass() > 0)
                .toList();
        if (interfaces.isEmpty()) return 0d;
        var totalMethods = interfaces.stream().mapToInt(ClassDescriptor::countMethodsInClass).sum();
        return (double) totalMethods / interfaces.size();
    }

    /**
     * ISP: max methods on any single interface in this module.
     */
    public int maxMethodsOnInterface() {
        return classes.stream()
                .filter(ClassDescriptor::isAbstract)
                .mapToInt(ClassDescriptor::countMethodsInClass)
                .max().orElse(0);
    }
}
