package ch.javacamp.metrics;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class DependencyAnalyzer {

    public ClassDescriptor getDependentClasses(ClassReader classReader) throws IOException {
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
        return new ClassDescriptor(ownName, isAbstract(classNode), dependentClasses);
    }

    private Set<String> extractExceptionTypes(MethodNode method) {
        Set<String> dependentClasses = new HashSet<>();
        if(method.exceptions != null){
            dependentClasses.addAll(method.exceptions.stream().map(this::transformClassName).toList());
        }
        return dependentClasses;
    }

    private Set<String> extractMethodInstructionTypes(MethodNode method) {
        Set<String> dependentClasses = new HashSet<>();
        if(method.instructions != null){
            for(var instr: method.instructions){
                instr.accept(new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        dependentClasses.add(transformClassName(owner));
                    }
                });
            }
        }
        return dependentClasses;
    }

    private Set<String> extractLocalVariable(MethodNode method) {
        Set<String> dependentClasses = new HashSet<>();
        if (method.localVariables != null) {
            for (LocalVariableNode localVar : method.localVariables) {
                Type localVarType = Type.getType(localVar.desc);
                addType(localVarType).ifPresent(dependentClasses::add);
            }
        }
        return dependentClasses;
    }

    private Set<String> extractMethodArguments(MethodNode method) {
        Set<String> dependentClasses = new HashSet<>();
        Type[] argumentTypes = Type.getArgumentTypes(method.desc);
        for (Type argType : argumentTypes) {
            addType(argType).ifPresent(dependentClasses::add);
        }
        return dependentClasses;
    }

    private Optional<String> extractReturnType(MethodNode method) {
        Type returnType = Type.getReturnType(method.desc);
        return addType(returnType);
    }

    private Set<String> extractFields(ClassNode classNode) {
        Set<String> dependentClasses = new HashSet<>();
        for (FieldNode field : classNode.fields) {
            Type type = Type.getType(field.desc);
            addType(type).ifPresent(dependentClasses::add);
        }
        return dependentClasses;
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

    private String transformClassName(String c){
        return c.replace("/",".");
    }
    private boolean isAbstract(ClassNode classNode) {
        boolean isInterface = (classNode.access & Opcodes.ACC_INTERFACE) != 0;
        boolean isAbstractClass = (classNode.access & Opcodes.ACC_ABSTRACT) != 0;
        return isAbstractClass || isInterface;
    }

}
