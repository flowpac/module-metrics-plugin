package ch.javacamp.metrics;

import java.util.Set;

public record ClassInfo(String className, boolean isAbstract, Set<String> dependencies) {

    public boolean hasDependency(Set<String> check){
        for(var d:dependencies){
            if(check.contains(d)){
                return true;
            }
        }
        return false;
    }

}
