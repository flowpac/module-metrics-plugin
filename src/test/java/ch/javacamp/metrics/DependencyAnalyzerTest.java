package ch.javacamp.metrics;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import java.util.Date;

class DependencyAnalyzerTest {

    private DependencyAnalyzer out;

    @BeforeEach
    void setUp(){
        out = new DependencyAnalyzer();
    }

    @Test
    @DisplayName("Basic analyzer Tests")
    void t1() throws Exception {
        String className = DemoClass.class.getName();
        ClassReader classReader = new ClassReader(className);
        var result = out.getDependentClasses(classReader);

        Assertions.assertThat(result.className()).isEqualTo(DemoClass.class.getName());
        Assertions.assertThat(result.dependencies()).containsExactlyInAnyOrder("java.util.Date","java.lang.Long", "java.lang.Short", "java.lang.Boolean", "java.lang.String", "java.lang.Object", "java.lang.Integer", IllegalArgumentException.class.getName());
    }

    @Test
    @DisplayName("Extract interfaces, super classes")
    public void t2() throws Exception {
        String className = I1Impl.class.getName();
        ClassReader classReader = new ClassReader(className);
        var result = out.getDependentClasses(classReader);

        Assertions.assertThat(result.className()).isEqualTo(I1Impl.class.getName());
        Assertions.assertThat(result.dependencies()).containsExactlyInAnyOrder(I1.class.getName(), I2.class.getName(), AnotherClass.class.getName());
    }

    @Test
    @DisplayName("Test for abstractness")
    public void t3() throws Exception {
        String className = I1.class.getName();
        ClassReader classReader = new ClassReader(className);
        var result = out.getDependentClasses(classReader);

        Assertions.assertThat(result.className()).isEqualTo(I1.class.getName());
        Assertions.assertThat(result.isAbstract()).isTrue();
    }

    public static class DemoClass {
        private String aStringField;

        public DemoClass(Long aLongParameter) {

        }

        public Integer doX(Boolean b) throws IllegalArgumentException{
            Short.toString((short) 1);
            Date date = new Date();
            return 0;
        }
    }

    public interface I1 {}
    public interface I2 {}
    public static abstract class AnotherClass{}
    public static class I1Impl extends AnotherClass implements I1, I2{}

}