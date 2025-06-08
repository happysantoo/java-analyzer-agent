package com.example.scanner.model;

/**
 * Represents a specific concurrency issue found in Java code.
 */
public class ConcurrencyIssue {
    private String type;
    private String description;
    private String className;
    private String methodName;
    private int lineNumber;
    private IssueSeverity severity;
    private String codeSnippet;
    private String suggestedFix;
    private String title;
    private String filePath;
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    
    public IssueSeverity getSeverity() { return severity; }
    public void setSeverity(IssueSeverity severity) { this.severity = severity; }
    
    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
    
    public String getSuggestedFix() { return suggestedFix; }
    public void setSuggestedFix(String suggestedFix) { this.suggestedFix = suggestedFix; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
