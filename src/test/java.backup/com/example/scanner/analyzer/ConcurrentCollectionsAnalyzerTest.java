package com.example.scanner.analyzer;

import com.example.scanner.model.ConcurrencyIssue;
import com.example.scanner.model.JavaSourceInfo;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentCollectionsAnalyzerTest {
    
    private ConcurrentCollectionsAnalyzer analyzer;
    private JavaParser javaParser;
    
    @BeforeEach
    void setUp() {
        analyzer = new ConcurrentCollectionsAnalyzer();
        javaParser = new JavaParser();
    }
    
    @Test
    void testDetectUnsafeCollectionUsage() {
        String code = """
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            
            public class TestClass {
                private List<String> items = new ArrayList<>();
                private Map<String, Object> cache = new HashMap<>();
                
                public void addItem(String item) {
                    items.add(item);
                    cache.put(item, new Object());
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        assertTrue(issues.stream().anyMatch(issue -> 
            issue.getDescription().toLowerCase().contains("arraylist")));
        assertTrue(issues.stream().anyMatch(issue -> 
            issue.getDescription().toLowerCase().contains("hashmap")));
    }
    
    @Test
    void testDetectVectorUsage() {
        String code = """
            import java.util.Vector;
            
            public class TestClass {
                private Vector<String> items = new Vector<>();
                
                public void addItem(String item) {
                    items.add(item);
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        assertTrue(issues.stream().anyMatch(issue -> 
            issue.getDescription().toLowerCase().contains("vector") &&
            issue.getDescription().toLowerCase().contains("legacy")));
    }
    
    @Test
    void testProperConcurrentCollectionUsage() {
        String code = """
            import java.util.concurrent.ConcurrentHashMap;
            import java.util.concurrent.CopyOnWriteArrayList;
            
            public class TestClass {
                private CopyOnWriteArrayList<String> items = new CopyOnWriteArrayList<>();
                private ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
                
                public void addItem(String item) {
                    items.add(item);
                    cache.put(item, new Object());
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        // Should have no issues for proper concurrent collections
        assertTrue(issues.isEmpty() || 
                  issues.stream().noneMatch(issue -> 
                      issue.getSeverity() == ConcurrencyIssue.IssueSeverity.HIGH));
    }
    
    @Test
    void testDetectSynchronizedWrapper() {
        String code = """
            import java.util.Collections;
            import java.util.ArrayList;
            import java.util.List;
            
            public class TestClass {
                private List<String> items = Collections.synchronizedList(new ArrayList<>());
                
                public void processItems() {
                    for (String item : items) { // Unsafe iteration
                        System.out.println(item);
                    }
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        assertTrue(issues.stream().anyMatch(issue -> 
            issue.getDescription().toLowerCase().contains("synchronized") &&
            issue.getDescription().toLowerCase().contains("iteration")));
    }
    
    private JavaSourceInfo createSourceInfo(String fileName, String content) {
        JavaSourceInfo sourceInfo = new JavaSourceInfo();
        sourceInfo.setFileName(fileName);
        sourceInfo.setFilePath("/test/" + fileName);
        sourceInfo.setPackageName("com.test");
        sourceInfo.setLinesOfCode(content.split("\n").length);
        return sourceInfo;
    }
}
