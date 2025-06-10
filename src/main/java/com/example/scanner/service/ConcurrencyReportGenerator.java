package com.example.scanner.service;

import com.example.scanner.model.*;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates detailed HTML reports for Java concurrency analysis.
 * Implements the Concurrency Report Generation partition from the activity diagram.
 */
@Service
public class ConcurrencyReportGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyReportGenerator.class);
    
    private final TemplateProcessor templateProcessor;
    
    public ConcurrencyReportGenerator(TemplateProcessor templateProcessor) {
        this.templateProcessor = templateProcessor;
    }
    
    /**
     * Generates a comprehensive HTML report with concurrency analysis results.
     */
    public void generateHtmlReport(List<AnalysisResult> results, String outputPath) throws IOException {
        logger.info("Generating concurrency HTML report: {}", outputPath);
        
        Context context = new Context();
        
        // Generate executive summary
        ReportSummary summary = generateExecutiveSummary(results);
        context.setVariable("summary", summary);
        
        // Create class-level reports
        List<ClassReport> classReports = createClassLevelReports(results);
        context.setVariable("classReports", classReports);
        
        // Compile all issues with line-by-line details
        List<ConcurrencyIssue> allIssues = results.stream()
            .flatMap(r -> r.getIssues().stream())
            .collect(Collectors.toList());
        context.setVariable("allIssues", allIssues);
        
        // Compile all recommendations
        List<ConcurrencyRecommendation> allRecommendations = results.stream()
            .flatMap(r -> r.getRecommendations().stream())
            .collect(Collectors.toList());
        context.setVariable("recommendations", allRecommendations);
        
        // Add metadata
        context.setVariable("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        context.setVariable("totalFiles", results.size());
        
        // Process template and write to file
        String htmlContent = templateProcessor.process("concurrency-report", context);
        
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(htmlContent);
        }
        
        logger.info("HTML report generated successfully: {}", outputPath);
    }
    
    /**
     * Generates an empty report when no Java files are found.
     */
    public void generateEmptyReport(String outputPath) throws IOException {
        Context context = new Context();
        context.setVariable("isEmpty", true);
        context.setVariable("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        String htmlContent = templateProcessor.process("empty-report", context);
        
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(htmlContent);
        }
        
        logger.info("Empty HTML report generated: {}", outputPath);
    }
    
    /**
     * Generates an error report when analysis fails.
     */
    public void generateErrorReport(Exception error, String outputPath) throws IOException {
        Context context = new Context();
        context.setVariable("hasError", true);
        context.setVariable("errorMessage", error.getMessage());
        context.setVariable("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        String htmlContent = templateProcessor.process("error-report", context);
        
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(htmlContent);
        }
        
        logger.info("Error HTML report generated: {}", outputPath);
    }
    
    /**
     * Creates executive summary with total classes analyzed and thread safety issues.
     */
    private ReportSummary generateExecutiveSummary(List<AnalysisResult> results) {
        ReportSummary summary = new ReportSummary();
        
        summary.setTotalFiles(results.size());
        summary.setTotalClasses(results.stream().mapToInt(AnalysisResult::getAnalyzedClasses).sum());
        summary.setTotalIssues(results.stream().mapToInt(r -> r.getIssues().size()).sum());
        summary.setTotalRecommendations(results.stream().mapToInt(r -> r.getRecommendations().size()).sum());
        
        long threadSafeClasses = results.stream().filter(AnalysisResult::isThreadSafe).count();
        long problematicClasses = results.size() - threadSafeClasses;
        
        summary.setThreadSafeClasses((int) threadSafeClasses);
        summary.setProblematicClasses((int) problematicClasses);
        
        // Calculate issue distribution by severity
        Map<IssueSeverity, Long> severityDistribution = results.stream()
            .flatMap(r -> r.getIssues().stream())
            .collect(Collectors.groupingBy(ConcurrencyIssue::getSeverity, Collectors.counting()));
        
        summary.setCriticalIssues(severityDistribution.getOrDefault(IssueSeverity.CRITICAL, 0L).intValue());
        summary.setHighSeverityIssues(severityDistribution.getOrDefault(IssueSeverity.HIGH, 0L).intValue());
        summary.setMediumSeverityIssues(severityDistribution.getOrDefault(IssueSeverity.MEDIUM, 0L).intValue());
        summary.setLowSeverityIssues(severityDistribution.getOrDefault(IssueSeverity.LOW, 0L).intValue());
        
        return summary;
    }
    
    /**
     * Creates class-level reports showing thread-safe vs problematic classes.
     */
    private List<ClassReport> createClassLevelReports(List<AnalysisResult> results) {
        return results.stream()
            .map(this::createClassReport)
            .collect(Collectors.toList());
    }
    
    private ClassReport createClassReport(AnalysisResult result) {
        ClassReport report = new ClassReport();
        report.setFileName(result.getFilePath().getFileName().toString());
        report.setFilePath(result.getFilePath().toString());
        report.setThreadSafe(result.isThreadSafe());
        report.setIssueCount(result.getIssues().size());
        report.setRecommendationCount(result.getRecommendations().size());
        report.setHasErrors(result.isHasErrors());
        report.setErrorMessage(result.getErrorMessage());
        
        // Categorize issues by type
        Map<String, Long> issueTypes = result.getIssues().stream()
            .collect(Collectors.groupingBy(ConcurrencyIssue::getType, Collectors.counting()));
        report.setIssueTypeDistribution(issueTypes);
        
        return report;
    }
    
    /**
     * Summary information for the executive report section.
     */
    public static class ReportSummary {
        private int totalFiles;
        private int totalClasses;
        private int totalIssues;
        private int totalRecommendations;
        private int threadSafeClasses;
        private int problematicClasses;
        private int criticalIssues;
        private int highSeverityIssues;
        private int mediumSeverityIssues;
        private int lowSeverityIssues;
        
        // Getters and Setters
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
        
        public int getTotalClasses() { return totalClasses; }
        public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }
        
        public int getTotalIssues() { return totalIssues; }
        public void setTotalIssues(int totalIssues) { this.totalIssues = totalIssues; }
        
        public int getTotalRecommendations() { return totalRecommendations; }
        public void setTotalRecommendations(int totalRecommendations) { this.totalRecommendations = totalRecommendations; }
        
        public int getThreadSafeClasses() { return threadSafeClasses; }
        public void setThreadSafeClasses(int threadSafeClasses) { this.threadSafeClasses = threadSafeClasses; }
        
        public int getProblematicClasses() { return problematicClasses; }
        public void setProblematicClasses(int problematicClasses) { this.problematicClasses = problematicClasses; }
        
        public int getCriticalIssues() { return criticalIssues; }
        public void setCriticalIssues(int criticalIssues) { this.criticalIssues = criticalIssues; }
        
        public int getHighSeverityIssues() { return highSeverityIssues; }
        public void setHighSeverityIssues(int highSeverityIssues) { this.highSeverityIssues = highSeverityIssues; }
        
        public int getMediumSeverityIssues() { return mediumSeverityIssues; }
        public void setMediumSeverityIssues(int mediumSeverityIssues) { this.mediumSeverityIssues = mediumSeverityIssues; }
        
        public int getLowSeverityIssues() { return lowSeverityIssues; }
        public void setLowSeverityIssues(int lowSeverityIssues) { this.lowSeverityIssues = lowSeverityIssues; }
    }
    
    /**
     * Report information for individual classes.
     */
    public static class ClassReport {
        private String fileName;
        private String filePath;
        private boolean threadSafe;
        private int issueCount;
        private int recommendationCount;
        private boolean hasErrors;
        private String errorMessage;
        private Map<String, Long> issueTypeDistribution;
        
        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public boolean isThreadSafe() { return threadSafe; }
        public void setThreadSafe(boolean threadSafe) { this.threadSafe = threadSafe; }
        
        public int getIssueCount() { return issueCount; }
        public void setIssueCount(int issueCount) { this.issueCount = issueCount; }
        
        public int getRecommendationCount() { return recommendationCount; }
        public void setRecommendationCount(int recommendationCount) { this.recommendationCount = recommendationCount; }
        
        public boolean isHasErrors() { return hasErrors; }
        public void setHasErrors(boolean hasErrors) { this.hasErrors = hasErrors; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Map<String, Long> getIssueTypeDistribution() { return issueTypeDistribution; }
        public void setIssueTypeDistribution(Map<String, Long> issueTypeDistribution) { 
            this.issueTypeDistribution = issueTypeDistribution; 
        }
    }
}
