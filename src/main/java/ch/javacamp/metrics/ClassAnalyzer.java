package ch.javacamp.metrics;

import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassAnalyzer {

    public Set<ClassDescriptor> processClasses(Path directory) {
        if (directory.toFile().exists()) {
            try (var files = Files.walk(directory)) {
                return files.map(Path::toFile)
                        .filter(this::isClassFile)
                        .map(this::analyzeClass)
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new HashSet<>();
    }

    private boolean isClassFile(File f) {
        return f.getName().endsWith(".class");
    }

    private ClassDescriptor analyzeClass(File f) {
        try {
            ClassReader reader = new ClassReader(new FileInputStream(f));
            return new DependencyAnalyzer().getDependentClasses(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
