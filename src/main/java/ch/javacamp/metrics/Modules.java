package ch.javacamp.metrics;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class Modules {

    private final List<ModuleDescriptor> modules = new ArrayList<>();

    public void addModule(ModuleDescriptor moduleDescriptor){
        if(moduleDescriptor.totalClasses() > 0) {
            this.modules.add(moduleDescriptor);
        }
    }

    public List<ModuleDescriptor> getModules() {
        return modules;
    }

    public Set<ModuleDescriptor> otherModules(ModuleDescriptor toExclude){
        return modules.stream().filter(x -> x != toExclude).collect(Collectors.toSet());
    }

    public Set<ClassDescriptor> externalClassesUsingClassesInThisModule(ModuleDescriptor module){
        var classNamesCurrentModule = module.allClassNames();
        var otherModules = otherModules(module);

        return otherModules
                .stream()
                .flatMap(x -> x.classes().stream())
                .filter(c -> c.hasDependency(classNamesCurrentModule))
                .collect(Collectors.toSet());
    }

    public Set<ClassDescriptor> externalClassesUsedByThisModule(ModuleDescriptor module){
        Set<ClassDescriptor> result = new HashSet<>();
        var allClassNamesOutsideCurrentModule = otherModules(module).stream()
                .flatMap(x -> x.classes().stream())
                .map(ClassDescriptor::className)
                .collect(Collectors.toSet());

        for (var c : module.classes()) {
            for (var dependency : c.dependencies()) {
                if (allClassNamesOutsideCurrentModule.contains(dependency)) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    public MetricsResult computeMetrics(ModuleDescriptor module){
        var outsideClassesWithDependenciesToMe = externalClassesUsingClassesInThisModule(module);
        var classesWithForeignDependencies = externalClassesUsedByThisModule(module);

        var ca = outsideClassesWithDependenciesToMe.size();
        var ce = classesWithForeignDependencies.size();
        var i = (double) ce / (double) (ce + ca);
        var a = module.abstractness();
        var d = Math.abs((a + i - 1) / 2.0);
        return new MetricsResult(module.totalClasses(), ca, ce, a, i, d);
    }

}
