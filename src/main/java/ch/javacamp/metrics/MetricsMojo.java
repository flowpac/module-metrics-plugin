package ch.javacamp.metrics;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.ClassReader;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


@Mojo(name = "analyze", defaultPhase = LifecyclePhase.PRE_SITE)
public class MetricsMojo extends AbstractMojo {

    private final MavenProject project;
    private final ModuleCollector moduleCollector;
    private final ModuleProcessingState processingState;

    @Inject
    public MetricsMojo(MavenProject project, ModuleCollector collector, ModuleProcessingState procssingState) {
        this.project = project;
        this.moduleCollector = collector;
        this.processingState = procssingState;
    }

    private static ClassInfo analyzeClass(File f) {
        try {
            ClassReader reader = new ClassReader(new FileInputStream(f));
            return DependencyAnalyzer.getDependentClasses(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isClassFile(File f) {
        return f.getName().endsWith(".class");
    }

    @Override
    public void execute() throws MojoExecutionException {
        project.getModules().forEach(processingState::addDetectedModule);
        processingState.addProcessedModule(project.getArtifactId());

        processClasses(project.getBuild().getOutputDirectory());

        if (processingState.allModulesProcessed()) {
            compute();
        }

    }

    private void compute() {
        getLog().info("Computation");

        for (ModuleInfo currentModule : moduleCollector.getModules()) {
            var moduleClasses = currentModule.allClasses();
            var otherModules = moduleCollector.otherModules(currentModule);

            var outsideClassesWithDependenciesToMe = otherModules
                    .stream()
                    .flatMap(x -> x.classes().stream())
                    .filter(c -> c.hasDependency(moduleClasses))
                    .count();

            int classesWithForeignDependencies = 0;
            var allOtherClasses = otherModules.stream()
                    .flatMap(x -> x.classes().stream())
                    .map(ClassInfo::className)
                    .collect(Collectors.toSet());

            for (ClassInfo c : currentModule.classes()) {
                for (var dep : c.dependencies()) {
                    if (allOtherClasses.contains(dep)) {
                        classesWithForeignDependencies++;
                        break;
                    }
                }
            }

            getLog().info(currentModule.name());
            getLog().info(" A: " + currentModule.abstractness());
            getLog().info("Ca: " + outsideClassesWithDependenciesToMe);
            getLog().info("Ce: " + classesWithForeignDependencies);
            var ca = outsideClassesWithDependenciesToMe;
            var ce = classesWithForeignDependencies;
            var i = (double) ce / (double) (ce + ca);
            getLog().info(" I: " + i);
            var a = currentModule.abstractness();
            getLog().info(" D: " + Math.abs((a + i - 1) / 2.0));
        }
    }

    private void processClasses(String classesDirectory) {
        Set<ClassInfo> s = new HashSet<>();
        var base = Path.of(classesDirectory);
        if (base.toFile().exists()) {
            try (var files = Files.walk(base, FileVisitOption.FOLLOW_LINKS)) {
                files.map(Path::toFile)
                        .filter(MetricsMojo::isClassFile)
                        .map(MetricsMojo::analyzeClass)
                        .forEach(s::add);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (!s.isEmpty()) {
            moduleCollector.addModule(new ModuleInfo(project.getArtifactId(), s));
        }
    }

}
