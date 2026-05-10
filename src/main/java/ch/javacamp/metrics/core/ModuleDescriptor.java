package ch.javacamp.metrics.core;

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



}
