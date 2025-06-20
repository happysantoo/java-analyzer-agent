package com.example.scanner.integration

import com.example.scanner.agent.JavaScannerAgent
import com.example.scanner.config.ScannerConfiguration
import com.example.scanner.service.*
import com.example.scanner.analyzer.*
import com.example.scanner.model.*
import org.springframework.ai.chat.client.ChatClient
import spock.lang.Specification
import spock.lang.Subject

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * End-to-end integration test that proves Spring filtering works from configuration through execution.
 */
class SpringFilteringEndToEndSpec extends Specification {

    @Subject
    ScannerConfiguration configuration
    
    @Subject
    JavaSourceAnalysisService sourceAnalysisService
    
    @Subject
    JavaScannerAgent scannerAgent

    // Mock dependencies
    ConcurrencyAnalysisEngine mockAnalysisEngine = Mock()
    ConcurrencyReportGenerator mockReportGenerator = Mock()
    JavaFileDiscoveryService mockFileDiscoveryService = Mock()

    // Analysis engine mocks
    ChatClient mockChatClient = Mock()
    ThreadSafetyAnalyzer mockThreadSafetyAnalyzer = Mock()
    SynchronizationAnalyzer mockSynchronizationAnalyzer = Mock()
    ConcurrentCollectionsAnalyzer mockConcurrentCollectionsAnalyzer = Mock()
    ExecutorFrameworkAnalyzer mockExecutorFrameworkAnalyzer = Mock()
    AtomicOperationsAnalyzer mockAtomicOperationsAnalyzer = Mock()
    LockUsageAnalyzer mockLockUsageAnalyzer = Mock()

    def setup() {
        configuration = new ScannerConfiguration()
        sourceAnalysisService = new JavaSourceAnalysisService()
        scannerAgent = new JavaScannerAgent()
        
        // Wire up scanner agent
        scannerAgent.configuration = configuration
        scannerAgent.analysisEngine = mockAnalysisEngine
        scannerAgent.reportGenerator = mockReportGenerator
        scannerAgent.fileDiscoveryService = mockFileDiscoveryService
        scannerAgent.sourceAnalysisService = sourceAnalysisService
        
        // Configure mock behaviors
        mockAnalysisEngine.analyzeSourceFiles(_) >> new AnalysisResult()
        mockReportGenerator.generateReport(_) >> "test-report.html"
        mockFileDiscoveryService.discoverJavaFiles(_) >> []
    }

    def "should load Spring filter configuration from YAML correctly"() {
        when: "loading configuration from the main scanner_config.yaml"
        String configPath = Paths.get("src", "main", "resources", "scanner_config.yaml").toString()
        configuration.loadConfiguration(configPath)
        
        then: "Spring filter should be enabled as configured"
        configuration.isSpringFilterEnabled() == true
        configuration.getSpringAnnotations().size() == 6
        configuration.getSpringAnnotations().containsAll([
            "Service", "Component", "Repository", 
            "Controller", "RestController", "Configuration"
        ])
    }

