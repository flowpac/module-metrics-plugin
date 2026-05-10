package ch.javacamp.metrics;

import ch.javacamp.metrics.analyzer.ClassAnalyzer;
import ch.javacamp.metrics.core.ModuleDescriptor;
import ch.javacamp.metrics.core.Modules;
import ch.javacamp.metrics.rendering.Renderer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.fusesource.jansi.Ansi;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.List;


@Mojo(name = "analyze", defaultPhase = LifecyclePhase.PRE_SITE)
public class MetricsMojo extends AbstractMojo {

    private final MavenProject project;
    private final MavenSession session;
    private final Modules modules;
    private final ClassAnalyzer classAnalyzer;
    private final Renderer renderer;

    @Inject
    public MetricsMojo(MavenProject project, MavenSession session, Modules collector, ClassAnalyzer classAnalyzer, Renderer renderer) {
        this.project = project;
        this.session = session;
        this.modules = collector;
        this.classAnalyzer = classAnalyzer;
        this.renderer = renderer;
    }

    @Override
    public void execute() {
        var classes = classAnalyzer.processClasses(Path.of(project.getBuild().getOutputDirectory()));
        modules.addModule(new ModuleDescriptor(project.getArtifactId(), classes));

        if (isLastProject()) {
            logReport();
            renderReport();
        }
    }

    private boolean isLastProject() {
        List<MavenProject> projects = session.getProjects();
        return projects.get(projects.size() - 1).equals(project);
    }

    private void logReport() {
        getLog().info(Ansi.ansi().bold().render("Computation").reset().toString());
        for (var result : modules.computeMetrics()) {
            getLog().info(Ansi.ansi().fgGreen().bold().render(result.name()).reset().toString());
            getLog().info(" A: " + result.abstractness());
            getLog().info("Ca: " + result.ca());
            getLog().info("Ce: " + result.ce());
            getLog().info(" I: " + result.instability());
            getLog().info(" D: " + result.distance());
        }
    }

    private void renderReport() {
        var parentProject = project;
        while (parentProject.getParent() != null) {
            parentProject = parentProject.getParent();
        }
        var targetDir = new File(parentProject.getBasedir(), "target");
        if (targetDir.exists() || targetDir.mkdir()) {
            renderer.render(targetDir.toPath().resolve("oo-metrics-report.html"), modules.computeMetrics());
        }
    }
}
