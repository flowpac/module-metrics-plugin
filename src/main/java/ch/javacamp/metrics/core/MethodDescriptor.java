package ch.javacamp.metrics.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Getter
@Accessors(fluent = true)
public class MethodDescriptor {

    private final static Set<String> SPECIAL_METHODS;
    static{
        SPECIAL_METHODS = Set.of(
                "equals(java.lang.Object):boolean",
                "hashCode():int",
                "toString():java.lang.String"
        );
    }
    private final String fullName;
    private final String owner;
    private final String shortName;
    private final String name;
    private final String returnType;
    private final String parameters;
    private final Visibility visibility;
    private final Set<String> writtenFields;
    private final Set<String> readFields;
    private final Set<String> invokedLocalMethods;
    private final List<MethodInvocation> invokedMethods;
    private int lines;

    public MethodDescriptor(String owner, String fullName, String shortName, Visibility visibility, String name, String returnType, String parameters){
        this.fullName = fullName;
        this.shortName = shortName;
        this.visibility = visibility;
        this.name = name;
        this.owner = owner;
        this.returnType = returnType;
        this.parameters = parameters;
        this.writtenFields = new HashSet<>();
        this.readFields = new HashSet<>();
        this.invokedLocalMethods = new HashSet<>();
        this.invokedMethods = new ArrayList<>();
        this.lines = 0;
    }

    public boolean isPublic(){
        return visibility == Visibility.PUBLIC;
    }

    public boolean isConstructor(){
        return this.name.contains("<init>") || this.name.contains("<clinit>");
    }

    public void addFieldWrite(String name){
        this.writtenFields.add(name);
    }

    public void addFieldRead(String name){
        this.readFields.add(name);
    }

    public void addLocalMethodInvocation(String methodName){
        this.invokedLocalMethods.add(methodName);
    }

    public boolean matches(String shortName){
        return this.shortName.equals(shortName);
    }

    public boolean callsOtherLocalMethods(){
        return !this.invokedLocalMethods.isEmpty();
    }

    public void addMethodCall(MethodInvocation methodInvocation){
        this.invokedMethods.add(methodInvocation);
    }

    public boolean readOrModifyOneSingleField(){
        Set<String> fields = Relation.combine(readFields(), writtenFields());
        return fields.size() == 1;
    }

    public boolean isSpecialMethod(){
        return isConstructor() || SPECIAL_METHODS.contains(this.shortName);
    }

    public void incLineCounter() {
        this.lines++;
    }

    public String fqn(){
        return String.format("%s#%s", this.owner, this.shortName);
    }

}
