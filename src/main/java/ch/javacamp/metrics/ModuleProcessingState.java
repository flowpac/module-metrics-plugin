package ch.javacamp.metrics;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class ModuleProcessingState {

    private final Set<String> detectedModules = new HashSet<>();
    private final Set<String> processedModules = new HashSet<>();

    public boolean allModulesProcessed(){
        return detectedModules.size() == processedModules.size();
    }

    public void addDetectedModule(String detectedModule){
        this.detectedModules.add(detectedModule);
    }

    public void addProcessedModule(String processedModule){
        this.processedModules.add(processedModule);
    }

}
