package com.example.scanner.analyzer;

import com.example.scanner.model.*;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * Analyzes Java code for concurrent collections usage patterns.
 */
@Component
public class ConcurrentCollectionsAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentCollectionsAnalyzer.class);
    
    private static final Set<String> UNSAFE_COLLECTIONS = Set.of(
        "HashMap", "ArrayList", "HashSet", "TreeMap", "TreeSet", "LinkedList", "Vector"
    );
    
    private static final Set<String> SAFE_COLLECTIONS = Set.of(
        "ConcurrentHashMap", "CopyOnWriteArrayList", "ConcurrentLinkedQueue", 
        "LinkedBlockingQueue", "ArrayBlockingQueue", "PriorityBlockingQueue",
        "DelayQueue", "SynchronousQueue", "LinkedTransferQueue", "ConcurrentSkipListMap",
        "ConcurrentSkipListSet"
    );
    
    public List<ConcurrencyIssue> analyze(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        // Check for unsafe collection usage
        issues.addAll(checkUnsafeCollections(sourceInfo, classInfo));
        
        return issues;
    }
    
    private List<ConcurrencyIssue> checkUnsafeCollections(JavaSourceInfo sourceInfo, ClassInfo classInfo) {
        List<ConcurrencyIssue> issues = new ArrayList<>();
        
        for (FieldInfo field : classInfo.getFields()) {
            // First check if it's a safe collection - if so, skip
            boolean isSafe = SAFE_COLLECTIONS.stream().anyMatch(safeType -> field.getType().contains(safeType));
            if (isSafe) {
                continue;
            }
            
            // Then check if it's an unsafe collection
            for (String unsafeType : UNSAFE_COLLECTIONS) {
                if (field.getType().contains(unsafeType) && !field.isFinal()) {
                    ConcurrencyIssue issue = new ConcurrencyIssue();
                    issue.setType("UNSAFE_COLLECTION");
                    issue.setClassName(classInfo.getName());
                    issue.setLineNumber(field.getLineNumber());
                    issue.setSeverity(IssueSeverity.MEDIUM);
                    issue.setDescription(String.format(
                        "Field '%s' uses %s which is not thread-safe. Consider using concurrent alternatives.",
                        field.getName(), unsafeType));
                    issue.setSuggestedFix(getSafeAlternative(unsafeType));
                    issues.add(issue);
                    break; // Only report one issue per field
                }
            }
        }
        
        return issues;
    }
    
    private String getSafeAlternative(String unsafeType) {
        return switch (unsafeType) {
            case "HashMap" -> "Use ConcurrentHashMap instead";
            case "ArrayList" -> "Use CopyOnWriteArrayList or Collections.synchronizedList()";
            case "HashSet" -> "Use ConcurrentHashMap.newKeySet() or Collections.synchronizedSet()";
            case "TreeMap" -> "Use ConcurrentSkipListMap for sorted concurrent map";
            case "TreeSet" -> "Use ConcurrentSkipListSet for sorted concurrent set";
            case "LinkedList" -> "Use ConcurrentLinkedQueue for concurrent queue operations";
            case "Vector" -> "Use CopyOnWriteArrayList or Collections.synchronizedList() instead of legacy Vector";
            default -> "Use thread-safe alternative";
        };
    }
}
