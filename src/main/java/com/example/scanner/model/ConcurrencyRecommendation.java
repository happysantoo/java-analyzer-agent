package com.example.scanner.model;

/**
 * Represents an AI-generated recommendation for fixing concurrency issues.
 */
public class ConcurrencyRecommendation {
    private String title;
    private String description;
    private RecommendationPriority priority;
    private RecommendationEffort effort;
    private String alternativeApproach;
    private String performanceImpact;
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public RecommendationPriority getPriority() { return priority; }
    public void setPriority(RecommendationPriority priority) { this.priority = priority; }
    
    public RecommendationEffort getEffort() { return effort; }
    public void setEffort(RecommendationEffort effort) { this.effort = effort; }
    
    public String getAlternativeApproach() { return alternativeApproach; }
    public void setAlternativeApproach(String alternativeApproach) { this.alternativeApproach = alternativeApproach; }
    
    public String getPerformanceImpact() { return performanceImpact; }
    public void setPerformanceImpact(String performanceImpact) { this.performanceImpact = performanceImpact; }
}
