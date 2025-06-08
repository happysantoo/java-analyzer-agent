package com.example.scanner.analyzer;

import com.example.scanner.model.*;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * Analyzes Java code for Executor Framework usage patterns.
 */
@Component
public class ExecutorFrameworkAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecutorFrameworkAnalyzer.class);
    
    public List<ConcurrencyIssue> analyze(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        // Check for executor service usage
        if (sourceInfo.getThreadRelatedImports().stream().anyMatch(imp -> imp.contains("Executor"))) {
            issues.addAll(checkExecutorUsage(sourceInfo, classInfo));
        }
        
        return issues;
    }
    
    private List<ConcurrencyIssue> checkExecutorUsage(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        // Check if ExecutorService fields are properly managed
        for (FieldInfo field : classInfo.getFields()) {
            if (field.getType().contains("ExecutorService") || field.getType().contains("ThreadPoolExecutor")) {
                boolean hasShutdownMethod = classInfo.getMethods().stream()
                    .anyMatch(method -> method.getName().contains("shutdown") || method.getName().contains("close"));
                
                if (!hasShutdownMethod) {
                    ConcurrencyIssue issue = new ConcurrencyIssue();
                    issue.setType("EXECUTOR_NOT_SHUTDOWN");
                    issue.setClassName(classInfo.getName());
                    issue.setLineNumber(field.getLineNumber());
                    issue.setSeverity(IssueSeverity.MEDIUM);
                    issue.setDescription("ExecutorService should be properly shutdown to prevent resource leaks");
                    issue.setSuggestedFix("Add shutdown() call in cleanup method or implement AutoCloseable");
                    issues.add(issue);
                }
            }
        }
        
        return issues;
    }
}
