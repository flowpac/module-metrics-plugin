package ch.javacamp.metrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class Renderer {

    public static final String HTML_TEMPLATE = "template/report-js.html";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();

    public void render(Path outputFile, List<MetricsResult> results){
        try(var is = getClass().getClassLoader().getResourceAsStream(HTML_TEMPLATE)) {
            Objects.requireNonNull(is);
            var template = IOUtils.toString(is);
            template = template.replace("##data##", gson.toJson(results));
            Files.writeString(outputFile, template);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
