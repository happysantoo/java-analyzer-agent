package com.example.scanner.model;

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents the complete analysis result for a Java file.
 */
public class AnalysisResult {
    private Path filePath;
    private String directoryPath;
    private String reportFilePath;
    private ScanStatistics scanStatistics;
    private int analyzedClasses;
    private boolean threadSafe;
    private boolean hasErrors;
    private String errorMessage;
    private List<ConcurrencyIssue> issues = new ArrayList<>();
    private List<ConcurrencyRecommendation> recommendations = new ArrayList<>();
    
    // Getters and Setters
    public Path getFilePath() { return filePath; }
    public void setFilePath(Path filePath) { this.filePath = filePath; }
    
    public String getDirectoryPath() { return directoryPath; }
    public void setDirectoryPath(String directoryPath) { this.directoryPath = directoryPath; }
    
    public String getReportFilePath() { return reportFilePath; }
    public void setReportFilePath(String reportFilePath) { this.reportFilePath = reportFilePath; }
    
    public ScanStatistics getScanStatistics() { return scanStatistics; }
    public void setScanStatistics(ScanStatistics scanStatistics) { this.scanStatistics = scanStatistics; }
    
    public int getAnalyzedClasses() { return analyzedClasses; }
    public void setAnalyzedClasses(int analyzedClasses) { this.analyzedClasses = analyzedClasses; }
    
    public boolean isThreadSafe() { return threadSafe; }
    public void setThreadSafe(boolean threadSafe) { this.threadSafe = threadSafe; }
    
    public boolean isHasErrors() { return hasErrors; }
    public void setHasErrors(boolean hasErrors) { this.hasErrors = hasErrors; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public List<ConcurrencyIssue> getIssues() { return issues; }
    public void setIssues(List<ConcurrencyIssue> issues) { this.issues = issues; }
    
    // Alias method for compatibility
    public List<ConcurrencyIssue> getConcurrencyIssues() { return issues; }
    
    public List<ConcurrencyRecommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<ConcurrencyRecommendation> recommendations) { 
        this.recommendations = recommendations; 
    }
}
