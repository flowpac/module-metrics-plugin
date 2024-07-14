package ch.javacamp.metrics.analyzer;

import ch.javacamp.metrics.core.Visibility;
import org.assertj.core.api.Assertions;
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

    @Test
    @DisplayName("Test PUBLIC visibility")
    public void t5() throws Exception {
        testVisibility(I1Impl.class, Visibility.PUBLIC);
    }

    @Test
    @DisplayName("Test DEFAULT visibility")
    public void t7() throws Exception {
        testVisibility(C4.class, Visibility.DEFAULT);
    }

    @Test
    @DisplayName("Number of Methods")
    public void t8() throws Exception {
        String className = I1Impl.class.getName();
        ClassReader classReader = new ClassReader(className);
        var result = out.getDependentClasses(classReader);
        Assertions.assertThat(result.numOfMethods()).isEqualTo(2);
    }

    private void testVisibility(Class<?> clazz, Visibility expected) throws Exception{
        String className = clazz.getName();
        ClassReader classReader = new ClassReader(className);
        var result = out.getDependentClasses(classReader);
        Assertions.assertThat(result.visibility()).isEqualTo(expected);
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
    public static class I1Impl extends AnotherClass implements I1, I2{
        public void foo(){}
        private void bar(){}
    }
    static class C4 { }

}