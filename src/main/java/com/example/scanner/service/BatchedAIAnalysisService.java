package com.example.scanner.service;

import com.example.scanner.model.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI service that batches multiple files/issues into single AI requests 
 * to dramatically reduce API calls and improve performance.
 * 
 * Potential reduction: 80-95% fewer AI calls for large projects.
 */
@Service
public class BatchedAIAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchedAIAnalysisService.class);
    
    @Autowired
    private ChatClient chatClient;
    
    // Configuration for batching strategy
    private static final int MAX_BATCH_SIZE = 10; // Files per batch
    private static final int MAX_ISSUES_PER_BATCH = 50; // Total issues per batch
    private static final int MAX_PROMPT_LENGTH = 8000; // Token limit consideration
    
    /**
     * Processes multiple analysis results in batches to reduce AI calls.
     * 
     * @param results List of analysis results from traditional analyzers
     * @return Updated results with AI recommendations
     */
    public List<AnalysisResult> generateBatchedRecommendations(List<AnalysisResult> results) {
        logger.info("Starting batched AI analysis for {} files", results.size());
        
        // Filter results that need AI recommendations (have issues)
        List<AnalysisResult> resultsWithIssues = results.stream()
            .filter(result -> !result.getIssues().isEmpty())
            .collect(Collectors.toList());
        
        if (resultsWithIssues.isEmpty()) {
            logger.info("No files with issues found, skipping AI analysis");
            return results;
        }
        
        // Create optimized batches
        List<BatchGroup> batches = createOptimalBatches(resultsWithIssues);
        logger.info("Created {} batches from {} files with issues", batches.size(), resultsWithIssues.size());
        
        // Process each batch
        for (BatchGroup batch : batches) {
            try {
                processBatch(batch);
            } catch (Exception e) {
                logger.error("Failed to process batch with {} files", batch.results.size(), e);
                // Apply fallback recommendations for this batch
                applyFallbackRecommendations(batch.results);
            }
        }
        
        logger.info("Completed batched AI analysis. Total AI calls: {}", batches.size());
        return results;
    }
    
    /**
     * Creates optimal batches considering file count, issue count, and prompt size limits.
     */
    private List<BatchGroup> createOptimalBatches(List<AnalysisResult> results) {
        List<BatchGroup> batches = new ArrayList<>();
        BatchGroup currentBatch = new BatchGroup();
        
        for (AnalysisResult result : results) {
            // Check if adding this result would exceed batch limits
            if (shouldStartNewBatch(currentBatch, result)) {
                if (!currentBatch.results.isEmpty()) {
                    batches.add(currentBatch);
                }
                currentBatch = new BatchGroup();
            }
            
            currentBatch.results.add(result);
            currentBatch.totalIssues += result.getIssues().size();
            currentBatch.estimatedPromptLength += estimatePromptContribution(result);
        }
        
        // Add the last batch
        if (!currentBatch.results.isEmpty()) {
            batches.add(currentBatch);
        }
        
        return batches;
    }
    
    /**
     * Determines if a new batch should be started based on various limits.
     */
    private boolean shouldStartNewBatch(BatchGroup currentBatch, AnalysisResult newResult) {
        if (currentBatch.results.isEmpty()) {
            return false; // First result in batch
        }
        
        // Check file count limit
        if (currentBatch.results.size() >= MAX_BATCH_SIZE) {
            return true;
        }
        
        // Check total issues limit
        if (currentBatch.totalIssues + newResult.getIssues().size() > MAX_ISSUES_PER_BATCH) {
            return true;
        }
        
        // Check estimated prompt length
        int newPromptContribution = estimatePromptContribution(newResult);
        if (currentBatch.estimatedPromptLength + newPromptContribution > MAX_PROMPT_LENGTH) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Estimates the prompt length contribution of a single result.
     */
    private int estimatePromptContribution(AnalysisResult result) {
        // Rough estimation: filename + class names + issues
        int baseLength = result.getFilePath().toString().length();
        baseLength += result.getIssues().size() * 100; // ~100 chars per issue description
        return baseLength;
    }
    
    /**
     * Processes a single batch with AI analysis.
     */
    private void processBatch(BatchGroup batch) {
        try {
            String batchPrompt = buildBatchPrompt(batch);
            
            PromptTemplate promptTemplate = new PromptTemplate(batchPrompt);
            Prompt chatPrompt = promptTemplate.create();
            
            String aiResponse = chatClient.prompt(chatPrompt).call().content();
            
            // Parse and distribute recommendations back to individual results
            distributeBatchRecommendations(batch, aiResponse);
            
        } catch (Exception e) {
            logger.error("AI batch processing failed for {} files", batch.results.size(), e);
            throw e; // Will trigger fallback in caller
        }
    }
    
    /**
     * Builds a comprehensive batch prompt for multiple files and issues.
     */
    private String buildBatchPrompt(BatchGroup batch) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a Java concurrency expert analyzing multiple files in a batch. ");
        prompt.append("Provide specific, actionable recommendations for each file's concurrency issues.\n\n");
        
        prompt.append(String.format("BATCH ANALYSIS (%d files, %d total issues)\n", 
            batch.results.size(), batch.totalIssues));
        prompt.append("=".repeat(50)).append("\n\n");
        
        // Add each file's context and issues
        for (int i = 0; i < batch.results.size(); i++) {
            AnalysisResult result = batch.results.get(i);
            prompt.append(String.format("FILE %d: %s\n", i + 1, result.getFilePath().getFileName()));
            prompt.append("-".repeat(30)).append("\n");
            
            if (!result.getIssues().isEmpty()) {
                prompt.append("Concurrency Issues:\n");
                for (int j = 0; j < result.getIssues().size(); j++) {
                    ConcurrencyIssue issue = result.getIssues().get(j);
                    prompt.append(String.format("  %d.%d. %s (Line %d) - %s: %s\n", 
                        i + 1, j + 1, issue.getType(), issue.getLineNumber(), 
                        issue.getSeverity(), issue.getDescription()));
                }
            }
            prompt.append("\n");
        }
        
        prompt.append("REQUIREMENTS:\n");
        prompt.append("- Provide recommendations for each file (FILE 1, FILE 2, etc.)\n");
        prompt.append("- Priority ranking (HIGH/MEDIUM/LOW) for each issue\n");
        prompt.append("- Specific code changes or patterns\n");
        prompt.append("- Estimated effort (SMALL/MEDIUM/LARGE)\n");
        prompt.append("- Keep recommendations concise but actionable\n\n");
        
        prompt.append("Format: \nFILE X RECOMMENDATIONS:\n1. [issue] - [priority] - [solution] - [effort]\n");
        
        return prompt.toString();
    }
    
    /**
     * Parses batch AI response and distributes recommendations to individual results.
     */
    private void distributeBatchRecommendations(BatchGroup batch, String aiResponse) {
        // Parse the batch response and map recommendations back to files
        String[] sections = aiResponse.split("FILE \\d+ RECOMMENDATIONS:");
        
        for (int i = 0; i < batch.results.size() && i + 1 < sections.length; i++) {
            AnalysisResult result = batch.results.get(i);
            String fileRecommendations = sections[i + 1].trim();
            
            List<ConcurrencyRecommendation> recommendations = parseSectionRecommendations(fileRecommendations);
            result.setRecommendations(recommendations);
        }
    }
    
    /**
     * Parses recommendations for a single file section.
     */
    private List<ConcurrencyRecommendation> parseSectionRecommendations(String section) {
        List<ConcurrencyRecommendation> recommendations = new ArrayList<>();
        
        String[] lines = section.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.matches("\\d+\\..*")) {
                ConcurrencyRecommendation rec = parseRecommendationLine(line);
                if (rec != null) {
                    recommendations.add(rec);
                }
            }
        }
        
        return recommendations;
    }
    
    /**
     * Parses a single recommendation line from AI response.
     */
    private ConcurrencyRecommendation parseRecommendationLine(String line) {
        try {
            ConcurrencyRecommendation rec = new ConcurrencyRecommendation();
            rec.setDescription(line);
            
            // Extract priority and effort from structured format
            if (line.toLowerCase().contains("high")) {
                rec.setPriority(RecommendationPriority.HIGH);
            } else if (line.toLowerCase().contains("low")) {
                rec.setPriority(RecommendationPriority.LOW);
            } else {
                rec.setPriority(RecommendationPriority.MEDIUM);
            }
            
            if (line.toLowerCase().contains("large")) {
                rec.setEffort(RecommendationEffort.LARGE);
            } else if (line.toLowerCase().contains("small")) {
                rec.setEffort(RecommendationEffort.SMALL);
            } else {
                rec.setEffort(RecommendationEffort.MEDIUM);
            }
            
            return rec;
        } catch (Exception e) {
            logger.warn("Failed to parse recommendation line: {}", line);
            return null;
        }
    }
    
    /**
     * Applies fallback recommendations when AI batch processing fails.
     */
    private void applyFallbackRecommendations(List<AnalysisResult> results) {
        for (AnalysisResult result : results) {
            List<ConcurrencyRecommendation> fallbackRecs = result.getIssues().stream()
                .map(issue -> {
                    ConcurrencyRecommendation rec = new ConcurrencyRecommendation();
                    rec.setDescription("Review and fix: " + issue.getDescription());
                    rec.setPriority(RecommendationPriority.MEDIUM);
                    rec.setEffort(RecommendationEffort.MEDIUM);
                    return rec;
                })
                .collect(Collectors.toList());
            
            result.setRecommendations(fallbackRecs);
        }
    }
    
    /**
     * Represents a group of analysis results to be processed in a single AI batch.
     */
    private static class BatchGroup {
        List<AnalysisResult> results = new ArrayList<>();
        int totalIssues = 0;
        int estimatedPromptLength = 0;
    }
}
