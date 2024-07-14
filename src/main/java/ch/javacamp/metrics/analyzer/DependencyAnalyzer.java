package ch.javacamp.metrics.analyzer;

import ch.javacamp.metrics.core.ClassDescriptor;
import ch.javacamp.metrics.core.Visibility;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyAnalyzer {

    public ClassDescriptor analyze(ClassReader classReader) {
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        Set<String> dependentClasses = new HashSet<>();

        // Extract super classes and interfaces
        dependentClasses.addAll(extractSuperclassAndInterfaces(classNode));
        // Extract field types
        dependentClasses.addAll(extractFields(classNode));

        // Extract types in methods
        for (MethodNode method : classNode.methods) {
            // Extract exception types
            dependentClasses.addAll(extractExceptionTypes(method));
            // Extract return type
            extractReturnType(method).ifPresent(dependentClasses::add);
            // Extract argument types
            dependentClasses.addAll(extractMethodArguments(method));
            // Extract local variable types
            dependentClasses.addAll(extractLocalVariable(method));
            // Extract instruction types
            dependentClasses.addAll(extractMethodInstructionTypes(method));
        }

        var ownName = transformClassName(classNode.name);
        dependentClasses.remove(ownName);
        var visibility = Visibility.parse(classNode.access);

        return new ClassDescriptor(ownName, isAbstract(classNode), visibility, dependentClasses, new HashSet<>());
    }

    private Set<String> extractExceptionTypes(MethodNode method) {
        if (method.exceptions == null) {
            return Set.of();
        }
        return method.exceptions.stream()
                .map(this::transformClassName)
                .collect(Collectors.toSet());
    }

    private Set<String> extractMethodInstructionTypes(MethodNode method) {
        if (method.instructions == null) {
            return Set.of();
        }

        Set<String> dependentClasses = new HashSet<>();
        for (var instr : method.instructions) {
            instr.accept(new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    dependentClasses.add(transformClassName(owner));
                }
            });
        }
        return dependentClasses;
    }

    private Set<String> extractLocalVariable(MethodNode method) {
        if (method.localVariables == null) {
            return Set.of();
        }
        return method.localVariables.stream()
                .map(l -> l.desc).map(Type::getType)
                .map(this::addType)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private Set<String> extractMethodArguments(MethodNode method) {
        return Arrays.stream(Type.getArgumentTypes(method.desc))
                .map(this::addType)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private Optional<String> extractReturnType(MethodNode method) {
        return addType(Type.getReturnType(method.desc));
    }

    private Set<String> extractFields(ClassNode classNode) {
        return classNode.fields.stream()
                .map(field -> Type.getType(field.desc))
                .map(this::addType)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private Set<String> extractSuperclassAndInterfaces(ClassNode classNode) {
        Set<String> dependentClasses = new HashSet<>();
        if (classNode.superName != null) {
            dependentClasses.add(transformClassName(classNode.superName));
        }
        for (String interfaceName : classNode.interfaces) {
            dependentClasses.add(transformClassName(interfaceName));
        }
        return dependentClasses;
    }

    private Optional<String> addType(Type type) {
        if (type.getSort() == Type.OBJECT) {
            return Optional.of(type.getClassName());
        } else if (type.getSort() == Type.ARRAY) {
            return addType(type.getElementType());
        }
        return Optional.empty();
    }

    private boolean isAbstract(ClassNode classNode) {
        boolean isInterface = (classNode.access & Opcodes.ACC_INTERFACE) != 0;
        boolean isAbstractClass = (classNode.access & Opcodes.ACC_ABSTRACT) != 0;
        return isAbstractClass || isInterface;
    }

    private String transformClassName(String c) {
        return c.replace("/", ".");
    }

}