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
class ThreadSafetyAnalyzerTest {
    
    private ThreadSafetyAnalyzer analyzer;
    private JavaParser javaParser;
    
    @BeforeEach
    void setUp() {
        analyzer = new ThreadSafetyAnalyzer();
        javaParser = new JavaParser();
    }
    
    @Test
    void testDetectRaceConditionInStaticField() {
        String code = """
            public class TestClass {
                private static int counter = 0;
                
                public void increment() {
                    counter++;
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().anyMatch(issue -> 
            issue.getTitle().contains("race condition") || 
            issue.getTitle().contains("shared mutable state")));
    }
    
    @Test
    void testDetectUnsafeCollectionUsage() {
        String code = """
            import java.util.ArrayList;
            import java.util.List;
            
            public class TestClass {
                private List<String> items = new ArrayList<>();
                
                public void addItem(String item) {
                    items.add(item);
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        // Should detect unsafe collection usage
        assertTrue(issues.stream().anyMatch(issue -> 
            issue.getDescription().toLowerCase().contains("arraylist") &&
            issue.getDescription().toLowerCase().contains("thread-safe")));
    }
    
    @Test
    void testNoIssuesWithThreadSafeCode() {
        String code = """
            import java.util.concurrent.atomic.AtomicInteger;
            
            public class TestClass {
                private final AtomicInteger counter = new AtomicInteger(0);
                
                public int increment() {
                    return counter.incrementAndGet();
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        // Should not report issues for properly thread-safe code
        assertTrue(issues.isEmpty() || 
                  issues.stream().noneMatch(issue -> 
                      issue.getSeverity() == ConcurrencyIssue.IssueSeverity.HIGH));
    }
    
    @Test
    void testDetectVolatileIssues() {
        String code = """
            public class TestClass {
                private boolean flag = false;
                private int value = 0;
                
                public void setFlag() {
                    value++;
                    flag = true;
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        // Should detect potential visibility issues
        assertFalse(issues.isEmpty());
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
