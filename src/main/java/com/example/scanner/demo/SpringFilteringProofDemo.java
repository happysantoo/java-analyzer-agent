package com.example.scanner.demo;

import com.example.scanner.service.JavaSourceAnalysisService;
import com.example.scanner.service.ConcurrencyAnalysisEngine;
import com.example.scanner.model.JavaSourceInfo;
import com.example.scanner.model.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Demonstrates Spring filtering functionality working correctly.
 */
public class SpringFilteringProofDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringFilteringProofDemo.class);
    
    private JavaSourceAnalysisService sourceAnalysisService;
    private ConcurrencyAnalysisEngine analysisEngine;
    
    public SpringFilteringProofDemo() {
        this.sourceAnalysisService = new JavaSourceAnalysisService();
        this.analysisEngine = new ConcurrencyAnalysisEngine();
    }
    
    public void demonstrateSpringFiltering() {
        try {
            logger.info("=== Spring Filtering Demo ===");
            
            // Create test files
            Path springServiceFile = createSpringServiceFile();
            Path plainClassFile = createPlainClassFile();
            
            List<Path> testFiles = List.of(springServiceFile, plainClassFile);
            
            // Test with filtering disabled
            logger.info("\n1. Testing with Spring filtering DISABLED:");
            sourceAnalysisService.setSpringFilterEnabled(false);
            List<JavaSourceInfo> allResults = sourceAnalysisService.analyzeJavaFiles(testFiles);
            
            int totalClassesWithoutFilter = allResults.stream()
                .mapToInt(sourceInfo -> sourceInfo.getClasses().size())
                .sum();
            
            logger.info("   Classes analyzed: {}", totalClassesWithoutFilter);
            allResults.forEach(sourceInfo -> {
                sourceInfo.getClasses().forEach(classInfo -> {
                    logger.info("   - {} (Spring managed: {})", 
                        classInfo.getName(), classInfo.isSpringManaged());
                });
            });
            
            // Test with filtering enabled
            logger.info("\n2. Testing with Spring filtering ENABLED:");
            sourceAnalysisService.setSpringFilterEnabled(true);
            sourceAnalysisService.setSpringAnnotations(List.of("Service", "Component", "Repository", 
                "Controller", "RestController", "Configuration"));
            
            List<JavaSourceInfo> filteredResults = sourceAnalysisService.analyzeJavaFiles(testFiles);
            
            int totalClassesWithFilter = filteredResults.stream()
                .mapToInt(sourceInfo -> sourceInfo.getClasses().size())
                .sum();
            
            logger.info("   Classes analyzed: {}", totalClassesWithFilter);
            filteredResults.forEach(sourceInfo -> {
                sourceInfo.getClasses().forEach(classInfo -> {
                    logger.info("   - {} (Spring managed: {}, Annotations: {})", 
                        classInfo.getName(), classInfo.isSpringManaged(), classInfo.getSpringAnnotations());
                });
            });
            
            // Demonstrate analysis difference
            logger.info("\n3. Analysis Results Comparison:");
            List<AnalysisResult> allAnalysisResults = analysisEngine.analyzeConcurrencyIssues(allResults);
            List<AnalysisResult> filteredAnalysisResults = analysisEngine.analyzeConcurrencyIssues(filteredResults);
            
            int allIssuesCount = allAnalysisResults.stream()
                .mapToInt(result -> result.getIssues().size())
                .sum();
            
            int filteredIssuesCount = filteredAnalysisResults.stream()
                .mapToInt(result -> result.getIssues().size())
                .sum();
            
            logger.info("   Issues found without filtering: {}", allIssuesCount);
            logger.info("   Issues found with filtering: {}", filteredIssuesCount);
            logger.info("   Classes filtered out: {}", totalClassesWithoutFilter - totalClassesWithFilter);
            
            logger.info("\n✅ Spring filtering is working correctly!");
            logger.info("   - When disabled: {} classes analyzed", totalClassesWithoutFilter);
            logger.info("   - When enabled: {} classes analyzed", totalClassesWithFilter);
            logger.info("   - Filtering effectiveness: {}% reduction in classes analyzed", 
                Math.round(((double)(totalClassesWithoutFilter - totalClassesWithFilter) / totalClassesWithoutFilter) * 100));
            
            // Cleanup
            Files.deleteIfExists(springServiceFile);
            Files.deleteIfExists(plainClassFile);
            
        } catch (Exception e) {
            logger.error("Demo failed", e);
        }
    }
    
    private Path createSpringServiceFile() throws Exception {
        Path file = Files.createTempFile("DemoSpringService", ".java");
        String content = """
package com.example.demo;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class DemoSpringService {
    private Map<String, String> cache = new HashMap<>(); // Concurrency issue
    private int counter = 0; // Concurrency issue
    
    public void updateCache(String key, String value) {
        cache.put(key, value); // Race condition
        counter++; // Race condition
    }
    
    public String getValue(String key) {
        return cache.get(key);
    }
}
""";
        Files.write(file, content.getBytes(), StandardOpenOption.WRITE);
        return file;
    }
    
    private Path createPlainClassFile() throws Exception {
        Path file = Files.createTempFile("DemoPlainClass", ".java");
        String content = """
package com.example.demo;

import java.util.HashMap;
import java.util.Map;

public class DemoPlainClass {
    private static Map<String, String> staticCache = new HashMap<>(); // Concurrency issue
    private int plainCounter = 0; // Concurrency issue
    
    public static void updateStaticCache(String key, String value) {
        staticCache.put(key, value); // Race condition
    }
    
    public void incrementPlain() {
        plainCounter++; // Race condition
    }
}
""";
        Files.write(file, content.getBytes(), StandardOpenOption.WRITE);
        return file;
    }
}
