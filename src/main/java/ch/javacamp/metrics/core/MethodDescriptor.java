package ch.javacamp.metrics.core;

import java.util.HashSet;
import java.util.Set;

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
    private final String shortName;
    private final String name;
    private final String returnType;
    private final String parameters;
    private final Visibility visibility;
    private final Set<String> writtenFields;
    private final Set<String> readFields;
    private final Set<String> invokedLocalMethods;
    private int lines;

    public MethodDescriptor(String fullName, String shortName, Visibility visibility, String name, String returnType, String parameters){
        this.fullName = fullName;
        this.shortName = shortName;
        this.visibility = visibility;
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.writtenFields = new HashSet<>();
        this.readFields = new HashSet<>();
        this.invokedLocalMethods = new HashSet<>();
        this.lines = 0;
    }

    public String getFullName() {
        return fullName;
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public int getLines() {
        return lines;
    }

    public boolean isPublic(){
        return visibility == Visibility.PUBLIC;
    }

    public boolean isConstructor(){
        return this.name.contains("<init>") || this.name.contains("<clinit>");
    }

    public String getParameters() {
        return parameters;
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

    public Set<String> getWrittenFields() {
        return writtenFields;
    }

    public Set<String> getReadFields() {
        return readFields;
    }

    public Set<String> getInvokedLocalMethods() {
        return invokedLocalMethods;
    }

    public boolean matches(String shortName){
        return this.shortName.equals(shortName);
    }

    public boolean callsOtherLocalMethods(){
        return !this.invokedLocalMethods.isEmpty();
    }

    public boolean readOrModifyOneSingleField(){
        Set<String> fields = Relation.combine(getReadFields(), getWrittenFields());
        return fields.size() == 1;
    }

    public String getShortName() {
        return shortName;
    }

    public boolean isSpecialMethod(){
        return isConstructor() || SPECIAL_METHODS.contains(this.shortName);
    }

    public void incLineCounter() {
        this.lines++;
    }
}
