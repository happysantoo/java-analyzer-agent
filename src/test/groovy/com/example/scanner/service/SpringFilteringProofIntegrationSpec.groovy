package com.example.scanner.service

import com.example.scanner.model.*
import com.example.scanner.analyzer.*
import com.example.scanner.config.ScannerConfiguration
import org.springframework.ai.chat.client.ChatClient

import spock.lang.Specification
import spock.lang.Shared
import spock.lang.Subject
import spock.lang.Unroll

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * Integration test that proves Spring annotation filtering works correctly.
 * This test demonstrates that:
 * 1. When Spring filtering is enabled, only Spring-annotated classes are analyzed
 * 2. When Spring filtering is disabled, all classes are analyzed
 * 3. The filtering configuration is properly loaded from YAML
 */
class SpringFilteringProofIntegrationSpec extends Specification {

    @Subject
    JavaSourceAnalysisService sourceAnalysisService
    
    ScannerConfiguration configuration
    
    @Shared
    Path tempTestFile

    def setup() {
        sourceAnalysisService = new JavaSourceAnalysisService()
        configuration = new ScannerConfiguration()
    }
    
    def setupSpec() {
        // Create a temporary test file with both Spring-annotated and plain classes
        tempTestFile = Files.createTempFile("SpringFilterTest", ".java")
        
        String testClassContent = '''
package com.example.test;

import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SpringManagedService {
    private Map<String, String> cache = new HashMap<>(); // Concurrency issue
    private int counter = 0; // Concurrency issue
    
    public void updateCache(String key, String value) {
        cache.put(key, value); // Race condition
        counter++; // Race condition
    }
}

@Component
public class SpringManagedComponent {
    private List<String> data = new ArrayList<>(); // Concurrency issue
    
    public void addData(String item) {
        data.add(item); // Race condition
    }
}

public class PlainJavaClass {
    private static Map<String, String> staticCache = new HashMap<>(); // Concurrency issue
    private int plainCounter = 0; // Concurrency issue
    
    public static void updateStaticCache(String key, String value) {
        staticCache.put(key, value); // Race condition
    }
    
    public void incrementPlain() {
        plainCounter++; // Race condition
    }
}

public class AnotherPlainClass {
    private List<Integer> numbers = new ArrayList<>(); // Concurrency issue
    
    public void addNumber(int num) {
        numbers.add(num); // Race condition
    }
}
'''
        
        Files.write(tempTestFile, testClassContent.bytes, StandardOpenOption.WRITE)
    }
    
    def cleanupSpec() {
        Files.deleteIfExists(tempTestFile)
    }

    def "should analyze only Spring-annotated classes when filtering is enabled"() {
        given: "Spring filtering is enabled"
        sourceAnalysisService.setSpringFilterEnabled(true)
        sourceAnalysisService.setSpringAnnotations(["Service", "Component", "Repository", "Controller", "RestController", "Configuration"])
        
        when: "analyzing the test file"
        List<JavaSourceInfo> sourceResults = sourceAnalysisService.analyzeJavaFiles([tempTestFile])
        
        then: "only Spring-annotated classes should be included in the results"
        sourceResults.size() == 1
        
        JavaSourceInfo sourceInfo = sourceResults[0]
        sourceInfo.classes.size() == 2 // Only SpringManagedService and SpringManagedComponent
        
        List<String> classNames = sourceInfo.classes.collect { it.name }
        classNames.contains("SpringManagedService")
        classNames.contains("SpringManagedComponent")
        !classNames.contains("PlainJavaClass")
        !classNames.contains("AnotherPlainClass")
        
        and: "Spring-managed classes should have their Spring annotations detected"
        ClassInfo serviceClass = sourceInfo.classes.find { it.name == "SpringManagedService" }
        serviceClass.isSpringManaged()
        serviceClass.springAnnotations.contains("Service")
        
        ClassInfo componentClass = sourceInfo.classes.find { it.name == "SpringManagedComponent" }
        componentClass.isSpringManaged()
        componentClass.springAnnotations.contains("Component")
    }

    def "should analyze all classes when filtering is disabled"() {
        given: "Spring filtering is disabled"
        sourceAnalysisService.setSpringFilterEnabled(false)
        
        when: "analyzing the test file"
        List<JavaSourceInfo> sourceResults = sourceAnalysisService.analyzeJavaFiles([tempTestFile])
        
        then: "all classes should be included in the results"
        sourceResults.size() == 1
        
        JavaSourceInfo sourceInfo = sourceResults[0]
        sourceInfo.classes.size() == 4 // All classes: SpringManagedService, SpringManagedComponent, PlainJavaClass, AnotherPlainClass
        
        List<String> classNames = sourceInfo.classes.collect { it.name }
        classNames.contains("SpringManagedService")
        classNames.contains("SpringManagedComponent")
        classNames.contains("PlainJavaClass")
        classNames.contains("AnotherPlainClass")
    }

