package com.example.scanner.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;

import com.example.scanner.config.ScannerConfiguration;

/**
 * Service responsible for discovering Java files in the project directory.
 * Implements the Java File Discovery partition from the activity diagram.
 */
@Service
public class JavaFileDiscoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(JavaFileDiscoveryService.class);
    
    @Autowired
    private ScannerConfiguration configuration;
    
    /**
     * Recursively discovers Java files in the specified project directory.
     * Filters for *.java extensions and optionally excludes test files.
     */
    public List<Path> discoverJavaFiles(Path projectPath) throws IOException {
        logger.info("Starting Java file discovery in: {}", projectPath);
        
        List<Path> javaFiles = Files.walk(projectPath)
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".java"))
            .filter(this::shouldIncludeFile)
            .collect(Collectors.toList());
            
        logger.info("Discovered {} Java files", javaFiles.size());
        
        if (logger.isDebugEnabled()) {
            javaFiles.forEach(file -> logger.debug("Found Java file: {}", file));
        }
        
        return javaFiles;
    }
    
    /**
     * Determines if a file should be included based on configuration.
     * Excludes test files and generated code if configured to do so.
     */
    private boolean shouldIncludeFile(Path filePath) {
        String fileName = filePath.toString().toLowerCase();
        
        // Exclude test files if configured
        if (configuration.isExcludeTestFiles()) {
            if (fileName.contains("/test/") || 
                fileName.contains("\\test\\") ||
                fileName.endsWith("test.java") ||
                fileName.endsWith("tests.java") ||
                fileName.contains("test")) {
                logger.debug("Excluding test file: {}", filePath);
                return false;
            }
        }
        
        // Exclude generated code if configured
        if (configuration.isExcludeGeneratedCode()) {
            if (fileName.contains("/generated/") || 
                fileName.contains("\\generated\\") ||
                fileName.contains("/target/generated-sources/") ||
                fileName.contains("\\target\\generated-sources\\")) {
                logger.debug("Excluding generated file: {}", filePath);
                return false;
            }
        }
        
        // Exclude specific patterns from configuration
        for (String excludePattern : configuration.getExcludePatterns()) {
            if (fileName.contains(excludePattern.toLowerCase())) {
                logger.debug("Excluding file matching pattern '{}': {}", excludePattern, filePath);
                return false;
            }
        }
        
        return true;
    }
}
