package com.example.scanner.model;

/**
 * Information about a Java field.
 */
public class FieldInfo {
    private String name;
    private String type;
    private int lineNumber;
    private boolean isVolatile;
    private boolean isStatic;
    private boolean isFinal;
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    
    public boolean isVolatile() { return isVolatile; }
    public void setVolatile(boolean isVolatile) { this.isVolatile = isVolatile; }
    
    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean isStatic) { this.isStatic = isStatic; }
    
    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
}