    def "should demonstrate that filtering affects concurrency analysis results"() {
        given: "we compare results with filtering enabled vs disabled"
        
        when: "analyzing with Spring filtering enabled"
        sourceAnalysisService.setSpringFilterEnabled(true)
        sourceAnalysisService.setSpringAnnotations(["Service", "Component", "Repository", "Controller", "RestController", "Configuration"])
        List<JavaSourceInfo> springFilteredSources = sourceAnalysisService.analyzeJavaFiles([tempTestFile])
        
        and: "analyzing with Spring filtering disabled"
        sourceAnalysisService.setSpringFilterEnabled(false)
        List<JavaSourceInfo> allSources = sourceAnalysisService.analyzeJavaFiles([tempTestFile])
        
        then: "filtered results should have fewer classes analyzed"
        springFilteredSources[0].classes.size() < allSources[0].classes.size()
        
        and: "filtered sources should only contain Spring-annotated classes"
        springFilteredSources[0].classes.size() == 2 // Only Spring classes
        allSources[0].classes.size() == 4 // All classes
        
        List<String> springClassNames = springFilteredSources[0].classes.collect { it.name }
        springClassNames.contains("SpringManagedService")
        springClassNames.contains("SpringManagedComponent")
        !springClassNames.contains("PlainJavaClass")
        !springClassNames.contains("AnotherPlainClass")
    }

    def "should load Spring filter configuration from YAML"() {
        given: "a configuration with Spring filtering enabled"
        
        when: "loading configuration from the test scanner_config.yaml"
        String configPath = "src/main/resources/scanner_config.yaml"
        configuration.loadConfiguration(configPath)
        
        then: "Spring filter configuration should be loaded correctly"
        configuration.isSpringFilterEnabled() == true // We set this to true in scanner_config.yaml
        configuration.getSpringAnnotations().contains("Service")
        configuration.getSpringAnnotations().contains("Component")
        configuration.getSpringAnnotations().contains("Repository")
        configuration.getSpringAnnotations().contains("Controller")
        configuration.getSpringAnnotations().contains("RestController")
        configuration.getSpringAnnotations().contains("Configuration")
    }

    @Unroll
    def "should correctly identify Spring annotations: #annotationType"() {
        given: "a test class with specific Spring annotation"
        String testContent = """
package com.example.test;

import org.springframework.stereotype.$annotationType;
import java.util.HashMap;
import java.util.Map;

@$annotationType
public class Test${annotationType}Class {
    private Map<String, String> data = new HashMap<>();
    
    public void updateData(String key, String value) {
        data.put(key, value);
    }
}
"""
        
        Path tempFile = Files.createTempFile("TestAnnotation", ".java")
        Files.write(tempFile, testContent.bytes, StandardOpenOption.WRITE)
        
        and: "Spring filtering is enabled with this annotation type"
        sourceAnalysisService.setSpringFilterEnabled(true)
        sourceAnalysisService.setSpringAnnotations([annotationType])
        
        when: "analyzing the file"
        List<JavaSourceInfo> results = sourceAnalysisService.analyzeJavaFiles([tempFile])
        
        then: "the class should be identified as Spring-managed"
        results.size() == 1
        results[0].classes.size() == 1
        
        ClassInfo classInfo = results[0].classes[0]
        classInfo.isSpringManaged()
        classInfo.springAnnotations.contains(annotationType)
        
        cleanup:
        Files.deleteIfExists(tempFile)
        
        where:
        annotationType << ["Service", "Component", "Repository", "Controller", "RestController", "Configuration"]
    }

    def "should prove that non-Spring classes are excluded when filtering is enabled"() {
        given: "Spring filtering is enabled"
        sourceAnalysisService.setSpringFilterEnabled(true)
        
        and: "a file with only non-Spring classes"
        String plainClassContent = '''
package com.example.test;

import java.util.HashMap;
import java.util.Map;

public class PlainClass1 {
    private Map<String, String> cache = new HashMap<>();
    
    public void update(String key, String value) {
        cache.put(key, value);
    }
}

public class PlainClass2 {
    private static int counter = 0;
    
    public static void increment() {
        counter++;
    }
}
'''
        
        Path plainFile = Files.createTempFile("PlainClasses", ".java")
        Files.write(plainFile, plainClassContent.bytes, StandardOpenOption.WRITE)
        
        when: "analyzing the plain classes file"
        List<JavaSourceInfo> results = sourceAnalysisService.analyzeJavaFiles([plainFile])
        
        then: "no classes should be analyzed"
        results.size() == 1
        results[0].classes.size() == 0 // No classes should be included
        
        cleanup:
        Files.deleteIfExists(plainFile)
    }
}
