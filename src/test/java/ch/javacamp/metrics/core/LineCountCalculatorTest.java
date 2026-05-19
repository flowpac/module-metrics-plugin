package ch.javacamp.metrics.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class LineCountCalculatorTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MethodDescriptor methodWithLines(String owner, int lineCount) {
        var m = new MethodDescriptor(owner, owner + "#doWork", "doWork()", Visibility.PUBLIC, "doWork", "void", "");
        for (int i = 0; i < lineCount; i++) {
            m.incLineCounter();
        }
        return m;
    }

    private ClassDescriptor classWithMethods(String className, MethodDescriptor... methods) {
        var methodSet = new HashSet<>(Arrays.asList(methods));
        return new ClassDescriptor(className, false, Visibility.PUBLIC, Set.of(), methodSet, Set.of());
    }

    // -------------------------------------------------------------------------
    // computePackages
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Single package: all lines aggregated under one entry")
    void computePackages_singlePackage() {
        var cls1 = classWithMethods("com.example.Foo",
                methodWithLines("com.example.Foo", 10),
                methodWithLines("com.example.Foo", 5));
        var cls2 = classWithMethods("com.example.Bar",
                methodWithLines("com.example.Bar", 20));

        var module = new ModuleDescriptor("my-module", Set.of(cls1, cls2));
        var result = new LineCountCalculator().computePackages(module);

        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).packageName()).isEqualTo("com.example");
        Assertions.assertThat(result.get(0).totalLines()).isEqualTo(35);
    }

    @Test
    @DisplayName("Two different packages: separate entries with correct line counts")
    void computePackages_twoPackages() {
        var cls1 = classWithMethods("com.example.service.OrderService",
                methodWithLines("com.example.service.OrderService", 30));
        var cls2 = classWithMethods("com.example.domain.Order",
                methodWithLines("com.example.domain.Order", 12));

        var module = new ModuleDescriptor("my-module", Set.of(cls1, cls2));
        var result = new LineCountCalculator().computePackages(module);

        Assertions.assertThat(result).hasSize(2);
        var packages = result.stream().map(PackageLineCount::packageName).toList();
        Assertions.assertThat(packages).containsExactlyInAnyOrder("com.example.service", "com.example.domain");

        var serviceLines = result.stream()
                .filter(p -> p.packageName().equals("com.example.service"))
                .findFirst().orElseThrow().totalLines();
        Assertions.assertThat(serviceLines).isEqualTo(30);

        var domainLines = result.stream()
                .filter(p -> p.packageName().equals("com.example.domain"))
                .findFirst().orElseThrow().totalLines();
        Assertions.assertThat(domainLines).isEqualTo(12);
    }

    @Test
    @DisplayName("Class in default package gets label '(default)'")
    void computePackages_defaultPackage() {
        var cls = classWithMethods("MyClass",
                methodWithLines("MyClass", 7));

        var module = new ModuleDescriptor("my-module", Set.of(cls));
        var result = new LineCountCalculator().computePackages(module);

        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).packageName()).isEqualTo("(default)");
        Assertions.assertThat(result.get(0).totalLines()).isEqualTo(7);
    }

    @Test
    @DisplayName("Multiple classes in same package: lines are summed")
    void computePackages_multipleClassesSamePackage_linesSummed() {
        var cls1 = classWithMethods("org.acme.Alpha", methodWithLines("org.acme.Alpha", 8));
        var cls2 = classWithMethods("org.acme.Beta",  methodWithLines("org.acme.Beta",  12));
        var cls3 = classWithMethods("org.acme.Gamma", methodWithLines("org.acme.Gamma", 5));

        var module = new ModuleDescriptor("my-module", Set.of(cls1, cls2, cls3));
        var result = new LineCountCalculator().computePackages(module);

        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).totalLines()).isEqualTo(25);
    }

    @Test
    @DisplayName("Result is sorted alphabetically by package name")
    void computePackages_sortedAlphabetically() {
        var cls1 = classWithMethods("z.pkg.A", methodWithLines("z.pkg.A", 1));
        var cls2 = classWithMethods("a.pkg.B", methodWithLines("a.pkg.B", 1));
        var cls3 = classWithMethods("m.pkg.C", methodWithLines("m.pkg.C", 1));

        var module = new ModuleDescriptor("my-module", Set.of(cls1, cls2, cls3));
        var result = new LineCountCalculator().computePackages(module);

        Assertions.assertThat(result)
                .extracting(PackageLineCount::packageName)
                .containsExactly("a.pkg", "m.pkg", "z.pkg");
    }

    @Test
    @DisplayName("Class with zero-line methods still contributes an entry with 0 lines")
    void computePackages_classWithNoLines_entryWithZeroLines() {
        var cls = classWithMethods("com.empty.Noop",
                methodWithLines("com.empty.Noop", 0));

        var module = new ModuleDescriptor("my-module", Set.of(cls));
        var result = new LineCountCalculator().computePackages(module);

        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).totalLines()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // computeModule (regression: wahrscheinlich bereits getestet, aber zur Sicherheit)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("computeModule: mean and median calculated over non-special methods")
    void computeModule_statistics() {
        // 3 normal methods with 10, 20, 30 lines
        var m1 = methodWithLines("com.example.Calc", 10);
        var m2 = methodWithLines("com.example.Calc", 20);
        var m3 = methodWithLines("com.example.Calc", 30);
        var cls = classWithMethods("com.example.Calc", m1, m2, m3);

        var module = new ModuleDescriptor("my-module", Set.of(cls));
        var result = new LineCountCalculator().computeModule(module);

        Assertions.assertThat(result.mean()).isEqualTo(20.0);
        Assertions.assertThat(result.median()).isEqualTo(20.0);
        Assertions.assertThat(result.percentile(25)).isLessThan(20.0);
        Assertions.assertThat(result.percentile(75)).isGreaterThan(20.0);
    }
}

