package com.example.scanner.service;

import com.example.scanner.model.*;
import com.example.scanner.analyzer.*;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.nio.file.Paths;

/**
 * Core concurrency analysis engine implementing the Concurrency Analysis Engine partition
 * from the activity diagram. Uses Spring AI for intelligent recommendations.
 */
@Service
public class ConcurrencyAnalysisEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyAnalysisEngine.class);
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private ThreadSafetyAnalyzer threadSafetyAnalyzer;
    
    @Autowired
    private SynchronizationAnalyzer synchronizationAnalyzer;
    
    @Autowired
    private ConcurrentCollectionsAnalyzer concurrentCollectionsAnalyzer;
    
    @Autowired
    private ExecutorFrameworkAnalyzer executorFrameworkAnalyzer;
    
    @Autowired
    private AtomicOperationsAnalyzer atomicOperationsAnalyzer;
    
    @Autowired
    private LockUsageAnalyzer lockUsageAnalyzer;
    
    /**
     * Analyzes Java source files for concurrency issues using multiple specialized analyzers.
     */
    public List<AnalysisResult> analyzeConcurrencyIssues(List<JavaSourceInfo> sourceFiles) {
        logger.info("Starting concurrency analysis for {} Java files", sourceFiles.size());
        
        List<AnalysisResult> results = new ArrayList<>();
        
        for (JavaSourceInfo sourceInfo : sourceFiles) {
            try {
                AnalysisResult result = analyzeFile(sourceInfo);
                results.add(result);
                logger.debug("Completed analysis for: {}", sourceInfo.getFilePath());
            } catch (Exception e) {
                logger.error("Failed to analyze file: {}", sourceInfo.getFilePath(), e);
                // Create a partial result with error information
                AnalysisResult errorResult = createErrorResult(sourceInfo, e);
                results.add(errorResult);
            }
        }
        
        logger.info("Concurrency analysis completed. Total results: {}", results.size());
        return results;
    }
    
    /**
     * Analyzes a single Java file for concurrency issues.
     */
    private AnalysisResult analyzeFile(JavaSourceInfo sourceInfo) {
        AnalysisResult result = new AnalysisResult();
        result.setFilePath(Paths.get(sourceInfo.getFilePath()));
        result.setAnalyzedClasses(sourceInfo.getClasses().size());
        
        List<ConcurrencyIssue> allIssues = new ArrayList<>();
        
        // Run parallel analysis using fork-join pattern from activity diagram
        for (ClassInfo classInfo : sourceInfo.getClasses()) {
            
            // Check Thread Safety Issues (race conditions, shared mutable state)
            var threadSafetyIssues = threadSafetyAnalyzer.analyze(sourceInfo, classInfo);
            if (threadSafetyIssues != null) {
                allIssues.addAll(threadSafetyIssues);
            }
            
            // Check Synchronization Problems (deadlocks, synchronized block issues)
            var synchronizationIssues = synchronizationAnalyzer.analyze(sourceInfo, classInfo);
            if (synchronizationIssues != null) {
                allIssues.addAll(synchronizationIssues);
            }
            
            // Check Concurrent Collections Usage (ConcurrentHashMap vs HashMap)
            var collectionIssues = concurrentCollectionsAnalyzer.analyze(sourceInfo, classInfo);
            if (collectionIssues != null) {
                allIssues.addAll(collectionIssues);
            }
            
            // Check Executor Framework Usage (thread pool management)
            var executorIssues = executorFrameworkAnalyzer.analyze(sourceInfo, classInfo);
            if (executorIssues != null) {
                allIssues.addAll(executorIssues);
            }
            
            // Check Atomic Operations (AtomicInteger, AtomicReference usage)
            var atomicIssues = atomicOperationsAnalyzer.analyze(sourceInfo, classInfo);
            if (atomicIssues != null) {
                allIssues.addAll(atomicIssues);
            }
            
            // Check Lock Usage (ReentrantLock, ReadWriteLock patterns)
            var lockIssues = lockUsageAnalyzer.analyze(sourceInfo, classInfo);
            if (lockIssues != null) {
                allIssues.addAll(lockIssues);
            }
        }
        
        result.setIssues(allIssues);
        result.setThreadSafe(allIssues.stream().noneMatch(issue -> 
            issue.getSeverity() == IssueSeverity.HIGH || issue.getSeverity() == IssueSeverity.CRITICAL));
        
        // Generate AI-powered recommendations
        result.setRecommendations(generateAIRecommendations(sourceInfo, allIssues));
        
        logger.debug("Analysis complete for {}: {} issues found", 
                    sourceInfo.getFilePath(), allIssues.size());
        
        return result;
    }
    
    /**
     * Generates AI-powered concurrency recommendations using Spring AI.
     */
    private List<ConcurrencyRecommendation> generateAIRecommendations(
            JavaSourceInfo sourceInfo, List<ConcurrencyIssue> issues) {
        
        if (issues.isEmpty()) {
            return List.of(); // No recommendations needed for thread-safe code
        }
        
        try {
            String prompt = buildRecommendationPrompt(sourceInfo, issues);
            
            PromptTemplate promptTemplate = new PromptTemplate(prompt);
            Prompt chatPrompt = promptTemplate.create();
            
            String aiResponse = chatClient.prompt(chatPrompt).call().content();
            
            return parseAIRecommendations(aiResponse, issues);
            
        } catch (Exception e) {
            logger.error("Failed to generate AI recommendations for: {}", sourceInfo.getFilePath(), e);
            return generateFallbackRecommendations(issues);
        }
    }
    
    /**
     * Builds a comprehensive prompt for AI analysis following Anthropic's guidelines.
     */
    private String buildRecommendationPrompt(JavaSourceInfo sourceInfo, List<ConcurrencyIssue> issues) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a Java concurrency expert. Analyze the following concurrency issues ");
        prompt.append("in a Java class and provide specific, actionable recommendations.\n\n");
        
        prompt.append("File: ").append(sourceInfo.getFilePath()).append("\n");
        prompt.append("Classes: ").append(sourceInfo.getClasses().stream()
                .map(ClassInfo::getName).toList()).append("\n\n");
        
        prompt.append("Concurrency Issues Found:\n");
        for (int i = 0; i < issues.size(); i++) {
            ConcurrencyIssue issue = issues.get(i);
            prompt.append(String.format("%d. %s (Line %d) - %s: %s\n", 
                i + 1, issue.getType(), issue.getLineNumber(), 
                issue.getSeverity(), issue.getDescription()));
        }
        
        prompt.append("\nPlease provide:\n");
        prompt.append("1. Priority ranking (HIGH/MEDIUM/LOW) for each issue\n");
        prompt.append("2. Specific code changes or patterns to fix each issue\n");
        prompt.append("3. Estimated effort (SMALL/MEDIUM/LARGE) for each fix\n");
        prompt.append("4. Alternative approaches where applicable\n");
        prompt.append("5. Performance considerations\n\n");
        
        prompt.append("Format your response as a structured list with clear action items.");
        
        return prompt.toString();
    }
    
    /**
     * Parses AI response into structured recommendations.
     */
    private List<ConcurrencyRecommendation> parseAIRecommendations(String aiResponse, List<ConcurrencyIssue> issues) {
        List<ConcurrencyRecommendation> recommendations = new ArrayList<>();
        
        // Simple parsing logic - in production, this would be more sophisticated
        String[] lines = aiResponse.split("\n");
        ConcurrencyRecommendation current = null;
        
        for (String line : lines) {
            line = line.trim();
            if (line.matches("\\d+\\..*")) {
                // New recommendation
                if (current != null) {
                    recommendations.add(current);
                }
                current = new ConcurrencyRecommendation();
                current.setDescription(line);
                current.setPriority(RecommendationPriority.MEDIUM); // Default
                current.setEffort(RecommendationEffort.MEDIUM); // Default
            } else if (current != null && !line.isEmpty()) {
                // Add to current recommendation
                current.setDescription(current.getDescription() + "\n" + line);
                
                // Extract priority and effort if mentioned
                if (line.toLowerCase().contains("high priority")) {
                    current.setPriority(RecommendationPriority.HIGH);
                } else if (line.toLowerCase().contains("low priority")) {
                    current.setPriority(RecommendationPriority.LOW);
                }
                
                if (line.toLowerCase().contains("large effort")) {
                    current.setEffort(RecommendationEffort.LARGE);
                } else if (line.toLowerCase().contains("small effort")) {
                    current.setEffort(RecommendationEffort.SMALL);
                }
            }
        }
        
        if (current != null) {
            recommendations.add(current);
        }
        
        return recommendations;
    }
    
    /**
     * Generates fallback recommendations when AI analysis fails.
     */
    private List<ConcurrencyRecommendation> generateFallbackRecommendations(List<ConcurrencyIssue> issues) {
        return issues.stream()
            .map(issue -> {
                ConcurrencyRecommendation rec = new ConcurrencyRecommendation();
                rec.setDescription("Review and fix: " + issue.getDescription());
                rec.setPriority(RecommendationPriority.MEDIUM);
                rec.setEffort(RecommendationEffort.MEDIUM);
                return rec;
            })
            .toList();
    }
    
    /**
     * Creates an error result when analysis fails.
     */
    private AnalysisResult createErrorResult(JavaSourceInfo sourceInfo, Exception error) {
        AnalysisResult result = new AnalysisResult();
        result.setFilePath(Paths.get(sourceInfo.getFilePath()));
        result.setAnalyzedClasses(0);
        result.setThreadSafe(false);
        result.setHasErrors(true);
        result.setErrorMessage(error.getMessage());
        
        return result;
    }
}
