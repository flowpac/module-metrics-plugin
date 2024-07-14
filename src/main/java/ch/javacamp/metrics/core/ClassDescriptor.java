package ch.javacamp.metrics.core;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public record ClassDescriptor(String className, boolean isAbstract, Visibility visibility, Set<String> dependencies, Set<MethodDescriptor> methods) {

    public boolean hasDependency(Set<String> check) {
        return dependencies.stream().anyMatch(check::contains);
    }

    public void addMethods(Set<MethodDescriptor> methods) {
        this.methods.addAll(methods);
    }

    public int countPublicMethodsInClass() {
        return (int) this.methods.stream()
                .filter(not(MethodDescriptor::isConstructor))
                .filter(MethodDescriptor::isPublic)
                .count();
    }

    public int countMethodsInClass() {
        return (int) this.methods.stream()
                .filter(not(MethodDescriptor::isConstructor))
                .count();
    }

    public int countMethodsWithLocalCalls() {
        return (int) this.methods.stream()
                .filter(not(MethodDescriptor::isConstructor))
                .filter(MethodDescriptor::callsOtherLocalMethods)
                .count();
    }

    public int countMethodsWithOnlyOneInvolvedField(){
        return (int) this.methods.stream()
                .filter(MethodDescriptor::readOrModifyOneSingleField)
                .count();
    }

    public Set<MethodDescriptor> getFilteredMethods(){
        return methods.stream()
                .filter(Predicate.not(MethodDescriptor::isSpecialMethod))
                .collect(Collectors.toSet());
    }

    public int lcom4(){
        var calculator = new LCOM4Calculator();
        return calculator.calculate(this.getFilteredMethods());
    }
}
