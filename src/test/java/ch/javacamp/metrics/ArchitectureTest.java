package ch.javacamp.metrics;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

@AnalyzeClasses(packages = "ch.javacamp.metrics")
public class ArchitectureTest {

    @ArchTest
    public static void noPackageCycles(JavaClasses importedClasses) {
        SlicesRuleDefinition.slices()
                .matching("ch.javacamp.metrics.(*)..")
                .should().beFreeOfCycles()
                .check(importedClasses);
    }
}
