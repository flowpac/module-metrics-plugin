package ch.javacamp.metrics.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Relation {

    @SafeVarargs
    public static Set<String> combine(Set<String> first, Set<String>... others){
        Set<String> result = new HashSet<>(first);
        Arrays.stream(others).forEach(result::addAll);
        return result;
    }

    public static boolean containsAtLeastOneElement(Set<String> base, Set<String> search){
        for(String element:search){
            if(base.contains(element)){
                return true;
            }
        }
        return false;
    }
}
