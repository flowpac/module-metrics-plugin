package ch.javacamp.metrics;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

class ModulesTest {

    @Test
    @DisplayName("Test computation with 3 classes")
    public void t1(){
        Modules modules = new Modules();

        var d1a = new ClassDescriptor("a-01", false, Set.of("a-02"));
        var d1b = new ClassDescriptor("a-02", false, Set.of("b-01"));
        var d1c = new ClassDescriptor("a-03", false, Set.of("c-01"));
        var modA = new ModuleDescriptor("mod-a", Set.of(d1a, d1b, d1c));
        modules.addModule(modA);

        var d2a = new ClassDescriptor("b-01", false, Set.of("a-01"));
        var d2b = new ClassDescriptor("b-02", false, Set.of("a-01"));
        var d2c = new ClassDescriptor("b-03", false, Set.of("a-02"));
        var modB = new ModuleDescriptor("mod-b", Set.of(d2a, d2b, d2c));
        modules.addModule(modB);

        var d3a = new ClassDescriptor("c-01", false, Set.of("a-01"));
        var d3b = new ClassDescriptor("c-02", false, Set.of("b-02"));
        var d3c = new ClassDescriptor("c-03", false, Set.of());
        var modC = new ModuleDescriptor("mod-c", Set.of(d3a, d3b, d3c));
        modules.addModule(modC);

        var metricsA = modules.computeMetrics(modA);
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
}