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
class ExecutorFrameworkAnalyzerTest {
    
    private ExecutorFrameworkAnalyzer analyzer;
    private JavaParser javaParser;
    
    @BeforeEach
    void setUp() {
        analyzer = new ExecutorFrameworkAnalyzer();
        javaParser = new JavaParser();
    }
    
    @Test
    void testDetectExecutorNotShutdown() {
        String code = """
            import java.util.concurrent.Executors;
            import java.util.concurrent.ExecutorService;
            
            public class TestClass {
                public void processItems() {
                    ExecutorService executor = Executors.newFixedThreadPool(10);
                    executor.submit(() -> {
                        System.out.println("Task");
                    });
                    // Missing executor.shutdown()
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        assertTrue(issues.stream().anyMatch(issue -> 
            issue.getDescription().toLowerCase().contains("shutdown")));
    }
    
    @Test
    void testDetectImproperThreadPoolSize() {
        String code = """
            import java.util.concurrent.Executors;
            import java.util.concurrent.ExecutorService;
            
            public class TestClass {
                public void processItems() {
                    ExecutorService executor = Executors.newFixedThreadPool(1000);
                    // Very large thread pool
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        assertTrue(issues.stream().anyMatch(issue -> 
            issue.getDescription().toLowerCase().contains("thread pool size")));
    }
    
    @Test
    void testProperExecutorUsage() {
        String code = """
            import java.util.concurrent.Executors;
            import java.util.concurrent.ExecutorService;
            import java.util.concurrent.TimeUnit;
            
            public class TestClass {
                public void processItems() {
                    ExecutorService executor = Executors.newFixedThreadPool(
                        Runtime.getRuntime().availableProcessors());
                    try {
                        executor.submit(() -> {
                            System.out.println("Task");
                        });
                    } finally {
                        executor.shutdown();
                        try {
                            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                                executor.shutdownNow();
                            }
                        } catch (InterruptedException e) {
                            executor.shutdownNow();
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        // Should have no high-severity issues for proper usage
        assertTrue(issues.stream().noneMatch(issue -> 
            issue.getSeverity() == ConcurrencyIssue.IssueSeverity.HIGH));
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
