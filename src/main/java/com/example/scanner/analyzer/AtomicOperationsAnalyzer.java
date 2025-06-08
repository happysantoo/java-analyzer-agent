package com.example.scanner.analyzer;

import com.example.scanner.model.*;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * Analyzes Java code for atomic operations usage patterns.
 */
@Component
public class AtomicOperationsAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(AtomicOperationsAnalyzer.class);
    
    public List<ConcurrencyIssue> analyze(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        // Check for opportunities to use atomic operations
        issues.addAll(checkAtomicOpportunities(sourceInfo, classInfo));
        
        return issues;
    }
    
    private List<ConcurrencyIssue> checkAtomicOpportunities(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        // Look for primitive fields that could benefit from atomic alternatives
        for (FieldInfo field : classInfo.getFields()) {
            if (isPrimitiveCounter(field) && !field.isVolatile()) {
                ConcurrencyIssue issue = new ConcurrencyIssue();
                issue.setType("ATOMIC_OPPORTUNITY");
                issue.setClassName(classInfo.getName());
                issue.setLineNumber(field.getLineNumber());
                issue.setSeverity(IssueSeverity.LOW);
                issue.setDescription(String.format(
                    "Field '%s' could benefit from atomic operations for thread safety",
                    field.getName()));
                issue.setSuggestedFix(getAtomicAlternative(field.getType()));
                issues.add(issue);
            }
        }
        
        return issues;
    }
    
    private boolean isPrimitiveCounter(FieldInfo field) {
        String name = field.getName().toLowerCase();
        return (field.getType().equals("int") || field.getType().equals("long")) &&
               (name.contains("count") || name.contains("index") || name.contains("size"));
    }
    
    private String getAtomicAlternative(String type) {
        return switch (type) {
            case "int" -> "Consider using AtomicInteger";
            case "long" -> "Consider using AtomicLong";
            case "boolean" -> "Consider using AtomicBoolean";
            default -> "Consider using appropriate atomic type";
        };
    }
}
