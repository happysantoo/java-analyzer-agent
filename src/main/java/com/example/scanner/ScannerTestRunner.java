package com.example.scanner;

import com.example.scanner.agent.JavaScannerAgent;
import com.example.scanner.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ScannerTestRunner implements CommandLineRunner {
    
    @Autowired
    private JavaScannerAgent scannerAgent;
    
    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && "test".equals(args[0])) {
            runTest();
        }
    }
    
    private void runTest() {
        System.out.println("=== Running Java Concurrency Scanner Test ===");
        
        // Test with sample files
        String testDirectory = System.getProperty("user.dir") + "/test-samples";
        System.out.println("Scanning directory: " + testDirectory);
        
        try {
            AnalysisResult result = scannerAgent.analyzeJavaCode(testDirectory);
            
            System.out.println("\n=== Test Results ===");
            System.out.println("Directory: " + result.getDirectoryPath());
            System.out.println("Total Issues: " + result.getConcurrencyIssues().size());
            System.out.println("Files Scanned: " + result.getScanStatistics().getTotalFiles());
            System.out.println("Lines Analyzed: " + result.getScanStatistics().getTotalLines());
            System.out.println("Scan Duration: " + result.getScanStatistics().getScanDurationMs() + "ms");
            
            if (!result.getConcurrencyIssues().isEmpty()) {
                System.out.println("\n=== Issues Found ===");
                result.getConcurrencyIssues().forEach(issue -> {
                    System.out.println("- " + issue.getTitle() + " (Line " + issue.getLineNumber() + ")");
                    System.out.println("  Severity: " + issue.getSeverity());
                    System.out.println("  File: " + issue.getFilePath());
                    System.out.println();
                });
            } else {
                System.out.println("No concurrency issues found!");
            }
            
            if (!result.getRecommendations().isEmpty()) {
                System.out.println("\n=== AI Recommendations ===");
                result.getRecommendations().forEach(rec -> {
                    System.out.println("- " + rec.getTitle());
                    System.out.println("  Priority: " + rec.getPriority());
                    System.out.println("  " + rec.getDescription());
                    System.out.println();
                });
            }
            
            System.out.println("Report file: " + result.getReportFilePath());
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
