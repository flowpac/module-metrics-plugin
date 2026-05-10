package ch.javacamp.metrics.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CallFlowBuilder {

    public CallFlowData build(List<ModuleDescriptor> modules) {
        Map<String, String> classToModule = buildClassToModuleIndex(modules);
        Map<String, MethodDescriptor> methodIndex = buildMethodIndex(modules);
        Map<String, List<String>> implementationIndex = buildImplementationIndex(modules);

        List<CallFlowData.ModuleInfo> moduleInfos = modules.stream()
                .sorted(Comparator.comparing(ModuleDescriptor::name))
                .map(mod -> CallFlowData.ModuleInfo.builder()
                        .name(mod.name())
                        .classes(mod.classes().stream()
                                .sorted(Comparator.comparing(ClassDescriptor::className))
                                .map(cls -> CallFlowData.ClassInfo.builder()
                                        .className(cls.className())
                                        .simpleName(simpleName(cls.className()))
                                        .isAbstract(cls.isAbstract())
                                        .methods(cls.methods().stream()
                                                .filter(m -> !m.isConstructor())
                                                .sorted(Comparator.comparing(MethodDescriptor::shortName))
                                                .map(m -> CallFlowData.MethodInfo.builder()
                                                        .fqn(m.fqn())
                                                        .name(m.name())
                                                        .shortName(m.shortName())
                                                        .visibility(m.visibility().name())
                                                        .build())
                                                .collect(Collectors.toList()))
                                        .build())
                                .filter(ci -> !ci.methods().isEmpty())
                                .collect(Collectors.toList()))
                        .build())
                .filter(mi -> !mi.classes().isEmpty())
                .collect(Collectors.toList());

        Map<String, List<CallFlowData.CallTarget>> callGraph = buildCallGraph(modules, classToModule, methodIndex, implementationIndex);

        return CallFlowData.builder()
                .modules(moduleInfos)
                .callGraph(callGraph)
                .build();
    }

    private Map<String, List<CallFlowData.CallTarget>> buildCallGraph(
            List<ModuleDescriptor> modules,
            Map<String, String> classToModule,
            Map<String, MethodDescriptor> methodIndex,
            Map<String, List<String>> implementationIndex) {

        Map<String, List<CallFlowData.CallTarget>> callGraph = new HashMap<>();

        for (MethodDescriptor method : methodIndex.values()) {
            List<CallFlowData.CallTarget> targets = new ArrayList<>();
            for (MethodInvocation inv : method.invokedMethods()) {
                String targetFqn = inv.fqn();
                String targetClass = inv.clazz();
                String targetModule = classToModule.getOrDefault(targetClass, null);
                if (targetModule == null) continue;

                boolean isInterfaceCall = isAbstractClass(targetClass, modules);
                List<CallFlowData.ImplementationOption> implOptions = new ArrayList<>();

                if (isInterfaceCall) {
                    List<String> implClasses = implementationIndex.getOrDefault(targetClass, List.of());
                    if (implClasses.size() == 1) {
                        String implClass = implClasses.get(0);
                        String resolvedFqn = implClass + "#" + inv.shortName();
                        String implModule = classToModule.getOrDefault(implClass, targetModule);
                        if (methodIndex.containsKey(resolvedFqn)) {
                            targetFqn = resolvedFqn;
                            targetClass = implClass;
                            targetModule = implModule;
                            isInterfaceCall = false;
                        }
                    } else if (implClasses.size() > 1) {
                        for (String implClass : implClasses) {
                            String implFqn = implClass + "#" + inv.shortName();
                            if (methodIndex.containsKey(implFqn)) {
                                implOptions.add(CallFlowData.ImplementationOption.builder()
                                        .fqn(implFqn)
                                        .className(implClass)
                                        .simpleName(simpleName(implClass))
                                        .moduleName(classToModule.getOrDefault(implClass, "?"))
                                        .build());
                            }
                        }
                    }
                }

                targets.add(CallFlowData.CallTarget.builder()
                        .targetFqn(targetFqn)
                        .targetClass(targetClass)
                        .targetSimpleName(simpleName(targetClass))
                        .targetModule(targetModule)
                        .targetMethod(inv.shortName())
                        .interfaceCall(isInterfaceCall)
                        .conditional(inv.conditional())
                        .implementations(implOptions)
                        .build());
            }
            if (!targets.isEmpty()) {
                callGraph.put(method.fqn(), targets);
            }
        }
        return callGraph;
    }

    private Map<String, String> buildClassToModuleIndex(List<ModuleDescriptor> modules) {
        Map<String, String> index = new HashMap<>();
        for (ModuleDescriptor mod : modules) {
            for (ClassDescriptor cls : mod.classes()) {
                index.put(cls.className(), mod.name());
            }
        }
        return index;
    }

    private Map<String, MethodDescriptor> buildMethodIndex(List<ModuleDescriptor> modules) {
        Map<String, MethodDescriptor> index = new HashMap<>();
        modules.stream()
                .flatMap(m -> m.classes().stream())
                .flatMap(c -> c.methods().stream())
                .forEach(m -> index.put(m.fqn(), m));
        return index;
    }

    private Map<String, List<String>> buildImplementationIndex(List<ModuleDescriptor> modules) {
        Set<String> abstractClasses = modules.stream()
                .flatMap(m -> m.classes().stream())
                .filter(ClassDescriptor::isAbstract)
                .map(ClassDescriptor::className)
                .collect(Collectors.toSet());

        Map<String, List<String>> index = new HashMap<>();
        modules.stream()
                .flatMap(m -> m.classes().stream())
                .filter(c -> !c.isAbstract())
                .forEach(c -> {
                    for (String superType : c.superTypes()) {
                        if (abstractClasses.contains(superType)) {
                            index.computeIfAbsent(superType, k -> new ArrayList<>()).add(c.className());
                        }
                    }
                });
        return index;
    }

    private boolean isAbstractClass(String className, List<ModuleDescriptor> modules) {
        return modules.stream()
                .flatMap(m -> m.classes().stream())
                .filter(c -> c.className().equals(className))
                .findFirst()
                .map(ClassDescriptor::isAbstract)
                .orElse(false);
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot >= 0 ? fqn.substring(dot + 1) : fqn;
    }
}
