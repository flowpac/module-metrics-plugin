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
import java.util.Set;

public class DependencyAnalyzer {

    public static ClassInfo getDependentClasses(ClassReader classReader) throws IOException {
        Set<String> dependentClasses = new HashSet<>();

        // Use ClassNode to make it easier to traverse
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        var ownName = classNode.name.replace("/",".");

        // Add superclass and interfaces
        extractSuperclassAndInterfaces(classNode, dependentClasses);

        // Add dependencies from fields
        for (FieldNode field : classNode.fields) {
            Type type = Type.getType(field.desc);
            addType(dependentClasses, type);
        }

        // Add dependencies from methods
        for (MethodNode method : classNode.methods) {
            // Add return type
            Type returnType = Type.getReturnType(method.desc);
            addType(dependentClasses, returnType);

            // Add argument types
            Type[] argumentTypes = Type.getArgumentTypes(method.desc);
            for (Type argType : argumentTypes) {
                addType(dependentClasses, argType);
            }

            // Add local variables
            if (method.localVariables != null) {
                for (LocalVariableNode localVar : method.localVariables) {
                    Type localVarType = Type.getType(localVar.desc);
                    addType(dependentClasses, localVarType);
                }
            }

            if(method.instructions != null){
                for(var instr:method.instructions){
                    instr.accept(new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            dependentClasses.add(owner.replace("/", "."));
                        }
                    });
                }
            }
        }

        boolean isInterface = (classNode.access & Opcodes.ACC_INTERFACE) != 0;
        boolean isAbstract = (classNode.access & Opcodes.ACC_ABSTRACT) != 0;
        return new ClassInfo(ownName, isInterface || isAbstract, dependentClasses);
    }

    private static void extractSuperclassAndInterfaces(ClassNode classNode, Set<String> dependentClasses) {
        if (classNode.superName != null) {
            dependentClasses.add(classNode.superName.replace('/', '.'));
        }
        for (String interfaceName : classNode.interfaces) {
            dependentClasses.add(interfaceName.replace('/', '.'));
        }
    }

    private static void addType(Set<String> dependentClasses, Type type) {
        if (type.getSort() == Type.OBJECT) {
            dependentClasses.add(type.getClassName());
        } else if (type.getSort() == Type.ARRAY) {
            addType(dependentClasses, type.getElementType());
        }
    }

    public static void main(String[] args) throws IOException {
        String className = DependencyAnalyzer.class.getName();
        ClassReader classReader = new ClassReader(className);
        var result = getDependentClasses(classReader);

        System.out.println("Dependent classes:");
        for (String depClass : result.dependencies()) {
            System.out.println(depClass);
        }
    }
}
