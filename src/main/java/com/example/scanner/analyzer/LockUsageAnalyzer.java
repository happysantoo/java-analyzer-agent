package com.example.scanner.analyzer;

import com.example.scanner.model.*;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * Analyzes Java code for lock usage patterns including ReentrantLock and ReadWriteLock.
 */
@Component
public class LockUsageAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(LockUsageAnalyzer.class);
    
    public List<ConcurrencyIssue> analyze(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        // Check for lock usage patterns
        if (sourceInfo.getThreadRelatedImports().stream().anyMatch(imp -> imp.contains("locks"))) {
            issues.addAll(checkLockUsage(sourceInfo, classInfo));
        }
        
        return issues;
    }
    
    private List<ConcurrencyIssue> checkLockUsage(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        // Check for Lock fields
        for (FieldInfo field : classInfo.getFields()) {
            if (field.getType().contains("Lock") || field.getType().contains("ReentrantLock")) {
                // Ensure proper try-finally pattern is mentioned in documentation
                ConcurrencyIssue issue = new ConcurrencyIssue();
                issue.setType("LOCK_USAGE_PATTERN");
                issue.setClassName(classInfo.getName());
                issue.setLineNumber(field.getLineNumber());
                issue.setSeverity(IssueSeverity.MEDIUM);
                issue.setDescription("Ensure Lock is used with proper try-finally pattern");
                issue.setSuggestedFix("Use lock.lock(); try { ... } finally { lock.unlock(); } pattern");
                issues.add(issue);
            }
        }
        
        return issues;
    }
}
