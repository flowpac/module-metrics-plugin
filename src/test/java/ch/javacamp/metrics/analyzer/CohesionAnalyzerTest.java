package ch.javacamp.metrics.analyzer;

import ch.javacamp.metrics.analyzer.test.TestClassA;
import ch.javacamp.metrics.core.MethodDescriptor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.Set;

class CohesionAnalyzerTest {

    @Test
    @DisplayName("Check full method name.")
    public void t1() throws Exception {
        var method = create("setL():void");
        Assertions.assertThat(method.getFullName()).isEqualTo("public void setL()");
    }

    @Test
    @DisplayName("Check return type.")
    public void t2() throws Exception {
        var method = create("setL():void");
        Assertions.assertThat(method.getReturnType()).isEqualTo("void");
    }

    @Test
    @DisplayName("Check empty parameter type.")
    public void t3() throws Exception {
        var method = create("setL():void");
        Assertions.assertThat(method.getParameters()).isEqualTo("");
    }

    @Test
    @DisplayName("Check parameter type.")
    public void t4() throws Exception {
        var method = create("bar(java.lang.String; int; int; java.lang.String):java.lang.Object");
        Assertions.assertThat(method.getParameters()).isEqualTo("java.lang.String; int; int; java.lang.String");
    }

    @Test
    @DisplayName("Check written fields.")
    public void t5() throws Exception {
        var method = create("bar(java.lang.String; int; int; java.lang.String):java.lang.Object");
        Assertions.assertThat(method.getWrittenFields()).containsExactlyInAnyOrder("i", "s");
    }

    @Test
    @DisplayName("Check read fields.")
    public void t6() throws Exception {
        var method = create("getS():java.lang.String");
        Assertions.assertThat(method.getReadFields()).containsExactlyInAnyOrder("s");
    }

    @Test
    @DisplayName("Check invoked local methods.")
    public void t7() throws Exception {
        var method = create("getDelegatedS():java.lang.String");
        Assertions.assertThat(method.getInvokedLocalMethods()).containsExactlyInAnyOrder("getS():java.lang.String");
    }

    @Test
    @DisplayName("Check line numbers")
    public void t8() throws Exception {
        var method = create("bar(java.lang.String; int; int; java.lang.String):java.lang.Object");
        Assertions.assertThat(method.getLines()).isEqualTo(3);
    }

    private MethodDescriptor create(String name) throws Exception {
        var methods = create();
        return methods.stream().filter(x -> x.matches(name)).findAny().orElseThrow();
    }

    private static Set<MethodDescriptor> create() throws IOException {
        ClassReader classReader = new ClassReader(TestClassA.class.getName());
        var analyzer = new CohesionAnalyzer();
        return analyzer.analyze(classReader);
    }

}