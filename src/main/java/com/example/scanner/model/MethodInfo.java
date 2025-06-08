package com.example.scanner.model;

import java.util.List;
import java.util.ArrayList;

/**
 * Information about a Java method.
 */
public class MethodInfo {
    private String name;
    private int lineNumber;
    private boolean isSynchronized;
    private boolean isStatic;
    private String returnType;
    private List<String> parameterTypes = new ArrayList<>();
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    
    public boolean isSynchronized() { return isSynchronized; }
    public void setSynchronized(boolean isSynchronized) { this.isSynchronized = isSynchronized; }
    
    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean isStatic) { this.isStatic = isStatic; }
    
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    
    public List<String> getParameterTypes() { return parameterTypes; }
    public void setParameterTypes(List<String> parameterTypes) { this.parameterTypes = parameterTypes; }
}
