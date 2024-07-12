package ch.javacamp.metrics;

import java.util.Set;

public record ClassDescriptor(String className, boolean isAbstract, Set<String> dependencies) {

    public boolean hasDependency(Set<String> check){
        for(var dependency:dependencies){
            if(check.contains(dependency)){
                return true;
            }
        }
        return false;
    }

}
