package ch.javacamp.metrics;

import java.util.Set;
import java.util.stream.Collectors;

public record ModuleInfo(String name, Set<ClassInfo> classes) {


    public long abstractClasses(){
        return classes.stream().filter(ClassInfo::isAbstract).count();
    }

    public long totalClasses(){
        return classes.size();
    }

    public Set<String> allClasses(){
        return classes.stream().map(ClassInfo::className).collect(Collectors.toSet());
    }

    public double abstractness(){
        return (double)abstractClasses() / (double)totalClasses();
    }

}
