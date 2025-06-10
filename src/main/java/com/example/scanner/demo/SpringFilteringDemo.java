package com.example.scanner.demo;

import com.example.scanner.service.JavaSourceAnalysisService;
import com.example.scanner.model.JavaSourceInfo;
import com.example.scanner.model.ClassInfo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Demonstration of Spring annotation filtering functionality
 */
public class SpringFilteringDemo {
    
    public static void main(String[] args) {
        JavaSourceAnalysisService service = new JavaSourceAnalysisService();
        Path testFile = Paths.get("test-samples/SpringAnnotatedClasses.java");
        
        System.out.println("=== Spring Annotation Filtering Demo ===\n");
        
        // Test with Spring filtering DISABLED (default)
        System.out.println("1. Spring Filtering DISABLED (analyzes all classes):");
        service.setSpringFilterEnabled(false);
        demonstrateAnalysis(service, testFile, "DISABLED");
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test with Spring filtering ENABLED
        System.out.println("2. Spring Filtering ENABLED (analyzes only @Service, @Component, @Repository):");
        service.setSpringFilterEnabled(true);
        demonstrateAnalysis(service, testFile, "ENABLED");
        
        System.out.println("\n=== Demo Complete ===");
    }
    
    private static void demonstrateAnalysis(JavaSourceAnalysisService service, Path testFile, String mode) {
        try {
            List<JavaSourceInfo> results = service.analyzeJavaFiles(List.of(testFile));
            
            if (results.isEmpty()) {
                System.out.println("No results found.");
                return;
            }
            
            JavaSourceInfo sourceInfo = results.get(0);
            System.out.println("File: " + sourceInfo.getFilePath());
            System.out.println("Classes found: " + sourceInfo.getClasses().size());
            System.out.println();
            
            for (ClassInfo classInfo : sourceInfo.getClasses()) {
                System.out.println("Class: " + classInfo.getName());
                System.out.println("  - Is Spring Managed: " + classInfo.isSpringManaged());
                if (!classInfo.getSpringAnnotations().isEmpty()) {
                    System.out.println("  - Spring Annotations: " + classInfo.getSpringAnnotations());
                }
                System.out.println("  - Methods: " + classInfo.getMethods().size());
                System.out.println("  - Fields: " + classInfo.getFields().size());
                System.out.println();
            }
            
            // Summary
            long springManagedCount = sourceInfo.getClasses().stream()
                    .mapToLong(c -> c.isSpringManaged() ? 1 : 0)
                    .sum();
            
            System.out.println("Summary for " + mode + " mode:");
            System.out.println("  - Total classes analyzed: " + sourceInfo.getClasses().size());
            System.out.println("  - Spring-managed classes: " + springManagedCount);
            System.out.println("  - Non-Spring classes: " + (sourceInfo.getClasses().size() - springManagedCount));
            
        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
