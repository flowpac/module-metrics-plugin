package ch.javacamp.metrics;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class ModuleProcessingState {

    private final Set<String> detectedModules = new HashSet<>();
    private final Set<String> processedModules = new HashSet<>();

    public boolean allModulesProcessed() {
        if (detectedModules.isEmpty()) {
            return false;
        }
        return processedModules.containsAll(detectedModules);
    }

    public void setDetectedModules(Set<String> modules) {
        if (detectedModules.isEmpty()) {
            detectedModules.addAll(modules);
        }
    }

    public void addProcessedModule(String processedModule) {

        this.processedModules.add(processedModule);
    }

}
