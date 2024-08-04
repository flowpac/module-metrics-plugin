package ch.javacamp.metrics.core;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CallFlowCalculator {

    Map<String, MethodDescriptor> methods = new HashMap<>();

    public void calculate(List<ModuleDescriptor> modules){

        modules.stream().flatMap(x -> x.classes().stream())
                .flatMap(x -> x.methods().stream())
                .forEach(x -> methods.put(x.fqn(), x));

        var metrics = methods.values().stream()
                .map(method ->
                    FlowMetric.builder()
                            .method(method)
                            .maxCallDepth(maxDepth(method))
                            .numberOfClassesInFlow(numClasses(method))
                            .pathLength(pathLength(method))
                            .build()
                ).collect(Collectors.toSet());

        System.out.println();
        System.out.println("--- Depth ---");
        metrics.stream()
                //.filter(m -> m.method.isPublic())
                .sorted(Comparator.comparingInt(FlowMetric::maxCallDepth).reversed())
                .limit(10)
                .map(m -> String.format("%s: %d", m.fqn(), m.maxCallDepth))
                .forEach(System.out::println);

        System.out.println();
        System.out.println("--- Classes ---");
        metrics.stream()
                //.filter(m -> m.method.isPublic())
                .sorted(Comparator.comparingInt(FlowMetric::numberOfClassesInFlow).reversed())
                .limit(10)
                .map(m -> String.format("%s: %d", m.fqn(), m.numberOfClassesInFlow))
                .forEach(System.out::println);

        System.out.println();
        System.out.println("--- Path Length ---");
        metrics.stream()
                //.filter(m -> m.method.isPublic())
                .sorted(Comparator.comparingInt(FlowMetric::pathLength).reversed())
                .limit(10)
                .map(m -> String.format("%s: %d", m.fqn(), m.pathLength))
                .forEach(System.out::println);
    }

    private int maxDepth(MethodDescriptor m){
        return maxDepth(m, 0, new HashSet<>());
    }

    private int maxDepth(MethodDescriptor m, int d, Set<String> cycleCheck){
        if(m == null || !cycleCheck.add(m.fqn())){
            return d;
        }
        return m.invokedMethods().stream()
                .map(MethodInvocation::fqn)
                .map(fqn -> maxDepth(methods.get(fqn), d + 1, cycleCheck))
                .max(Comparator.naturalOrder()).orElse(d);
    }

    private int pathLength(MethodDescriptor m){
        return pathLength(m, 0, new HashSet<>());
    }

    private int pathLength(MethodDescriptor m, int d, Set<String> cycleCheck){
        if(m == null || !cycleCheck.add(m.fqn())){
            return d;
        }
        return m.invokedMethods().stream()
                .map(MethodInvocation::fqn)
                .map(fqn -> pathLength(methods.get(fqn), d + 1, cycleCheck))
                .reduce(Integer::sum).orElse(0);
    }


    private int numClasses(MethodDescriptor m){
        return numClasses(m, 0, new HashSet<>(), new HashSet<>()).size();
    }

    private Set<String> numClasses(MethodDescriptor m, int d, Set<String> cycleCheck, Set<String> classes){

        if(m == null || !cycleCheck.add(m.fqn())){
            return classes;
        }
        classes.add(m.owner());

        return m.invokedMethods().stream()
                .map(MethodInvocation::fqn)
                .flatMap(fqn -> numClasses(methods.get(fqn), d + 1, cycleCheck, classes).stream())
                .collect(Collectors.toSet());
    }

    @Builder
    @Getter
    @Accessors(fluent = true)
    public static class FlowMetric {
        MethodDescriptor method;
        int numberOfClassesInFlow;
        int maxCallDepth;
        int pathLength;

        public String fqn(){
            return method.fqn();
        }
    }
}
