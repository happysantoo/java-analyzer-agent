package com.example.scanner.agent;

import com.example.scanner.config.ScannerConfiguration;
import com.example.scanner.service.JavaFileDiscoveryService;
import com.example.scanner.service.JavaSourceAnalysisService;
import com.example.scanner.service.ConcurrencyAnalysisEngine;
import com.example.scanner.service.ConcurrencyReportGenerator;
import com.example.scanner.model.AnalysisResult;
import com.example.scanner.model.ScanStatistics;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main orchestrator agent following Anthropic's efficient agent design principles.
 * Coordinates the entire Java concurrency analysis workflow.
 */
@Component
public class JavaScannerAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(JavaScannerAgent.class);
    
    @Autowired
    private ScannerConfiguration configuration;
    
    @Autowired
    private JavaFileDiscoveryService fileDiscoveryService;
    
    @Autowired
    private JavaSourceAnalysisService sourceAnalysisService;
    
    @Autowired
    private ConcurrencyAnalysisEngine analysisEngine;
    
    @Autowired
    private ConcurrencyReportGenerator reportGenerator;
    
    /**
     * Executes the complete concurrency analysis workflow as defined in the activity diagram.
     */
    public void executeConcurrencyAnalysis(String scanPath, String outputPath, String configPath) {
        logger.info("Starting Java concurrency analysis for path: {}", scanPath);
        
        try {
            // Input Validation (Activity Diagram Step 1)
            if (!validateInputs(scanPath, outputPath, configPath)) {
                return;
            }
            
            // Load Configuration
            configuration.loadConfiguration(configPath);
            logger.info("Loaded concurrency analysis configuration");
            
            // Java File Discovery (Activity Diagram Step 2)
            List<Path> javaFiles = fileDiscoveryService.discoverJavaFiles(Paths.get(scanPath));
            if (javaFiles.isEmpty()) {
                logger.warn("No Java files found in path: {}", scanPath);
                generateEmptyReport(outputPath);
                return;
            }
            
            logger.info("Discovered {} Java files for analysis", javaFiles.size());
            
            // Java Source Analysis (Activity Diagram Step 3)
            var sourceAnalysisResults = sourceAnalysisService.analyzeJavaFiles(javaFiles);
            logger.info("Completed source analysis for {} files", sourceAnalysisResults.size());
            
            // Concurrency Analysis Engine (Activity Diagram Step 4)
            List<AnalysisResult> concurrencyResults = analysisEngine.analyzeConcurrencyIssues(sourceAnalysisResults);
            logger.info("Completed concurrency analysis, found {} potential issues", 
                       concurrencyResults.stream().mapToInt(r -> r.getIssues().size()).sum());
            
            // Concurrency Report Generation (Activity Diagram Step 5)
            reportGenerator.generateHtmlReport(concurrencyResults, outputPath);
            
            // Display Final Statistics (Activity Diagram Final Step)
            displayFinalStatistics(javaFiles.size(), concurrencyResults);
            
            logger.info("Java concurrency analysis completed successfully. Report saved to: {}", outputPath);
            
        } catch (Exception e) {
            logger.error("Critical error during concurrency analysis", e);
            handleCriticalError(e, outputPath);
        }
    }
    
    private boolean validateInputs(String scanPath, String outputPath, String configPath) {
        Path scanDir = Paths.get(scanPath);
        if (!scanDir.toFile().exists() || !scanDir.toFile().isDirectory()) {
            logger.error("Invalid scan path: {} (does not exist or is not a directory)", scanPath);
            System.err.println("Error: Invalid Java project path - " + scanPath);
            return false;
        }
        
        // Validate output path is writable
        Path outputDir = Paths.get(outputPath).getParent();
        if (outputDir != null && !outputDir.toFile().exists()) {
            if (!outputDir.toFile().mkdirs()) {
                logger.error("Cannot create output directory: {}", outputDir);
                System.err.println("Error: Cannot create output directory - " + outputDir);
                return false;
            }
        }
        
        return true;
    }
    
    private void generateEmptyReport(String outputPath) {
        try {
            reportGenerator.generateEmptyReport(outputPath);
            logger.info("Generated empty concurrency report at: {}", outputPath);
        } catch (Exception e) {
            logger.error("Failed to generate empty report", e);
        }
    }
    
    private void handleCriticalError(Exception error, String outputPath) {
        try {
            reportGenerator.generateErrorReport(error, outputPath);
            logger.info("Generated error report at: {}", outputPath);
        } catch (Exception e) {
            logger.error("Failed to generate error report", e);
        }
    }
    
    private void displayFinalStatistics(int totalFiles, List<AnalysisResult> results) {
        ScanStatistics stats = new ScanStatistics();
        stats.setTotalJavaFiles(totalFiles);
        stats.setTotalIssuesFound(results.stream().mapToInt(r -> r.getIssues().size()).sum());
        stats.setTotalRecommendations(results.stream().mapToInt(r -> r.getRecommendations().size()).sum());
        stats.setThreadSafeClasses((int) results.stream().filter(r -> r.isThreadSafe()).count());
        stats.setProblematicClasses((int) results.stream().filter(r -> !r.isThreadSafe()).count());
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Java Concurrency Analysis - Final Statistics");
        System.out.println("=".repeat(60));
        System.out.printf("Java files scanned: %d%n", stats.getTotalJavaFiles());
        System.out.printf("Concurrency issues found: %d%n", stats.getTotalIssuesFound());
        System.out.printf("Thread safety recommendations: %d%n", stats.getTotalRecommendations());
        System.out.printf("Thread-safe classes: %d%n", stats.getThreadSafeClasses());
        System.out.printf("Classes with concurrency issues: %d%n", stats.getProblematicClasses());
        System.out.println("=".repeat(60));
        
        logger.info("Analysis statistics: {}", stats);
    }
    
    /**
     * Simplified analysis method for testing purposes.
     * Analyzes Java code in the specified directory and returns a single result.
     */
    public AnalysisResult analyzeJavaCode(String scanPath) {
        logger.info("Starting simplified Java concurrency analysis for path: {}", scanPath);
        
        try {
            // Java File Discovery
            List<Path> javaFiles = fileDiscoveryService.discoverJavaFiles(Paths.get(scanPath));
            if (javaFiles.isEmpty()) {
                logger.warn("No Java files found in path: {}", scanPath);
                return createEmptyAnalysisResult(scanPath);
            }
            
            // Java Source Analysis
            var sourceAnalysisResults = sourceAnalysisService.analyzeJavaFiles(javaFiles);
            
            // Concurrency Analysis Engine
            List<AnalysisResult> concurrencyResults = analysisEngine.analyzeConcurrencyIssues(sourceAnalysisResults);
            
            // Combine results into a single AnalysisResult
            if (concurrencyResults.isEmpty()) {
                return createEmptyAnalysisResult(scanPath);
            }
            
            // For simplicity, return the first result but aggregate statistics
            AnalysisResult mainResult = concurrencyResults.get(0);
            mainResult.setDirectoryPath(scanPath);
            
            // Create aggregate statistics
            ScanStatistics stats = new ScanStatistics();
            stats.setTotalJavaFiles(javaFiles.size());
            stats.setTotalIssuesFound(concurrencyResults.stream().mapToInt(r -> r.getIssues().size()).sum());
            stats.setTotalRecommendations(concurrencyResults.stream().mapToInt(r -> r.getRecommendations().size()).sum());
            stats.setThreadSafeClasses((int) concurrencyResults.stream().filter(r -> r.isThreadSafe()).count());
            stats.setProblematicClasses((int) concurrencyResults.stream().filter(r -> !r.isThreadSafe()).count());
            
            mainResult.setScanStatistics(stats);
            
            return mainResult;
            
        } catch (Exception e) {
            logger.error("Error during simplified concurrency analysis", e);
            return createErrorAnalysisResult(scanPath, e);
        }
    }
    
    private AnalysisResult createEmptyAnalysisResult(String scanPath) {
        AnalysisResult result = new AnalysisResult();
        result.setDirectoryPath(scanPath);
        result.setThreadSafe(true);
        result.setScanStatistics(new ScanStatistics());
        return result;
    }
    
    private AnalysisResult createErrorAnalysisResult(String scanPath, Exception error) {
        AnalysisResult result = new AnalysisResult();
        result.setDirectoryPath(scanPath);
        result.setThreadSafe(false);
        result.setScanStatistics(new ScanStatistics());
        return result;
    }
}