    def "should demonstrate complete Spring filtering workflow"() {
        given: "a test directory with mixed Spring and non-Spring classes"
        Path tempDir = Files.createTempDirectory("spring-filter-test")
        
        // Spring-managed service
        Path springServiceFile = tempDir.resolve("UserService.java")
        Files.write(springServiceFile, '''
package com.example.test;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {
    private Map<String, String> userCache = new HashMap<>(); // Concurrency issue
    private int userCount = 0; // Concurrency issue
    
    public void addUser(String id, String name) {
        userCache.put(id, name); // Race condition
        userCount++; // Race condition
    }
}
'''.bytes, StandardOpenOption.CREATE)

        // Spring-managed component
        Path springComponentFile = tempDir.resolve("DataProcessor.java") 
        Files.write(springComponentFile, '''
package com.example.test;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataProcessor {
    private List<String> data = new ArrayList<>(); // Concurrency issue
    
    public void processData(String item) {
        data.add(item); // Race condition
    }
}
'''.bytes, StandardOpenOption.CREATE)

        // Plain Java class (should be filtered out)
        Path plainClassFile = tempDir.resolve("UtilityClass.java")
        Files.write(plainClassFile, '''
package com.example.test;

import java.util.HashMap;
import java.util.Map;

public class UtilityClass {
    private static Map<String, String> cache = new HashMap<>(); // Concurrency issue but should be ignored
    
    public static void putCache(String key, String value) {
        cache.put(key, value); // Race condition but should be ignored
    }
}
'''.bytes, StandardOpenOption.CREATE)

        and: "configuration is set to enable Spring filtering"
        configuration.loadConfiguration(Paths.get("src", "main", "resources", "scanner_config.yaml").toString())
        
        when: "applying configuration to the source analysis service"
        if (configuration.isSpringFilterEnabled()) {
            sourceAnalysisService.setSpringFilterEnabled(true)
            sourceAnalysisService.setSpringAnnotations(configuration.getSpringAnnotations())
        }
        
        and: "discovering and analyzing Java files"
        List<Path> javaFiles = [springServiceFile, springComponentFile, plainClassFile]
        def sourceResults = sourceAnalysisService.analyzeJavaFiles(javaFiles)
        
        then: "should only analyze Spring-managed classes"
        sourceResults.size() == 3 // One result per file
        
        // Count total classes analyzed across all files
        int totalClassesAnalyzed = sourceResults.sum { it.classes.size() } as int
        totalClassesAnalyzed == 2 // Only Spring classes should be analyzed
        
        // Verify which classes were analyzed
        List<String> analyzedClassNames = sourceResults.collectMany { sourceInfo ->
            sourceInfo.classes.collect { it.name }
        }
        
        analyzedClassNames.contains("UserService")
        analyzedClassNames.contains("DataProcessor")
        !analyzedClassNames.contains("UtilityClass") // Plain class should be filtered out
        
        and: "Spring classes should have their annotations detected"
        def userServiceClass = sourceResults.collectMany { it.classes }.find { it.name == "UserService" }
        userServiceClass.isSpringManaged()
        userServiceClass.springAnnotations.contains("Service")
        
        def dataProcessorClass = sourceResults.collectMany { it.classes }.find { it.name == "DataProcessor" }
        dataProcessorClass.isSpringManaged()
        dataProcessorClass.springAnnotations.contains("Component")
        
        cleanup:
        [springServiceFile, springComponentFile, plainClassFile].each { Files.deleteIfExists(it) }
        Files.deleteIfExists(tempDir)
    }

    def "should compare results with filtering enabled vs disabled"() {
        given: "a test file with both Spring and non-Spring classes"
        String mixedClassContent = '''
package com.example.test;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class SpringService {
    private Map<String, String> cache = new HashMap<>();
    public void update(String key, String value) { cache.put(key, value); }
}

public class PlainClass {
    private Map<String, String> cache = new HashMap<>();
    public void update(String key, String value) { cache.put(key, value); }
}
'''
        
        Path testFile = Files.createTempFile("MixedClasses", ".java")
        Files.write(testFile, mixedClassContent.bytes, StandardOpenOption.WRITE)
        
        when: "analyzing with Spring filtering enabled"
        sourceAnalysisService.setSpringFilterEnabled(true)
        sourceAnalysisService.setSpringAnnotations(["Service"])
        def filteredResults = sourceAnalysisService.analyzeJavaFiles([testFile])
        
        and: "analyzing with Spring filtering disabled"
        sourceAnalysisService.setSpringFilterEnabled(false)
        def allResults = sourceAnalysisService.analyzeJavaFiles([testFile])
        
        then: "filtered results should contain fewer classes"
        def filteredClassCount = filteredResults.sum { it.classes.size() } as int
        def allClassCount = allResults.sum { it.classes.size() } as int
        
        filteredClassCount == 1  // Only SpringService
        allClassCount == 2       // Both SpringService and PlainClass
        
        filteredClassCount < allClassCount
        
        and: "filtered results should only contain Spring-managed classes"
        def filteredClassNames = filteredResults.collectMany { it.classes }.collect { it.name }
        filteredClassNames == ["SpringService"]
        
        and: "all results should contain both classes"
        def allClassNames = allResults.collectMany { it.classes }.collect { it.name }
        allClassNames.containsAll(["SpringService", "PlainClass"])
        
        cleanup:
        Files.deleteIfExists(testFile)
    }
}
