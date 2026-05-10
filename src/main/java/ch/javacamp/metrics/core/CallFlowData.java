package ch.javacamp.metrics.core;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@Accessors(fluent = true)
public class CallFlowData {

    private final List<ModuleInfo> modules;
    private final Map<String, List<CallTarget>> callGraph;

    @Builder
    @Getter
    @Accessors(fluent = true)
    public static class ModuleInfo {
        private final String name;
        private final List<ClassInfo> classes;
    }

    @Builder
    @Getter
    @Accessors(fluent = true)
    public static class ClassInfo {
        private final String className;
        private final String simpleName;
        private final boolean isAbstract;
        private final List<MethodInfo> methods;
    }

    @Builder
    @Getter
    @Accessors(fluent = true)
    public static class MethodInfo {
        private final String fqn;
        private final String name;
        private final String shortName;
        private final String visibility;
    }

    @Builder
    @Getter
    @Accessors(fluent = true)
    public static class CallTarget {
        private final String targetFqn;
        private final String targetClass;
        private final String targetSimpleName;
        private final String targetModule;
        private final String targetMethod;
        private final boolean interfaceCall;
        private final boolean conditional;
        private final List<ImplementationOption> implementations;
    }

    @Builder
    @Getter
    @Accessors(fluent = true)
    public static class ImplementationOption {
        private final String fqn;
        private final String className;
        private final String simpleName;
        private final String moduleName;
    }
}
