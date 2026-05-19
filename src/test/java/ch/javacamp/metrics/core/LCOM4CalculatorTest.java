package ch.javacamp.metrics.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

class LCOM4CalculatorTest {

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Creates a MethodDescriptor with the given shortName, read fields and invoked local methods.
     * shortName must follow the pattern "methodName():returnType" (e.g. "doWork():void").
     */
    private MethodDescriptor method(String shortName, Set<String> readFields, Set<String> localCalls) {
        String name = shortName.contains("(") ? shortName.substring(0, shortName.indexOf("(")) : shortName;
        var m = new MethodDescriptor("Owner", "public void " + name + "()", shortName,
                Visibility.PUBLIC, name, "void", "");
        readFields.forEach(m::addFieldRead);
        localCalls.forEach(m::addLocalMethodInvocation);
        return m;
    }

    private LCOM4Calculator calculator() {
        return new LCOM4Calculator();
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Empty method set → 0 components")
    void emptyMethodSet_returnsZero() {
        Assertions.assertThat(calculator().calculate(Set.of())).isEqualTo(0);
    }

    @Test
    @DisplayName("Single method with no fields → 1 component")
    void singleMethodNoFields_returnsOne() {
        var m = method("doWork():void", Set.of(), Set.of());
        Assertions.assertThat(calculator().calculate(Set.of(m))).isEqualTo(1);
    }

    @Test
    @DisplayName("Single method with one field → 1 component")
    void singleMethodWithField_returnsOne() {
        var m = method("getX():int", Set.of("x"), Set.of());
        Assertions.assertThat(calculator().calculate(Set.of(m))).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // Connected via shared fields
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Two methods sharing a field → 1 component")
    void twoMethodsSharingField_returnsOne() {
        var m1 = method("getX():int",  Set.of("x"), Set.of());
        var m2 = method("setX():void", Set.of("x"), Set.of());
        Assertions.assertThat(calculator().calculate(Set.of(m1, m2))).isEqualTo(1);
    }

    @Test
    @DisplayName("Two methods with different fields, no shared calls → 2 components")
    void twoMethodsNoSharedFieldOrCall_returnsTwo() {
        var m1 = method("getX():int",  Set.of("x"), Set.of());
        var m2 = method("getY():int",  Set.of("y"), Set.of());
        Assertions.assertThat(calculator().calculate(Set.of(m1, m2))).isEqualTo(2);
    }

    @Test
    @DisplayName("Three methods – transitive field chain → 1 component")
    void transitiveChainViaFields_returnsOne() {
        // m1 shares fieldA with m2; m2 shares fieldB with m3 → all connected transitively
        var m1 = method("m1():void", Set.of("fieldA"),           Set.of());
        var m2 = method("m2():void", Set.of("fieldA", "fieldB"), Set.of());
        var m3 = method("m3():void", Set.of("fieldB"),           Set.of());
        Assertions.assertThat(calculator().calculate(Set.of(m1, m2, m3))).isEqualTo(1);
    }

    @Test
    @DisplayName("Three methods – two groups, no bridge → 2 components")
    void threeMethodsTwoGroups_returnsTwo() {
        var m1 = method("m1():void", Set.of("a"), Set.of());
        var m2 = method("m2():void", Set.of("a"), Set.of()); // connected to m1
        var m3 = method("m3():void", Set.of("z"), Set.of()); // isolated
        Assertions.assertThat(calculator().calculate(Set.of(m1, m2, m3))).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // Connected via local method calls
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("m1 calls m2 via local invocation → 1 component")
    void methodsConnectedViaLocalCall_returnsOne() {
        // m1 invokes m2 → the group for m1 will contain "m2():void", matching m2's shortName
        var m1 = method("m1():void", Set.of(), Set.of("m2():void"));
        var m2 = method("m2():void", Set.of(), Set.of());
        Assertions.assertThat(calculator().calculate(Set.of(m1, m2))).isEqualTo(1);
    }

    @Test
    @DisplayName("No shared fields, no local calls → each method is its own component")
    void noConnectionAtAll_eachMethodIsOwnComponent() {
        var m1 = method("a():void", Set.of(), Set.of());
        var m2 = method("b():void", Set.of(), Set.of());
        var m3 = method("c():void", Set.of(), Set.of());
        Assertions.assertThat(calculator().calculate(Set.of(m1, m2, m3))).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // Mixed connectivity
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Two distinct clusters (field-based) → 2 components")
    void twoDistinctFieldClusters_returnsTwo() {
        var m1 = method("getA():int",  Set.of("a"), Set.of());
        var m2 = method("setA():void", Set.of("a"), Set.of());
        var m3 = method("getB():int",  Set.of("b"), Set.of());
        var m4 = method("setB():void", Set.of("b"), Set.of());
        Assertions.assertThat(calculator().calculate(Set.of(m1, m2, m3, m4))).isEqualTo(2);
    }

    @Test
    @DisplayName("Two clusters merged by a bridge method → 1 component")
    void twoClustersConnectedByBridgeMethod_returnsOne() {
        var m1 = method("getA():int",  Set.of("a"), Set.of());
        var m2 = method("setA():void", Set.of("a"), Set.of());
        // bridge: touches both clusters
        var bridge = method("process():void", Set.of("a", "b"), Set.of());
        var m3 = method("getB():int",  Set.of("b"), Set.of());
        Assertions.assertThat(calculator().calculate(Set.of(m1, m2, bridge, m3))).isEqualTo(1);
    }
}

