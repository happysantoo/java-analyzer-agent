package com.example.scanner.analyzer;

import com.example.scanner.model.*;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Analyzes Java code for thread safety issues.
 * Implements one of the parallel analysis branches from the activity diagram.
 */
@Component
public class ThreadSafetyAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreadSafetyAnalyzer.class);
    
    // Patterns for detecting potential race conditions
    private static final Pattern MUTABLE_FIELD_PATTERN = Pattern.compile(
        "(?:private|protected|public)?\\s+(?!final\\s)\\w+\\s+\\w+\\s*=");
    
    private static final Pattern UNSAFE_COLLECTION_PATTERN = Pattern.compile(
        "(?:HashMap|ArrayList|HashSet|TreeMap|TreeSet|LinkedList)");
    
    /**
     * Analyzes a class for thread safety issues including race conditions and shared mutable state.
     */
    public List<ConcurrencyIssue> analyze(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        logger.debug("Analyzing thread safety for class: {}", classInfo.getName());
        
        // Check for shared mutable state
        issues.addAll(checkSharedMutableState(sourceInfo, classInfo));
        
        // Check for unsafe publication
        issues.addAll(checkUnsafePublication(sourceInfo, classInfo));
        
        // Check for race conditions in methods
        issues.addAll(checkRaceConditions(sourceInfo, classInfo));
        
        logger.debug("Found {} thread safety issues in class: {}", issues.size(), classInfo.getName());
        return issues;
    }
    
    /**
     * Checks for shared mutable state that could lead to race conditions.
     */
    private List<ConcurrencyIssue> checkSharedMutableState(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        for (FieldInfo field : classInfo.getFields()) {
            if (!field.isFinal() && !field.isVolatile() && isSharedMutableType(field.getType())) {
                ConcurrencyIssue issue = new ConcurrencyIssue();
                issue.setType("SHARED_MUTABLE_STATE");
                issue.setClassName(classInfo.getName());
                issue.setLineNumber(field.getLineNumber());
                issue.setSeverity(IssueSeverity.HIGH);
                issue.setDescription(String.format(
                    "Field '%s' of type '%s' is mutable and not thread-safe. " +
                    "Consider making it final, volatile, or using thread-safe alternatives.",
                    field.getName(), field.getType()));
                
                issue.setCodeSnippet(extractCodeSnippet(sourceInfo.getContent(), field.getLineNumber()));
                issue.setSuggestedFix(generateSharedStateFix(field));
                
                issues.add(issue);
            }
        }
        
        return issues;
    }
    
    /**
     * Checks for unsafe publication of objects.
     */
    private List<ConcurrencyIssue> checkUnsafePublication(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        // Check for non-final static fields
        for (FieldInfo field : classInfo.getFields()) {
            if (field.isStatic() && !field.isFinal() && !field.isVolatile()) {
                ConcurrencyIssue issue = new ConcurrencyIssue();
                issue.setType("UNSAFE_PUBLICATION");
                issue.setClassName(classInfo.getName());
                issue.setLineNumber(field.getLineNumber());
                issue.setSeverity(IssueSeverity.MEDIUM);
                issue.setDescription(String.format(
                    "Static field '%s' is not final or volatile, which may lead to unsafe publication. " +
                    "Consider making it final or volatile for thread safety.",
                    field.getName()));
                
                issue.setCodeSnippet(extractCodeSnippet(sourceInfo.getContent(), field.getLineNumber()));
                issue.setSuggestedFix("Make the field final or volatile: " + 
                    (field.getType().contains("Collection") ? "final" : "volatile") + " " + field.getType());
                
                issues.add(issue);
            }
        }
        
        return issues;
    }
    
    /**
     * Checks for potential race conditions in methods.
     */
    private List<ConcurrencyIssue> checkRaceConditions(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        // Look for methods that modify non-final fields without synchronization
        for (MethodInfo method : classInfo.getMethods()) {
            if (!method.isSynchronized() && methodModifiesSharedState(method, classInfo)) {
                ConcurrencyIssue issue = new ConcurrencyIssue();
                issue.setType("POTENTIAL_RACE_CONDITION");
                issue.setClassName(classInfo.getName());
                issue.setMethodName(method.getName());
                issue.setLineNumber(method.getLineNumber());
                issue.setSeverity(IssueSeverity.HIGH);
                issue.setDescription(String.format(
                    "Method '%s' appears to modify shared state without proper synchronization. " +
                    "This could lead to race conditions in multi-threaded environments.",
                    method.getName()));
                
                issue.setCodeSnippet(extractCodeSnippet(sourceInfo.getContent(), method.getLineNumber()));
                issue.setSuggestedFix("Add synchronization: synchronized " + method.getReturnType() + " " + method.getName());
                
                issues.add(issue);
            }
        }
        
        return issues;
    }
    
    /**
     * Determines if a type represents shared mutable state.
     */
    private boolean isSharedMutableType(String type) {
        return UNSAFE_COLLECTION_PATTERN.matcher(type).find() ||
               type.contains("Map") && !type.contains("Concurrent") ||
               type.contains("List") && !type.contains("Concurrent") ||
               type.contains("Set") && !type.contains("Concurrent");
    }
    
    /**
     * Checks if a method likely modifies shared state.
     */
    private boolean methodModifiesSharedState(MethodInfo method, ClassInfo classInfo) {
        // Simple heuristic: methods with void return type or setters
        return method.getReturnType().equals("void") || 
               method.getName().startsWith("set") ||
               method.getName().startsWith("add") ||
               method.getName().startsWith("remove") ||
               method.getName().startsWith("put");
    }
    
    /**
     * Extracts a code snippet around the specified line number.
     */
    private String extractCodeSnippet(String content, int lineNumber) {
        String[] lines = content.split("\n");
        if (lineNumber <= 0 || lineNumber > lines.length) {
            return "";
        }
        
        int start = Math.max(0, lineNumber - 3);
        int end = Math.min(lines.length, lineNumber + 2);
        
        StringBuilder snippet = new StringBuilder();
        for (int i = start; i < end; i++) {
            snippet.append(String.format("%3d: %s%n", i + 1, lines[i]));
        }
        
        return snippet.toString();
    }
    
    /**
     * Generates a suggested fix for shared state issues.
     */
    private String generateSharedStateFix(FieldInfo field) {
        if (field.getType().contains("Map")) {
            return "Use ConcurrentHashMap: private final ConcurrentHashMap<K,V> " + field.getName();
        } else if (field.getType().contains("List")) {
            return "Use Collections.synchronizedList() or CopyOnWriteArrayList: " +
                   "private final List<T> " + field.getName() + " = Collections.synchronizedList(new ArrayList<>())";
        } else if (field.getType().contains("Set")) {
            return "Use Collections.synchronizedSet() or ConcurrentHashMap.newKeySet(): " +
                   "private final Set<T> " + field.getName() + " = ConcurrentHashMap.newKeySet()";
        } else {
            return "Make field volatile or use proper synchronization: volatile " + field.getType() + " " + field.getName();
        }
    }
}
