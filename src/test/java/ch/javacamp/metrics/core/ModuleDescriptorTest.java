package ch.javacamp.metrics.core;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ModuleDescriptorTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MethodDescriptor method(String shortName, Visibility visibility, int lines, int additionalComplexity) {
        String name = shortName.contains("(") ? shortName.substring(0, shortName.indexOf("(")) : shortName;
        var m = new MethodDescriptor("Owner", "public void " + name + "()", shortName,
                visibility, name, "void", "");
        for (int i = 0; i < lines; i++) m.incLineCounter();
        for (int i = 0; i < additionalComplexity; i++) m.incComplexity();
        return m;
    }

    private MethodDescriptor publicMethod(String shortName, int lines) {
        return method(shortName, Visibility.PUBLIC, lines, 0);
    }

    private MethodDescriptor constructor(int lines) {
        return new MethodDescriptor("Owner", "public Owner()", "<init>():void",
                Visibility.PUBLIC, "<init>", "void", "");
    }

    private MethodDescriptor methodWithField(String shortName, String fieldName) {
        var m = method(shortName, Visibility.PUBLIC, 2, 0);
        m.addFieldRead(fieldName);
        return m;
    }

    private ClassDescriptor cls(String className, boolean isAbstract, Visibility viz, MethodDescriptor... methods) {
        var methodSet = new HashSet<MethodDescriptor>();
        for (var m : methods) methodSet.add(m);
        return new ClassDescriptor(className, isAbstract, viz, Set.of(), methodSet, Set.of());
    }

    private ClassDescriptor cls(String className, Set<String> deps) {
        return new ClassDescriptor(className, false, Visibility.PUBLIC, deps, new HashSet<>(), Set.of());
    }

    // -------------------------------------------------------------------------
    // totalLines
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("totalLines: sums all method lines across all classes")
    void totalLines_sumsAllMethodLines() {
        var m1 = publicMethod("doWork():void", 10);
        var m2 = publicMethod("validate():void", 5);
        var m3 = publicMethod("execute():void", 8);
        var clsA = cls("pkg.A", false, Visibility.PUBLIC, m1, m2);
        var clsB = cls("pkg.B", false, Visibility.PUBLIC, m3);

        var module = new ModuleDescriptor("mod", Set.of(clsA, clsB));

        Assertions.assertThat(module.totalLines()).isEqualTo(23);
    }

    @Test
    @DisplayName("totalLines: returns 0 for module with no methods")
    void totalLines_noMethods_returnsZero() {
        var clsA = cls("pkg.A", false, Visibility.PUBLIC);
        var module = new ModuleDescriptor("mod", Set.of(clsA));
        Assertions.assertThat(module.totalLines()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // averageLCOM4
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("averageLCOM4: ignores classes with no non-special methods")
    void averageLCOM4_ignoresClassesWithoutFilteredMethods() {
        // class with only special methods → filtered out from average
        var equalsMethod = method("equals(java.lang.Object):boolean", Visibility.PUBLIC, 1, 0);
        var hashCodeMethod = method("hashCode():int", Visibility.PUBLIC, 1, 0);
        var clsSpecialOnly = cls("pkg.Special", false, Visibility.PUBLIC, equalsMethod, hashCodeMethod);

        // class with one normal method → lcom4 = 1
        var normalMethod = publicMethod("doWork():void", 3);
        var clsNormal = cls("pkg.Normal", false, Visibility.PUBLIC, normalMethod);

        var module = new ModuleDescriptor("mod", Set.of(clsSpecialOnly, clsNormal));

        // Only clsNormal is counted; clsSpecialOnly has no filtered methods
        Assertions.assertThat(module.averageLCOM4()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("averageLCOM4: returns 0 when all classes have no filtered methods")
    void averageLCOM4_allSpecialMethods_returnsZero() {
        var ctor = constructor(2);
        var clsA = cls("pkg.A", false, Visibility.PUBLIC, ctor);
        var module = new ModuleDescriptor("mod", Set.of(clsA));
        Assertions.assertThat(module.averageLCOM4()).isEqualTo(0.0);
    }

    // -------------------------------------------------------------------------
    // shareOfGetterSetters
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("shareOfGetterSetters: correct ratio of single-field methods")
    void shareOfGetterSetters_calculatesCorrectRatio() {
        // getter reads one field → counts as getter/setter
        var getter = methodWithField("getName():java.lang.String", "name");
        // complex method reads two fields → does not count
        var complex = method("process():void", Visibility.PUBLIC, 5, 0);
        complex.addFieldRead("fieldA");
        complex.addFieldRead("fieldB");

        var clsA = cls("pkg.A", false, Visibility.PUBLIC, getter, complex);
        var module = new ModuleDescriptor("mod", Set.of(clsA));

        // 1 out of 2 methods touches exactly one field
        Assertions.assertThat(module.shareOfGetterSetters()).isCloseTo(0.5, Offset.offset(0.01));
    }

    // -------------------------------------------------------------------------
    // publicApiSurface
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("publicApiSurface: only public methods on public classes, no constructors")
    void publicApiSurface_countsOnlyPublicMethodsOnPublicClasses() {
        var pubMethod1 = publicMethod("doWork():void", 3);
        var pubMethod2 = publicMethod("validate():void", 2);
        var privMethod  = method("helper():void", Visibility.PRIVATE, 1, 0);
        var ctor = constructor(1);
        var pubClass = cls("pkg.PubClass", false, Visibility.PUBLIC, pubMethod1, pubMethod2, privMethod, ctor);

        // DEFAULT visibility class — its public methods should NOT count
        var pubMethodOnHiddenClass = publicMethod("hidden():void", 1);
        var hiddenClass = cls("pkg.HiddenClass", false, Visibility.DEFAULT, pubMethodOnHiddenClass);

        var module = new ModuleDescriptor("mod", Set.of(pubClass, hiddenClass));

        // Only pubMethod1 + pubMethod2 from pubClass are counted
        Assertions.assertThat(module.publicApiSurface()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // averageCyclomaticComplexity
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("averageCyclomaticComplexity: excludes special methods and zero-line methods")
    void averageCyclomaticComplexity_excludesSpecialAndZeroLineMethods() {
        // normal method: lines=3, base CC=1, +2 → CC=3
        var normal = method("process():void", Visibility.PUBLIC, 3, 2);
        // zero-line method → excluded from average
        var noLines = method("empty():void", Visibility.PUBLIC, 0, 0);
        // special method → excluded
        var toStr = method("toString():java.lang.String", Visibility.PUBLIC, 2, 0);

        var clsA = cls("pkg.A", false, Visibility.PUBLIC, normal, noLines, toStr);
        var module = new ModuleDescriptor("mod", Set.of(clsA));

        // Only "normal" counts: CC = 3
        Assertions.assertThat(module.averageCyclomaticComplexity()).isCloseTo(3.0, Offset.offset(0.01));
    }

    // -------------------------------------------------------------------------
    // maxCyclomaticComplexity
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("maxCyclomaticComplexity: returns highest CC across all non-special methods")
    void maxCyclomaticComplexity_returnsHighestValue() {
        var simple  = method("simple():void",  Visibility.PUBLIC, 2, 1);  // CC = 2
        var complex = method("complex():void", Visibility.PUBLIC, 5, 8);  // CC = 9
        var medium  = method("medium():void",  Visibility.PUBLIC, 3, 3);  // CC = 4

        var clsA = cls("pkg.A", false, Visibility.PUBLIC, simple, complex, medium);
        var module = new ModuleDescriptor("mod", Set.of(clsA));

        Assertions.assertThat(module.maxCyclomaticComplexity()).isEqualTo(9);
    }

    @Test
    @DisplayName("maxCyclomaticComplexity: returns 0 for empty module")
    void maxCyclomaticComplexity_emptyModule_returnsZero() {
        var clsA = cls("pkg.A", false, Visibility.PUBLIC);
        var module = new ModuleDescriptor("mod", Set.of(clsA));
        Assertions.assertThat(module.maxCyclomaticComplexity()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // dependencyInversionRatio
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("dependencyInversionRatio: 0 when module has no resolved dependencies")
    void dependencyInversionRatio_noDependencies_returnsZero() {
        var clsA = cls("pkg.A", Set.of("unresolved.X"));
        var module = new ModuleDescriptor("mod", Set.of(clsA));

        // "unresolved.X" is not in the index → totalDeps stays 0
        Assertions.assertThat(module.dependencyInversionRatio(Map.of())).isEqualTo(0.0);
    }

    @Test
    @DisplayName("dependencyInversionRatio: 1.0 when all resolved deps are abstract")
    void dependencyInversionRatio_allAbstract_returnsOne() {
        var clsA = cls("pkg.A", Set.of("api.I1", "api.I2"));
        var module = new ModuleDescriptor("mod", Set.of(clsA));

        var index = Map.of("api.I1", true, "api.I2", true);
        Assertions.assertThat(module.dependencyInversionRatio(index)).isCloseTo(1.0, Offset.offset(0.01));
    }

    @Test
    @DisplayName("dependencyInversionRatio: 0.5 for half abstract, half concrete deps")
    void dependencyInversionRatio_halfAbstract() {
        var clsA = cls("pkg.A", Set.of("api.I1", "impl.C1"));
        var module = new ModuleDescriptor("mod", Set.of(clsA));

        var index = Map.of("api.I1", true, "impl.C1", false);
        Assertions.assertThat(module.dependencyInversionRatio(index)).isCloseTo(0.5, Offset.offset(0.01));
    }

    // -------------------------------------------------------------------------
    // averageMethodsPerInterface / maxMethodsOnInterface
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("averageMethodsPerInterface: returns 0.0 when no abstract classes exist")
    void averageMethodsPerInterface_noInterfaces_returnsZero() {
        var m = publicMethod("doWork():void", 3);
        var concrete = cls("pkg.Concrete", false, Visibility.PUBLIC, m);
        var module = new ModuleDescriptor("mod", Set.of(concrete));
        Assertions.assertThat(module.averageMethodsPerInterface()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("averageMethodsPerInterface: average over interfaces with at least one method")
    void averageMethodsPerInterface_twoInterfacesWithDifferentMethodCounts() {
        // interface with 3 methods
        var i1m1 = publicMethod("a():void", 0);
        var i1m2 = publicMethod("b():void", 0);
        var i1m3 = publicMethod("c():void", 0);
        var iface1 = cls("pkg.IFace1", true, Visibility.PUBLIC, i1m1, i1m2, i1m3);

        // interface with 1 method
        var i2m1 = publicMethod("x():void", 0);
        var iface2 = cls("pkg.IFace2", true, Visibility.PUBLIC, i2m1);

        var module = new ModuleDescriptor("mod", Set.of(iface1, iface2));

        // average of 3 and 1 = 2.0 (countMethodsInClass excludes constructors)
        Assertions.assertThat(module.averageMethodsPerInterface()).isCloseTo(2.0, Offset.offset(0.01));
    }

    @Test
    @DisplayName("maxMethodsOnInterface: returns count of largest interface")
    void maxMethodsOnInterface_returnsLargestInterfaceMethodCount() {
        var m1 = publicMethod("a():void", 0);
        var m2 = publicMethod("b():void", 0);
        var m3 = publicMethod("c():void", 0);
        var m4 = publicMethod("d():void", 0);
        var bigInterface = cls("pkg.BigInterface", true, Visibility.PUBLIC, m1, m2, m3, m4);

        var smallM = publicMethod("x():void", 0);
        var smallInterface = cls("pkg.SmallInterface", true, Visibility.PUBLIC, smallM);

        var module = new ModuleDescriptor("mod", Set.of(bigInterface, smallInterface));

        Assertions.assertThat(module.maxMethodsOnInterface()).isEqualTo(4);
    }
}

