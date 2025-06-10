package com.example.scanner.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Information about a Java class.
 */
public class ClassInfo {
    private String name;
    private boolean isInterface;
    private int lineNumber;
    private List<String> parentClasses = new ArrayList<>();
    private List<String> implementedInterfaces = new ArrayList<>();
    private List<MethodInfo> methods = new ArrayList<>();
    private List<FieldInfo> fields = new ArrayList<>();
    
    // Spring annotation tracking
    private Set<String> springAnnotations = new HashSet<>();
    private boolean isSpringManaged = false;
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public boolean isInterface() { return isInterface; }
    public void setInterface(boolean isInterface) { this.isInterface = isInterface; }
    
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    
    public List<String> getParentClasses() { return parentClasses; }
    public void setParentClasses(List<String> parentClasses) { this.parentClasses = parentClasses; }
    
    public List<String> getImplementedInterfaces() { return implementedInterfaces; }
    public void setImplementedInterfaces(List<String> implementedInterfaces) { 
        this.implementedInterfaces = implementedInterfaces; 
    }
    
    public List<MethodInfo> getMethods() { return methods; }
    public void setMethods(List<MethodInfo> methods) { this.methods = methods; }
    
    public List<FieldInfo> getFields() { return fields; }
    public void setFields(List<FieldInfo> fields) { this.fields = fields; }
    
    // Spring annotation methods
    public Set<String> getSpringAnnotations() { return springAnnotations; }
    public void setSpringAnnotations(Set<String> springAnnotations) { 
        this.springAnnotations = springAnnotations; 
    }
    
    public boolean isSpringManaged() { return isSpringManaged; }
    public void setSpringManaged(boolean springManaged) { this.isSpringManaged = springManaged; }
    
    public void addSpringAnnotation(String annotation) {
        this.springAnnotations.add(annotation);
        this.isSpringManaged = true;
    }
    
    public boolean hasSpringAnnotation(String annotation) {
        return springAnnotations.contains(annotation);
    }
}
