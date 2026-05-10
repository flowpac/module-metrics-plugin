package ch.javacamp.metrics.rendering;

import ch.javacamp.metrics.core.CallFlowData;
import ch.javacamp.metrics.core.MetricsResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class Renderer {

    public static final String HTML_TEMPLATE = "template/report-js.html";
    public static final String DEPENDENCY_GRAPH_TEMPLATE = "template/dependency-graph.html";
    public static final String CALL_FLOW_TEMPLATE = "template/call-flow.html";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();

    public void render(Path outputFile, List<MetricsResult> results) {
        renderTemplate(HTML_TEMPLATE, outputFile, gson.toJson(results));
    }

    public void renderDependencyGraph(Path outputFile, List<MetricsResult> results) {
        renderTemplate(DEPENDENCY_GRAPH_TEMPLATE, outputFile, gson.toJson(results));
    }

    public void renderCallFlow(Path outputFile, CallFlowData callFlowData) {
        renderTemplate(CALL_FLOW_TEMPLATE, outputFile, gson.toJson(callFlowData));
    }

    private void renderTemplate(String templatePath, Path outputFile, String jsonData) {
        try (var is = getClass().getClassLoader().getResourceAsStream(templatePath)) {
            Objects.requireNonNull(is);
            var template = IOUtils.toString(is, "UTF-8");
            template = template.replace("##data##", jsonData);
            Files.writeString(outputFile, template);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
