package ch.javacamp.metrics.core;


import java.util.Set;

public record ClassDescriptor(String className, boolean isAbstract, Visibility visibility, int numOfMethods,
                              int numOfPublicMethods, Set<String> dependencies) {

    public boolean hasDependency(Set<String> check) {
        return dependencies.stream().anyMatch(check::contains);
    }

}
