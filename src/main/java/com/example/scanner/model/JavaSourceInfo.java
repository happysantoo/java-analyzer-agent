package com.example.scanner.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Information about a Java source file.
 */
public class JavaSourceInfo {
    private String fileName;
    private String filePath;
    private List<ClassInfo> classes = new ArrayList<>();
    private int totalLines;
    private String packageName;
    private String content;
    private Set<String> threadRelatedImports = new HashSet<>();
    
    // Constructors
    public JavaSourceInfo() {}
    
    public JavaSourceInfo(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }
    
    // Getters and Setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public List<ClassInfo> getClasses() { return classes; }
    public void setClasses(List<ClassInfo> classes) { this.classes = classes; }
    
    public int getTotalLines() { return totalLines; }
    public void setTotalLines(int totalLines) { this.totalLines = totalLines; }
    
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Set<String> getThreadRelatedImports() { return threadRelatedImports; }
    public void setThreadRelatedImports(Set<String> threadRelatedImports) { this.threadRelatedImports = threadRelatedImports; }
    
    // Utility methods
    public void addClass(ClassInfo classInfo) {
        if (classes == null) {
            classes = new ArrayList<>();
        }
        classes.add(classInfo);
    }
}