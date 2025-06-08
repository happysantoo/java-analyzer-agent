package com.example.scanner.model;

import java.util.List;
import java.util.ArrayList;

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
}
