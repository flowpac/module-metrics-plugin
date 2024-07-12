package ch.javacamp.metrics;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class ModuleCollector {

    private final List<ModuleInfo> modules = new ArrayList<>();


    public void addModule(ModuleInfo moduleInfo){
        this.modules.add(moduleInfo);
    }

    public List<ModuleInfo> getModules() {
        return modules;
    }

    public Set<ModuleInfo> otherModules(ModuleInfo toExclude){
        return modules.stream().filter(x -> x != toExclude).collect(Collectors.toSet());
    }
}
