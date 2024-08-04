package ch.javacamp.metrics.analyzer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AsmUtils {


    public static String transformClassName(String c) {
        return c.replace("/", ".");
    }

}
