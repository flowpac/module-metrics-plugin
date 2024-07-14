package ch.javacamp.metrics.core;

import org.objectweb.asm.Opcodes;

public enum Visibility {
    PUBLIC, PRIVATE, PROTECTED, DEFAULT;

    public static Visibility parse(int access) {
        Visibility visibility;
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            visibility = Visibility.PUBLIC;
        } else if ((access & Opcodes.ACC_PROTECTED) != 0) {
            visibility = Visibility.PROTECTED;
        } else if ((access & Opcodes.ACC_PRIVATE) != 0) {
            visibility = Visibility.PRIVATE;
        } else {
            visibility = Visibility.DEFAULT;
        }
        return visibility;
    }

}
