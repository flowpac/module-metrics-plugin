package ch.javacamp.metrics.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MethodDescriptorTest {

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private MethodDescriptor method(String shortName, String name) {
        return new MethodDescriptor("pkg.Owner", "public void " + name + "()", shortName,
                Visibility.PUBLIC, name, "void", "");
    }

    private MethodDescriptor method(String shortName, String name, Visibility visibility) {
        return new MethodDescriptor("pkg.Owner", visibility.name().toLowerCase() + " void " + name + "()",
                shortName, visibility, name, "void", "");
    }

    private MethodDescriptor methodWithParams(String params) {
        return new MethodDescriptor("pkg.Owner", "public void go()", "go():void",
                Visibility.PUBLIC, "go", "void", params);
    }

    // -------------------------------------------------------------------------
    // isConstructor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isConstructor: <init> is recognised as constructor")
    void isConstructor_initName_returnsTrue() {
        var m = method("<init>():void", "<init>");
        Assertions.assertThat(m.isConstructor()).isTrue();
    }

    @Test
    @DisplayName("isConstructor: <clinit> is recognised as constructor")
    void isConstructor_clinitName_returnsTrue() {
        var m = method("<clinit>():void", "<clinit>");
        Assertions.assertThat(m.isConstructor()).isTrue();
    }

    @Test
    @DisplayName("isConstructor: normal method is not a constructor")
    void isConstructor_normalMethod_returnsFalse() {
        var m = method("doWork():void", "doWork");
        Assertions.assertThat(m.isConstructor()).isFalse();
    }

    // -------------------------------------------------------------------------
    // isSpecialMethod
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isSpecialMethod: equals is a special method")
    void isSpecialMethod_equals_returnsTrue() {
        var m = method("equals(java.lang.Object):boolean", "equals");
        Assertions.assertThat(m.isSpecialMethod()).isTrue();
    }

    @Test
    @DisplayName("isSpecialMethod: hashCode is a special method")
    void isSpecialMethod_hashCode_returnsTrue() {
        var m = method("hashCode():int", "hashCode");
        Assertions.assertThat(m.isSpecialMethod()).isTrue();
    }

    @Test
    @DisplayName("isSpecialMethod: toString is a special method")
    void isSpecialMethod_toString_returnsTrue() {
        var m = method("toString():java.lang.String", "toString");
        Assertions.assertThat(m.isSpecialMethod()).isTrue();
    }

    @Test
    @DisplayName("isSpecialMethod: constructor (<init>) is a special method")
    void isSpecialMethod_constructor_returnsTrue() {
        var m = method("<init>():void", "<init>");
        Assertions.assertThat(m.isSpecialMethod()).isTrue();
    }

    @Test
    @DisplayName("isSpecialMethod: normal business method is not special")
    void isSpecialMethod_normalMethod_returnsFalse() {
        var m = method("calculate():void", "calculate");
        Assertions.assertThat(m.isSpecialMethod()).isFalse();
    }

    // -------------------------------------------------------------------------
    // isPublic
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isPublic: PUBLIC visibility returns true")
    void isPublic_publicVisibility_returnsTrue() {
        var m = method("doWork():void", "doWork", Visibility.PUBLIC);
        Assertions.assertThat(m.isPublic()).isTrue();
    }

    @Test
    @DisplayName("isPublic: PRIVATE visibility returns false")
    void isPublic_privateVisibility_returnsFalse() {
        var m = method("helper():void", "helper", Visibility.PRIVATE);
        Assertions.assertThat(m.isPublic()).isFalse();
    }

    @Test
    @DisplayName("isPublic: DEFAULT visibility returns false")
    void isPublic_defaultVisibility_returnsFalse() {
        var m = method("packageMethod():void", "packageMethod", Visibility.DEFAULT);
        Assertions.assertThat(m.isPublic()).isFalse();
    }

    // -------------------------------------------------------------------------
    // parameterCount
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("parameterCount: empty parameters returns 0")
    void parameterCount_emptyParams_returnsZero() {
        Assertions.assertThat(methodWithParams("").parameterCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("parameterCount: single parameter returns 1")
    void parameterCount_oneParam_returnsOne() {
        Assertions.assertThat(methodWithParams("java.lang.String").parameterCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("parameterCount: multiple parameters returns correct count")
    void parameterCount_multipleParams_returnsCorrectCount() {
        Assertions.assertThat(methodWithParams("java.lang.String; int; boolean").parameterCount()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // readOrModifyOneSingleField
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("readOrModifyOneSingleField: no fields accessed → false")
    void readOrModifyOneSingleField_noFields_returnsFalse() {
        var m = method("doWork():void", "doWork");
        Assertions.assertThat(m.readOrModifyOneSingleField()).isFalse();
    }

    @Test
    @DisplayName("readOrModifyOneSingleField: reads exactly one field → true")
    void readOrModifyOneSingleField_readOneField_returnsTrue() {
        var m = method("getName():java.lang.String", "getName");
        m.addFieldRead("name");
        Assertions.assertThat(m.readOrModifyOneSingleField()).isTrue();
    }

    @Test
    @DisplayName("readOrModifyOneSingleField: writes exactly one field → true")
    void readOrModifyOneSingleField_writeOneField_returnsTrue() {
        var m = method("setName():void", "setName");
        m.addFieldWrite("name");
        Assertions.assertThat(m.readOrModifyOneSingleField()).isTrue();
    }

    @Test
    @DisplayName("readOrModifyOneSingleField: reads and writes same field → true (set size = 1)")
    void readOrModifyOneSingleField_readAndWriteSameField_returnsTrue() {
        var m = method("increment():void", "increment");
        m.addFieldRead("count");
        m.addFieldWrite("count");
        Assertions.assertThat(m.readOrModifyOneSingleField()).isTrue();
    }

    @Test
    @DisplayName("readOrModifyOneSingleField: reads two different fields → false")
    void readOrModifyOneSingleField_twoDifferentFields_returnsFalse() {
        var m = method("process():void", "process");
        m.addFieldRead("a");
        m.addFieldRead("b");
        Assertions.assertThat(m.readOrModifyOneSingleField()).isFalse();
    }

    @Test
    @DisplayName("readOrModifyOneSingleField: reads one field and writes a different field → false")
    void readOrModifyOneSingleField_readOneWriteAnother_returnsFalse() {
        var m = method("transform():void", "transform");
        m.addFieldRead("source");
        m.addFieldWrite("target");
        Assertions.assertThat(m.readOrModifyOneSingleField()).isFalse();
    }

    // -------------------------------------------------------------------------
    // callsOtherLocalMethods
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("callsOtherLocalMethods: no local invocations → false")
    void callsOtherLocalMethods_noInvocations_returnsFalse() {
        var m = method("doWork():void", "doWork");
        Assertions.assertThat(m.callsOtherLocalMethods()).isFalse();
    }

    @Test
    @DisplayName("callsOtherLocalMethods: with a local invocation → true")
    void callsOtherLocalMethods_withInvocation_returnsTrue() {
        var m = method("doWork():void", "doWork");
        m.addLocalMethodInvocation("validate():boolean");
        Assertions.assertThat(m.callsOtherLocalMethods()).isTrue();
    }

    // -------------------------------------------------------------------------
    // incLineCounter / incComplexity
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("incLineCounter: line count increases with each call")
    void incLineCounter_incrementsLines() {
        var m = method("doWork():void", "doWork");
        Assertions.assertThat(m.lines()).isEqualTo(0);
        m.incLineCounter();
        m.incLineCounter();
        m.incLineCounter();
        Assertions.assertThat(m.lines()).isEqualTo(3);
    }

    @Test
    @DisplayName("incComplexity: cyclomatic complexity starts at 1 and increments")
    void incComplexity_startsAtOneAndIncrements() {
        var m = method("doWork():void", "doWork");
        Assertions.assertThat(m.cyclomaticComplexity()).isEqualTo(1);
        m.incComplexity();
        m.incComplexity();
        Assertions.assertThat(m.cyclomaticComplexity()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // fqn
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("fqn: returns owner#shortName")
    void fqn_returnsOwnerHashShortName() {
        var m = method("doWork():void", "doWork");
        Assertions.assertThat(m.fqn()).isEqualTo("pkg.Owner#doWork():void");
    }
}

