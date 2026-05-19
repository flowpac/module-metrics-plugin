package ch.javacamp.metrics.core;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class ModulesTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ClassDescriptor createClassDescriptor(String name, Set<String> dependencies) {
        return new ClassDescriptor(name, false, Visibility.PUBLIC, dependencies, new HashSet<>(), Set.of());
    }

    private ClassDescriptor createAbstractClassDescriptor(String name) {
        return new ClassDescriptor(name, true, Visibility.PUBLIC, Set.of(), new HashSet<>(), Set.of());
    }

    private MethodDescriptor methodWithLines(String owner, int lineCount) {
        var m = new MethodDescriptor(owner, owner + "#work", "work():void",
                Visibility.PUBLIC, "work", "void", "");
        for (int i = 0; i < lineCount; i++) m.incLineCounter();
        return m;
    }

    private ClassDescriptor createClassWithMethods(String className, Set<String> deps, MethodDescriptor... methods) {
        var methodSet = new HashSet<MethodDescriptor>();
        for (var m : methods) methodSet.add(m);
        return new ClassDescriptor(className, false, Visibility.PUBLIC, deps, methodSet, Set.of());
    }

    // -------------------------------------------------------------------------
    // Existing test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Test computation with 3 classes")
    public void t1(){
        Modules modules = new Modules();

        var d1a = createClassDescriptor("a-01", Set.of("a-02"));
        var d1b = createClassDescriptor("a-02", Set.of("b-01"));
        var d1c = createClassDescriptor("a-03", Set.of("c-01"));
        var modA = new ModuleDescriptor("mod-a", Set.of(d1a, d1b, d1c));
        modules.addModule(modA);

        var d2a = createClassDescriptor("b-01", Set.of("a-01"));
        var d2b = createClassDescriptor("b-02", Set.of("a-01"));
        var d2c = createClassDescriptor("b-03", Set.of("a-02"));
        var modB = new ModuleDescriptor("mod-b", Set.of(d2a, d2b, d2c));
        modules.addModule(modB);

        var d3a = createClassDescriptor("c-01", Set.of("a-01"));
        var d3b = createClassDescriptor("c-02", Set.of("b-02"));
        var d3c = createClassDescriptor("c-03", Set.of());
        var modC = new ModuleDescriptor("mod-c", Set.of(d3a, d3b, d3c));
        modules.addModule(modC);

        var metricsA = modules.computeMetrics(modA);
        Assertions.assertThat(metricsA.name()).isEqualTo("mod-a");
        Assertions.assertThat(metricsA.numberOfClasses()).isEqualTo(3);
        Assertions.assertThat(metricsA.ca()).isEqualTo(4);
        Assertions.assertThat(metricsA.ce()).isEqualTo(2);
        Assertions.assertThat(metricsA.abstractness()).isEqualTo(0);
        Assertions.assertThat(metricsA.instability()).isCloseTo(0.33, Offset.offset(0.01));

        var metricsB = modules.computeMetrics(modB);
        Assertions.assertThat(metricsB.numberOfClasses()).isEqualTo(3);
        Assertions.assertThat(metricsB.ca()).isEqualTo(2);
        Assertions.assertThat(metricsB.ce()).isEqualTo(3);
        Assertions.assertThat(metricsB.abstractness()).isEqualTo(0);
        Assertions.assertThat(metricsB.instability()).isCloseTo(0.60, Offset.offset(0.01));

        var metricsC = modules.computeMetrics(modC);
        Assertions.assertThat(metricsC.numberOfClasses()).isEqualTo(3);
        Assertions.assertThat(metricsC.ca()).isEqualTo(1);
        Assertions.assertThat(metricsC.ce()).isEqualTo(2);
        Assertions.assertThat(metricsC.abstractness()).isEqualTo(0);
        Assertions.assertThat(metricsC.instability()).isCloseTo(0.66, Offset.offset(0.01));
    }

    // -------------------------------------------------------------------------
    // packageLineStatistics
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("packageLineStatistics: classes grouped by package with summed lines")
    void packageLineStatistics_populatedInComputeMetrics() {
        var modules = new Modules();

        var m1 = methodWithLines("com.example.service.OrderService", 20);
        var m2 = methodWithLines("com.example.service.InvoiceService", 10);
        var m3 = methodWithLines("com.example.domain.Order", 5);

        var cls1 = createClassWithMethods("com.example.service.OrderService", Set.of(), m1);
        var cls2 = createClassWithMethods("com.example.service.InvoiceService", Set.of(), m2);
        var cls3 = createClassWithMethods("com.example.domain.Order", Set.of(), m3);
        var mod = new ModuleDescriptor("mod", Set.of(cls1, cls2, cls3));
        modules.addModule(mod);

        var result = modules.computeMetrics(mod);

        Assertions.assertThat(result.packageLineStatistics()).hasSize(2);
        var serviceLines = result.packageLineStatistics().stream()
                .filter(p -> p.packageName().equals("com.example.service"))
                .findFirst().orElseThrow().totalLines();
        Assertions.assertThat(serviceLines).isEqualTo(30);

        var domainLines = result.packageLineStatistics().stream()
                .filter(p -> p.packageName().equals("com.example.domain"))
                .findFirst().orElseThrow().totalLines();
        Assertions.assertThat(domainLines).isEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // Circular dependencies
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("circularDependencies: two modules depending on each other form a cycle")
    void circularDependencies_mutualDependencyDetected() {
        var modules = new Modules();

        var clsA = createClassDescriptor("pkg.A", Set.of("pkg.B"));
        var clsB = createClassDescriptor("pkg.B", Set.of("pkg.A"));
        var modA = new ModuleDescriptor("mod-a", Set.of(clsA));
        var modB = new ModuleDescriptor("mod-b", Set.of(clsB));
        modules.addModule(modA);
        modules.addModule(modB);

        var result = modules.computeMetrics(modA);

        Assertions.assertThat(result.circularDependencies()).hasSize(1);
        Assertions.assertThat(result.circularDependencies().get(0))
                .containsExactly("mod-a", "mod-b", "mod-a");
    }

    @Test
    @DisplayName("circularDependencies: no cycle when dependencies are one-directional")
    void circularDependencies_noCycleForOneWayDependency() {
        var modules = new Modules();

        var clsA = createClassDescriptor("pkg.A", Set.of("pkg.B"));
        var clsB = createClassDescriptor("pkg.B", Set.of());
        var modA = new ModuleDescriptor("mod-a", Set.of(clsA));
        var modB = new ModuleDescriptor("mod-b", Set.of(clsB));
        modules.addModule(modA);
        modules.addModule(modB);

        var result = modules.computeMetrics(modA);

        Assertions.assertThat(result.circularDependencies()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // SDP violations & DIP ratio (require the full list-based computeMetrics)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("sdpViolations: stable module depending on unstable module triggers violation")
    void sdpViolations_stableDependsOnUnstable() {
        var modules = new Modules();

        // mod-core: isolated — nobody depends on it, no external deps
        var clsCore = createClassDescriptor("core.C", Set.of());
        var modCore = new ModuleDescriptor("mod-core", Set.of(clsCore));

        // mod-unstable: depends on core.C (ce=1), only mod-stable depends on it (ca=1) → I≈0.5
        var clsU = createClassDescriptor("pkg.U", Set.of("core.C"));
        var modUnstable = new ModuleDescriptor("mod-unstable", Set.of(clsU));

        // mod-ext: three classes depending on mod-stable → add to mod-stable's ca
        var ext1 = createClassDescriptor("ext.E1", Set.of("pkg.S"));
        var ext2 = createClassDescriptor("ext.E2", Set.of("pkg.S"));
        var ext3 = createClassDescriptor("ext.E3", Set.of("pkg.S"));
        var modExt = new ModuleDescriptor("mod-ext", Set.of(ext1, ext2, ext3));

        // mod-stable: depended on by 3 ext classes (ca=3), depends on mod-unstable (ce=1) → I=0.25
        var clsS = createClassDescriptor("pkg.S", Set.of("pkg.U"));
        var modStable = new ModuleDescriptor("mod-stable", Set.of(clsS));

        modules.addModule(modCore);
        modules.addModule(modUnstable);
        modules.addModule(modExt);
        modules.addModule(modStable);

        var results = modules.computeMetrics();
        var stableResult = results.stream()
                .filter(r -> r.name().equals("mod-stable"))
                .findFirst().orElseThrow();

        Assertions.assertThat(stableResult.sdpViolations()).hasSize(1);
        Assertions.assertThat(stableResult.sdpViolations().get(0).dependencyModule())
                .isEqualTo("mod-unstable");
        Assertions.assertThat(stableResult.sdpViolations().get(0).dependencyInstability())
                .isGreaterThan(stableResult.sdpViolations().get(0).thisInstability());
    }

    @Test
    @DisplayName("sdpViolations: no violation when module depends on equally or more stable module")
    void sdpViolations_noneWhenDependencyIsMoreStable() {
        var modules = new Modules();

        // mod-stable-dep: depended on by many, depends on nothing → I close to 0
        var depClass1 = createClassDescriptor("ext.X1", Set.of("stable.Core"));
        var depClass2 = createClassDescriptor("ext.X2", Set.of("stable.Core"));
        var depClass3 = createClassDescriptor("ext.X3", Set.of("stable.Core"));
        var modDeps = new ModuleDescriptor("mod-deps", Set.of(depClass1, depClass2, depClass3));

        var stableCore = createClassDescriptor("stable.Core", Set.of());
        var modStableCore = new ModuleDescriptor("mod-stable-core", Set.of(stableCore));

        // mod-consumer: depends on the stable core → pointing at something more stable
        var consumer = createClassDescriptor("consumer.App", Set.of("stable.Core"));
        var modConsumer = new ModuleDescriptor("mod-consumer", Set.of(consumer));

        modules.addModule(modDeps);
        modules.addModule(modStableCore);
        modules.addModule(modConsumer);

        var results = modules.computeMetrics();
        var consumerResult = results.stream()
                .filter(r -> r.name().equals("mod-consumer"))
                .findFirst().orElseThrow();

        Assertions.assertThat(consumerResult.sdpViolations()).isEmpty();
    }

    @Test
    @DisplayName("dependencyInversionRatio: correctly computes ratio of abstract vs concrete deps")
    void dependencyInversionRatio_halfAbstractDeps() {
        var modules = new Modules();

        // mod-contracts: one abstract + one concrete class
        var abstractI = createAbstractClassDescriptor("contracts.MyInterface");
        var concreteC = createClassDescriptor("contracts.ConcreteHelper", Set.of());
        var modContracts = new ModuleDescriptor("mod-contracts", Set.of(abstractI, concreteC));

        // mod-app: depends equally on abstract and concrete type
        var appClass = createClassDescriptor("app.App", Set.of("contracts.MyInterface", "contracts.ConcreteHelper"));
        var modApp = new ModuleDescriptor("mod-app", Set.of(appClass));

        modules.addModule(modContracts);
        modules.addModule(modApp);

        var results = modules.computeMetrics();
        var appResult = results.stream()
                .filter(r -> r.name().equals("mod-app"))
                .findFirst().orElseThrow();

        Assertions.assertThat(appResult.dependencyInversionRatio())
                .isCloseTo(0.5, Offset.offset(0.01));
    }

    @Test
    @DisplayName("dependencyInversionRatio: 1.0 when all resolved deps are abstract")
    void dependencyInversionRatio_allAbstract() {
        var modules = new Modules();

        var abstractI1 = createAbstractClassDescriptor("api.I1");
        var abstractI2 = createAbstractClassDescriptor("api.I2");
        var modApi = new ModuleDescriptor("mod-api", Set.of(abstractI1, abstractI2));

        var implClass = createClassDescriptor("impl.Impl",
                Set.of("api.I1", "api.I2"));
        var modImpl = new ModuleDescriptor("mod-impl", Set.of(implClass));

        modules.addModule(modApi);
        modules.addModule(modImpl);

        var results = modules.computeMetrics();
        var implResult = results.stream()
                .filter(r -> r.name().equals("mod-impl"))
                .findFirst().orElseThrow();

        Assertions.assertThat(implResult.dependencyInversionRatio())
                .isCloseTo(1.0, Offset.offset(0.01));
    }
}

