package com.example.scanner.config;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Configuration for the Java concurrency scanner following Anthropic's design principles.
 */
@Component
public class ScannerConfiguration {
    
    @Value("${scanner.exclude-test-files:true}")
    private boolean excludeTestFiles;
    
    @Value("${scanner.exclude-generated-code:true}")
    private boolean excludeGeneratedCode;
    
    private List<String> excludePatterns = new ArrayList<>();
    private int maxFileSize = 10_000_000; // 10MB default
    private boolean enableAIRecommendations = true;
    private String aiModel = "claude-3-sonnet";
    
    // Spring filtering configuration
    private boolean springFilterEnabled = false;
    private List<String> springAnnotations = List.of("Service", "Component", "Repository", "Controller", "RestController", "Configuration");
    
    /**
     * Loads configuration from YAML file.
     */
    public void loadConfiguration(String configPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(fis);
            
            if (config.containsKey("scanner")) {
                Map<String, Object> scannerConfig = (Map<String, Object>) config.get("scanner");
                
                excludeTestFiles = (Boolean) scannerConfig.getOrDefault("exclude-test-files", excludeTestFiles);
                excludeGeneratedCode = (Boolean) scannerConfig.getOrDefault("exclude-generated-code", excludeGeneratedCode);
                maxFileSize = (Integer) scannerConfig.getOrDefault("max-file-size", maxFileSize);
                enableAIRecommendations = (Boolean) scannerConfig.getOrDefault("enable-ai-recommendations", enableAIRecommendations);
                aiModel = (String) scannerConfig.getOrDefault("ai-model", aiModel);
                
                if (scannerConfig.containsKey("exclude-patterns")) {
                    excludePatterns = (List<String>) scannerConfig.get("exclude-patterns");
                }
                
                // Load Spring filter configuration
                if (scannerConfig.containsKey("spring-filter")) {
                    Map<String, Object> springFilterConfig = (Map<String, Object>) scannerConfig.get("spring-filter");
                    springFilterEnabled = (Boolean) springFilterConfig.getOrDefault("enabled", springFilterEnabled);
                    
                    if (springFilterConfig.containsKey("annotations")) {
                        springAnnotations = (List<String>) springFilterConfig.get("annotations");
                    }
                }
            }
        } catch (IOException e) {
            // Use default configuration if file not found
            System.out.println("Configuration file not found, using defaults: " + configPath);
        }
    }
    
    // Getters
    public boolean isExcludeTestFiles() { return excludeTestFiles; }
    public boolean isExcludeGeneratedCode() { return excludeGeneratedCode; }
    public List<String> getExcludePatterns() { return excludePatterns; }
    public int getMaxFileSize() { return maxFileSize; }
    public boolean isEnableAIRecommendations() { return enableAIRecommendations; }
    public String getAiModel() { return aiModel; }
    public boolean isSpringFilterEnabled() { return springFilterEnabled; }
    public List<String> getSpringAnnotations() { return springAnnotations; }
}
