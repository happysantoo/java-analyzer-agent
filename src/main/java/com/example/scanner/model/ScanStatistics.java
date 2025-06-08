package com.example.scanner.model;

/**
 * Statistics for the scanning operation.
 */
public class ScanStatistics {
    private int totalJavaFiles;
    private int totalIssuesFound;
    private int totalRecommendations;
    private int threadSafeClasses;
    private int problematicClasses;
    private int totalLines;
    private long scanDurationMs;
    
    // Getters and Setters
    public int getTotalJavaFiles() { return totalJavaFiles; }
    public void setTotalJavaFiles(int totalJavaFiles) { this.totalJavaFiles = totalJavaFiles; }
    
    // For backward compatibility
    public int getTotalFiles() { return totalJavaFiles; }
    public void setTotalFiles(int totalFiles) { this.totalJavaFiles = totalFiles; }
    
    public int getTotalIssuesFound() { return totalIssuesFound; }
    public void setTotalIssuesFound(int totalIssuesFound) { this.totalIssuesFound = totalIssuesFound; }
    
    public int getTotalRecommendations() { return totalRecommendations; }
    public void setTotalRecommendations(int totalRecommendations) { this.totalRecommendations = totalRecommendations; }
    
    public int getThreadSafeClasses() { return threadSafeClasses; }
    public void setThreadSafeClasses(int threadSafeClasses) { this.threadSafeClasses = threadSafeClasses; }
    
    public int getProblematicClasses() { return problematicClasses; }
    public void setProblematicClasses(int problematicClasses) { this.problematicClasses = problematicClasses; }
    
    public int getTotalLines() { return totalLines; }
    public void setTotalLines(int totalLines) { this.totalLines = totalLines; }
    
    public long getScanDurationMs() { return scanDurationMs; }
    public void setScanDurationMs(long scanDurationMs) { this.scanDurationMs = scanDurationMs; }
    
    @Override
    public String toString() {
        return String.format(
            "ScanStatistics{files=%d, issues=%d, recommendations=%d, threadSafe=%d, problematic=%d, totalLines=%d, durationMs=%d}",
            totalJavaFiles, totalIssuesFound, totalRecommendations, threadSafeClasses, problematicClasses, totalLines, scanDurationMs
        );
    }
}
