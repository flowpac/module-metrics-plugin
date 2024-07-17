package ch.javacamp.metrics.analyzer;

import ch.javacamp.metrics.core.MethodDescriptor;
import ch.javacamp.metrics.core.Visibility;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CohesionAnalyzer {

    public enum FieldOperation { READ, WRITE, IRRELEVANT;
        public static FieldOperation parse(int asmOpCode) {
            return switch (asmOpCode) {
                case Opcodes.PUTFIELD -> WRITE;
                case Opcodes.GETFIELD -> READ;
                default -> IRRELEVANT;
            };
        }
    }

    public Set<MethodDescriptor> analyze(ClassReader classReader){
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        Map<String, MethodDescriptor> methods = new HashMap<>();
        for(MethodNode methodNode:classNode.methods){
            var shortName = generateShortMethodName(methodNode);
            var method = methods.computeIfAbsent(shortName, ignored -> new MethodDescriptor(generateFullMethodSignature(methodNode),
                    generateShortMethodName(methodNode),
                    Visibility.parse(methodNode.access),
                    methodNode.name,
                    getReturnType(methodNode),
                    getParameterTypes(methodNode.desc)));

            var visitor = new CohesionMethodAnalyzer(classNode.name, method);
            methodNode.accept(visitor);
        }
        return Set.copyOf(methods.values());
    }

    private String generateFullMethodSignature(MethodNode method){
        var returnType = getReturnType(method);
        var parameterTypes = getParameterTypes(method.desc);
        var methodVisibility = Visibility.parse(method.access).name().toLowerCase();
        return String.format("%s %s %s(%s)", methodVisibility, returnType, method.name, parameterTypes);
    }

    private static String getReturnType(MethodNode method) {
        return Type.getReturnType(method.desc).getClassName();
    }

    private static String generateShortMethodName(MethodNode method){
        return generateShortMethodName(method.name, method.desc);
    }

    private static String generateShortMethodName(String name, String descriptor){
        var returnType = Type.getReturnType(descriptor).getClassName();
        var parameterTypes = getParameterTypes(descriptor);
        return String.format("%s(%s):%s", name, parameterTypes, returnType);
    }

    private static String getParameterTypes(String descriptor) {
        return String.join("; ", Arrays.stream(Type.getMethodType(descriptor).getArgumentTypes()).map(Type::getClassName).toList());
    }

    private static class CohesionMethodAnalyzer extends MethodVisitor {

        private final String owner;
        private final MethodDescriptor method;

        CohesionMethodAnalyzer(String owner, MethodDescriptor method){
            super(Opcodes.ASM9);
            this.owner = owner;
            this.method = method;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldType) {
            switch(FieldOperation.parse(opcode)){
                case READ -> method.addFieldRead(fieldName);
                case WRITE -> method.addFieldWrite(fieldName);
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            if(this.owner.equals(owner)) {
                method.addLocalMethodInvocation(generateShortMethodName(name, descriptor));
            }
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            this.method.incLineCounter();
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }
    }
}
