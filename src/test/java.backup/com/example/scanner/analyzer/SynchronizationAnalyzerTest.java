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
class SynchronizationAnalyzerTest {
    
    private SynchronizationAnalyzer analyzer;
    private JavaParser javaParser;
    
    @BeforeEach
    void setUp() {
        analyzer = new SynchronizationAnalyzer();
        javaParser = new JavaParser();
    }
    
    @Test
    void testDetectPotentialDeadlock() {
        String code = """
            public class TestClass {
                private final Object lock1 = new Object();
                private final Object lock2 = new Object();
                
                public void methodA() {
                    synchronized (lock1) {
                        synchronized (lock2) {
                            // work
                        }
                    }
                }
                
                public void methodB() {
                    synchronized (lock2) {
                        synchronized (lock1) {
                            // work
                        }
                    }
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        assertTrue(issues.stream().anyMatch(issue -> 
            issue.getDescription().toLowerCase().contains("deadlock")));
    }
    
    @Test
    void testDetectNestedSynchronization() {
        String code = """
            public class TestClass {
                private final Object lock = new Object();
                
                public synchronized void outerMethod() {
                    synchronized (lock) {
                        // nested synchronization
                    }
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        assertTrue(issues.stream().anyMatch(issue -> 
            issue.getDescription().toLowerCase().contains("nested")));
    }
    
    @Test
    void testDetectSynchronizationOnThis() {
        String code = """
            public class TestClass {
                public void badMethod() {
                    synchronized (this) {
                        // synchronizing on this is not recommended
                    }
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        assertTrue(issues.stream().anyMatch(issue -> 
            issue.getDescription().toLowerCase().contains("synchronizing on this")));
    }
    
    @Test
    void testNoIssuesWithProperSynchronization() {
        String code = """
            public class TestClass {
                private final Object lock = new Object();
                
                public void safeMethod() {
                    synchronized (lock) {
                        // safe single lock usage
                    }
                }
            }
            """;
        
        CompilationUnit cu = javaParser.parse(code).getResult().orElseThrow();
        JavaSourceInfo sourceInfo = createSourceInfo("TestClass.java", code);
        
        List<ConcurrencyIssue> issues = analyzer.analyze(cu, sourceInfo);
        
        // Should have minimal or no high-severity issues
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
