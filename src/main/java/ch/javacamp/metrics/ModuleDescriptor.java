package ch.javacamp.metrics;

import java.util.Set;
import java.util.stream.Collectors;

public record ModuleDescriptor(String name, Set<ClassDescriptor> classes) {


    public long abstractClasses(){
        return classes.stream().filter(ClassDescriptor::isAbstract).count();
    }

    public long totalClasses(){
        return classes.size();
    }

    public Set<String> allClassNames(){
        return classes.stream().map(ClassDescriptor::className).collect(Collectors.toSet());
    }

    public double abstractness(){
        return (double)abstractClasses() / (double)totalClasses();
    }

    public double averageMethodsPerClass(){
        var methods = classes.stream().map(ClassDescriptor::numOfMethods).reduce(Integer::sum).orElse(0);
        return (double) methods / (double) totalClasses();
    }

    public double averagePublicMethodsPerClass(){
        var methods = classes.stream().map(ClassDescriptor::numOfPublicMethods).reduce(Integer::sum).orElse(0);
        return (double) methods / (double) totalClasses();
    }



}
