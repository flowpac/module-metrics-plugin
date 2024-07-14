package ch.javacamp.metrics.analyzer;

import ch.javacamp.metrics.analyzer.test.TestClassA;
import ch.javacamp.metrics.analyzer.test.TestClassB;
import ch.javacamp.metrics.analyzer.test.TestClassC;
import ch.javacamp.metrics.core.ClassDescriptor;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import java.io.IOException;

class ClassAnalyzerTest {

    @Test
    @DisplayName("Check number of Methods.")
    public void t1() throws Exception {
        var classDescriptor = analyze(DependencyAnalyzerTest.I1Impl.class);
        Assertions.assertThat(classDescriptor.countMethodsInClass()).isEqualTo(2);
    }

    @Test
    @DisplayName("Check number of public Methods.")
    public void t2() throws Exception {
        var classDescriptor = analyze(DependencyAnalyzerTest.I1Impl.class);
        Assertions.assertThat(classDescriptor.countPublicMethodsInClass()).isEqualTo(1);
    }

    @Test
    @DisplayName("Check number of Methods accessing only one field.")
    public void t3() throws Exception {
        var classDescriptor = analyze(TestClassA.class);
        Assertions.assertThat(classDescriptor.countMethodsWithOnlyOneInvolvedField()).isEqualTo(4);
    }

    @Test
    @DisplayName("LCOM4 - combine to one group")
    public void t4() throws Exception {
        var classDescriptor = analyze(TestClassA.class);
        Assertions.assertThat(classDescriptor.lcom4()).isEqualTo(1);
    }

    @Test
    @DisplayName("LCOM4 - two different groups")
    public void t5() throws Exception {
        var classDescriptor = analyze(TestClassB.class);
        Assertions.assertThat(classDescriptor.lcom4()).isEqualTo(2);
    }

    @Test
    @DisplayName("Count methods with local calls - t1")
    public void t6() throws Exception {
        var classDescriptor = analyze(TestClassB.class);
        Assertions.assertThat(classDescriptor.countMethodsWithLocalCalls()).isEqualTo(1);
    }

    @Test
    @DisplayName("Count methods with local calls - t2")
    public void t7() throws Exception {
        var classDescriptor = analyze(TestClassC.class);
        Assertions.assertThat(classDescriptor.countMethodsWithLocalCalls()).isEqualTo(2);
    }

    private static ClassDescriptor analyze(Class<?> clazz) throws IOException {
        ClassReader classReader = new ClassReader(clazz.getName());
        return new ClassAnalyzer().analyze(classReader);
    }

}