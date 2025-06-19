package com.example.scanner.service

import com.example.scanner.model.*
import com.example.scanner.analyzer.*
import org.springframework.ai.chat.client.ChatClient
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

/**
 * Integration test specification to prove Spring annotation filtering works end-to-end.
 * This test verifies that only Spring-annotated classes (@Service, @Component, etc.) 
 * are analyzed and sent to AI when filtering is enabled.
 */
class SpringFilteringIntegrationSpec extends Specification {

    @Subject
    JavaSourceAnalysisService analysisService
    
    @Subject  
    ConcurrencyAnalysisEngine analysisEngine

    // Mock dependencies for analysis engine
    ChatClient mockChatClient = Mock()
    ThreadSafetyAnalyzer mockThreadSafetyAnalyzer = Mock()
    SynchronizationAnalyzer mockSynchronizationAnalyzer = Mock()
    ConcurrentCollectionsAnalyzer mockConcurrentCollectionsAnalyzer = Mock()
    ExecutorFrameworkAnalyzer mockExecutorFrameworkAnalyzer = Mock()
    AtomicOperationsAnalyzer mockAtomicOperationsAnalyzer = Mock()
    LockUsageAnalyzer mockLockUsageAnalyzer = Mock()

    def setup() {
        analysisService = new JavaSourceAnalysisService()
        analysisEngine = new ConcurrencyAnalysisEngine()
        
        // Wire up the analysis engine with mocks
        analysisEngine.chatClient = mockChatClient
        analysisEngine.threadSafetyAnalyzer = mockThreadSafetyAnalyzer
        analysisEngine.synchronizationAnalyzer = mockSynchronizationAnalyzer
        analysisEngine.concurrentCollectionsAnalyzer = mockConcurrentCollectionsAnalyzer
        analysisEngine.executorFrameworkAnalyzer = mockExecutorFrameworkAnalyzer
        analysisEngine.atomicOperationsAnalyzer = mockAtomicOperationsAnalyzer
        analysisEngine.lockUsageAnalyzer = mockLockUsageAnalyzer
    }

    def "should analyze only Spring-annotated classes when filtering is enabled"() {
        given: "Spring filtering is enabled"
        analysisService.setSpringFilterEnabled(true)
        
        and: "a Java file with mixed Spring and non-Spring classes"
        def javaCode = '''
            package com.example.service;
            
            import org.springframework.stereotype.Service;
            import org.springframework.stereotype.Component;
            import org.springframework.stereotype.Repository;
            import java.util.HashMap;
            import java.util.Map;
            
            @Service
            public class UserService {
                private Map<String, Object> userData = new HashMap<>(); // Thread safety issue
                
                public synchronized void addUser(String name, Object data) {
                    userData.put(name, data);
                }
            }
            
            @Component
            public class DataProcessor {
                private volatile int counter = 0;
                
                public void incrementCounter() {
                    counter++; // Race condition
                }
            }
            
            @Repository
            public class UserRepository {
                private HashMap<Long, String> cache = new HashMap<>(); // Thread safety issue
                
                public void cacheUser(Long id, String name) {
                    cache.put(id, name);
                }
            }
            
            // This class should be FILTERED OUT (no Spring annotation)
            public class UtilityHelper {
                private Map<String, String> config = new HashMap<>(); // Should NOT be analyzed
                
                public void updateConfig(String key, String value) {
                    config.put(key, value);
                }
            }
            
            // This class should also be FILTERED OUT (no Spring annotation)
            public class MathUtils {
                private static int calculationCount = 0; // Should NOT be analyzed
                
                public static int calculate(int a, int b) {
                    calculationCount++;
                    return a + b;
                }
            }
        '''
        
        when: "analyzing the Java file"
        def tempFile = createTempFile("MixedSpringClasses.java", javaCode)
        def sourceInfoList = analysisService.analyzeJavaFiles([tempFile])
        
        then: "should find only Spring-annotated classes"
        sourceInfoList.size() == 1
        def sourceInfo = sourceInfoList[0]
        sourceInfo.classes.size() == 3 // Only @Service, @Component, @Repository classes
        
        and: "Spring-annotated classes should be present"
        def classNames = sourceInfo.classes.collect { it.name }
        classNames.contains("UserService")
        classNames.contains("DataProcessor") 
        classNames.contains("UserRepository")
        
        and: "non-Spring classes should be filtered out"
        !classNames.contains("UtilityHelper")
        !classNames.contains("MathUtils")
        
        and: "Spring annotations should be properly tracked"
        def userService = sourceInfo.classes.find { it.name == "UserService" }
        userService.isSpringManaged()
        userService.hasSpringAnnotation("Service")
        
        def dataProcessor = sourceInfo.classes.find { it.name == "DataProcessor" }
        dataProcessor.isSpringManaged()
        dataProcessor.hasSpringAnnotation("Component")
        
        def userRepository = sourceInfo.classes.find { it.name == "UserRepository" }
        userRepository.isSpringManaged()
        userRepository.hasSpringAnnotation("Repository")

        cleanup:
        Files.deleteIfExists(tempFile)
    }

    def "should analyze all classes when filtering is disabled"() {
        given: "Spring filtering is disabled"
        analysisService.setSpringFilterEnabled(false)
        
        and: "the same Java file with mixed Spring and non-Spring classes"
        def javaCode = '''
            package com.example.service;
            
            import org.springframework.stereotype.Service;
            import java.util.HashMap;
            import java.util.Map;
            
            @Service
            public class UserService {
                private Map<String, Object> userData = new HashMap<>();
            }
            
            public class UtilityHelper {
                private Map<String, String> config = new HashMap<>();
            }
            
            public class MathUtils {
                private static int calculationCount = 0;
            }
        '''
        
        when: "analyzing the Java file"
        def tempFile = createTempFile("MixedClasses.java", javaCode)
        def sourceInfoList = analysisService.analyzeJavaFiles([tempFile])
        
        then: "should find all classes"
        sourceInfoList.size() == 1
        def sourceInfo = sourceInfoList[0]
        sourceInfo.classes.size() == 3 // All classes included
        
        and: "all classes should be present"
        def classNames = sourceInfo.classes.collect { it.name }
        classNames.contains("UserService")
        classNames.contains("UtilityHelper")
        classNames.contains("MathUtils")
        
        and: "Spring annotation information should still be tracked"
        def userService = sourceInfo.classes.find { it.name == "UserService" }
        userService.isSpringManaged()
        userService.hasSpringAnnotation("Service")
        
        def utilityHelper = sourceInfo.classes.find { it.name == "UtilityHelper" }
        !utilityHelper.isSpringManaged()
        utilityHelper.springAnnotations.isEmpty()

        cleanup:
        Files.deleteIfExists(tempFile)
    }

    def "should prove that only Spring-annotated classes reach the AI service"() {
        given: "Spring filtering is enabled"
        analysisService.setSpringFilterEnabled(true)
        
        and: "a Java file with both Spring and non-Spring classes"
        def javaCode = '''
            import org.springframework.stereotype.Service;
            import org.springframework.stereotype.Component;
            import java.util.HashMap;
            
            @Service
            public class SpringService {
                private HashMap<String, Object> data = new HashMap<>();
            }
            
            @Component  
            public class SpringComponent {
                private volatile int counter = 0;
            }
            
            public class PlainClass {
                private HashMap<String, Object> plainData = new HashMap<>();
            }
            
            public class AnotherPlainClass {
                private static int staticCounter = 0;
            }
        '''
        
        and: "keep track of which classes are analyzed"
        def analyzedClasses = []
        mockThreadSafetyAnalyzer.analyze(_, _) >> { sourceInfo, classInfo ->
            analyzedClasses.add(classInfo.name)
            // Assert that only Spring-managed classes reach this analyzer
            assert classInfo.isSpringManaged() : "Non-Spring class ${classInfo.name} reached the analyzer when filtering is enabled!"
            // Return an issue for this Spring-managed class
            return [createConcurrencyIssue("RACE_CONDITION", "Thread safety issue in ${classInfo.name}", IssueSeverity.HIGH)]
        }
        
        // Mock other analyzers to always return empty lists (never null)
        mockSynchronizationAnalyzer.analyze(_, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(_, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(_, _) >> []
        mockAtomicOperationsAnalyzer.analyze(_, _) >> []
        mockLockUsageAnalyzer.analyze(_, _) >> []
        
        and: "AI service returns mock recommendations"
        mockChatClient.prompt(_) >> {
            def mockPrompt = Mock()
            mockPrompt.call() >> Mock() {
                content() >> "Mock AI recommendation for Spring class"
            }
            return mockPrompt
        }
        
        when: "performing end-to-end analysis"
        def tempFile = createTempFile("FilteringTest.java", javaCode)
        def sourceInfoList = analysisService.analyzeJavaFiles([tempFile])
        
        // Debug: Print what classes were found
        println "Classes found after filtering: ${sourceInfoList[0].classes.collect { it.name }}"
        println "Spring-managed classes: ${sourceInfoList[0].classes.findAll { it.isSpringManaged() }.collect { it.name }}"
        
        def analysisResults = analysisEngine.analyzeConcurrencyIssues(sourceInfoList)
        
        then: "only Spring-annotated classes should be included in analysis"
        sourceInfoList.size() == 1
        sourceInfoList[0].classes.size() == 2 // Only Spring classes
        def classNames = sourceInfoList[0].classes.collect { it.name }
        classNames.contains("SpringService")
        classNames.contains("SpringComponent")
        !classNames.contains("PlainClass")
        !classNames.contains("AnotherPlainClass")
        
        and: "analysis should complete successfully"
        analysisResults.size() == 1
        def result = analysisResults[0]
        !result.hasErrors
        result.analyzedClasses == 2
        
        and: "only Spring classes should have been sent to analyzers"
        analyzedClasses.size() == 2
        analyzedClasses.contains("SpringService")
        analyzedClasses.contains("SpringComponent")
        !analyzedClasses.contains("PlainClass")
        !analyzedClasses.contains("AnotherPlainClass")
        
        and: "should have found issues for Spring classes"
        result.issues.size() == 2 // One issue from each Spring class
        result.issues.every { issue ->
            issue.description.contains("SpringService") || issue.description.contains("SpringComponent")
        }
        
        cleanup:
        Files.deleteIfExists(tempFile)
    }

    @Unroll
    def "should detect and filter various Spring annotations: #annotation"() {
        given: "Spring filtering is enabled"
        analysisService.setSpringFilterEnabled(true)
        
        and: "a Java file with the specific Spring annotation"
        def javaCode = """
            import org.springframework.stereotype.${annotation};
            import org.springframework.web.bind.annotation.RestController;
            import org.springframework.context.annotation.Configuration;
            import java.util.HashMap;
            
            @${annotationName}
            public class Test${annotation}Class {
                private HashMap<String, Object> data = new HashMap<>();
            }
            
            public class PlainClass {
                private HashMap<String, Object> plainData = new HashMap<>();
            }
        """
        
        when: "analyzing the Java file"
        def tempFile = createTempFile("Test${annotation}.java", javaCode)
        def sourceInfoList = analysisService.analyzeJavaFiles([tempFile])
        
        then: "should include only the Spring-annotated class"
        sourceInfoList.size() == 1
        def sourceInfo = sourceInfoList[0]
        sourceInfo.classes.size() == 1 // Only the Spring-annotated class
        
        and: "the Spring-annotated class should be properly identified"
        def springClass = sourceInfo.classes[0]
        springClass.name == "Test${annotation}Class"
        springClass.isSpringManaged()
        springClass.hasSpringAnnotation(annotationName)
        
        cleanup:
        Files.deleteIfExists(tempFile)
        
        where:
        annotation      | annotationName
        "Service"       | "Service"
        "Component"     | "Component"
        "Repository"    | "Repository"
        "Controller"    | "Controller"
        "RestController"| "RestController"
        "Configuration" | "Configuration"
    }

    def "should prove filtering improves performance by reducing classes sent to analyzers"() {
        given: "a large Java file with many classes"
        def javaCode = '''
            import org.springframework.stereotype.Service;
            import org.springframework.stereotype.Component;
            import java.util.HashMap;
            
            @Service
            public class ImportantService {
                private HashMap<String, Object> data = new HashMap<>();
            }
            
            @Component
            public class CriticalComponent {
                private volatile int counter = 0;
            }
            
            // These 8 classes should be filtered out
            public class UtilHelper1 { private int data1; }
            public class UtilHelper2 { private int data2; }
            public class UtilHelper3 { private int data3; }
            public class UtilHelper4 { private int data4; }
            public class UtilHelper5 { private int data5; }
            public class UtilHelper6 { private int data6; }
            public class UtilHelper7 { private int data7; }
            public class UtilHelper8 { private int data8; }
        '''
        
        and: "analyzers are set up to track calls"
        def analyzerCallCount = 0
        mockThreadSafetyAnalyzer.analyze(_, _) >> {
            analyzerCallCount++
            return []
        }
        mockSynchronizationAnalyzer.analyze(_, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(_, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(_, _) >> []
        mockAtomicOperationsAnalyzer.analyze(_, _) >> []
        mockLockUsageAnalyzer.analyze(_, _) >> []
        
        when: "Spring filtering is enabled and analysis is performed"
        analysisService.setSpringFilterEnabled(true)
        def tempFile = createTempFile("PerformanceTest.java", javaCode)
        def sourceInfoList = analysisService.analyzeJavaFiles([tempFile])
        analysisEngine.analyzeConcurrencyIssues(sourceInfoList)
        
        then: "should only analyze Spring-annotated classes"
        sourceInfoList[0].classes.size() == 2 // Only 2 Spring classes
        analyzerCallCount == 2 // ThreadSafetyAnalyzer called only for 2 Spring classes
        
        when: "Spring filtering is disabled and analysis is performed"
        analyzerCallCount = 0 // Reset counter
        analysisService.setSpringFilterEnabled(false)
        sourceInfoList = analysisService.analyzeJavaFiles([tempFile])
        analysisEngine.analyzeConcurrencyIssues(sourceInfoList)
        
        then: "should analyze all classes"
        sourceInfoList[0].classes.size() == 10 // All 10 classes
        analyzerCallCount == 10 // ThreadSafetyAnalyzer called for all 10 classes
        
        cleanup:
        Files.deleteIfExists(tempFile)
    }

    def "should handle edge cases in Spring filtering gracefully"() {
        given: "Spring filtering is enabled"
        analysisService.setSpringFilterEnabled(true)
        
        and: "a Java file with edge cases"
        def javaCode = '''
            import org.springframework.stereotype.Service;
            
            // Interface with Spring annotation (should be included)
            @Service
            public interface ServiceInterface {
                void doSomething();
            }
            
            // Class with multiple annotations (should be included)
            @Service
            @Deprecated
            public class MultiAnnotatedService {
                private String data;
            }
            
            // Class without any annotations (should be excluded)
            public class PlainClass {
                private String data;
            }
            
            // Abstract class with Spring annotation (should be included)
            @Service
            public abstract class AbstractService {
                protected String baseData;
            }
        '''
        
        when: "analyzing the Java file"
        def tempFile = createTempFile("EdgeCases.java", javaCode)
        def sourceInfoList = analysisService.analyzeJavaFiles([tempFile])
        
        then: "should handle all edge cases correctly"
        sourceInfoList.size() == 1
        def sourceInfo = sourceInfoList[0]
        sourceInfo.classes.size() == 3 // Only Spring-annotated classes
        
        and: "should include interface with Spring annotation"
        def serviceInterface = sourceInfo.classes.find { it.name == "ServiceInterface" }
        serviceInterface != null
        serviceInterface.isInterface()
        serviceInterface.isSpringManaged()
        
        and: "should include class with multiple annotations"
        def multiAnnotatedService = sourceInfo.classes.find { it.name == "MultiAnnotatedService" }
        multiAnnotatedService != null
        multiAnnotatedService.isSpringManaged()
        
        and: "should include abstract class with Spring annotation"
        def abstractService = sourceInfo.classes.find { it.name == "AbstractService" }
        abstractService != null
        abstractService.isSpringManaged()
        
        and: "should exclude plain class"
        def plainClass = sourceInfo.classes.find { it.name == "PlainClass" }
        plainClass == null
        
        cleanup:
        Files.deleteIfExists(tempFile)
    }

    // Helper method to create temporary files for testing
    private Path createTempFile(String fileName, String content) {
        def tempDir = Files.createTempDirectory("spring-filtering-test")
        def tempFile = tempDir.resolve(fileName)
        Files.write(tempFile, content.getBytes())
        return tempFile
    }

    // Helper method to create mock concurrency issues
    private ConcurrencyIssue createConcurrencyIssue(String type, String description, IssueSeverity severity) {
        def issue = new ConcurrencyIssue()
        issue.type = type
        issue.description = description
        issue.severity = severity
        issue.className = "TestClass"
        issue.lineNumber = 10
        return issue
    }

    def "should demonstrate that filtering excludes non-Spring classes from being sent to AI"() {
        given: "a clear test case with specific Spring and non-Spring classes"
        def javaCode = '''
            package com.test;
            import org.springframework.stereotype.Service;
            import org.springframework.stereotype.Component;
            
            @Service
            public class UserService {
                // This should be analyzed
                private java.util.Map<String, Object> data = new java.util.HashMap<>();
            }
            
            @Component  
            public class DataProcessor {
                // This should be analyzed
                private volatile int counter = 0;
            }
            
            public class UtilityClass {
                // This should NOT be analyzed when filtering is enabled
                private java.util.Map<String, String> cache = new java.util.HashMap<>();
            }
            
            public class MathHelper {
                // This should NOT be analyzed when filtering is enabled
                private static int calculations = 0;
            }
        '''
        
        when: "Spring filtering is ENABLED"
        analysisService.setSpringFilterEnabled(true)
        def tempFile = createTempFile("TestClasses.java", javaCode)
        def sourceInfoWithFiltering = analysisService.analyzeJavaFiles([tempFile])
        
        then: "only Spring-annotated classes should be included"
        sourceInfoWithFiltering.size() == 1
        def springFilteredClasses = sourceInfoWithFiltering[0].classes.collect { it.name }
        springFilteredClasses.size() == 2
        springFilteredClasses.contains("UserService")
        springFilteredClasses.contains("DataProcessor")
        !springFilteredClasses.contains("UtilityClass")
        !springFilteredClasses.contains("MathHelper")
        
        when: "Spring filtering is DISABLED"
        analysisService.setSpringFilterEnabled(false)
        def sourceInfoWithoutFiltering = analysisService.analyzeJavaFiles([tempFile])
        
        then: "all classes should be included"
        sourceInfoWithoutFiltering.size() == 1
        def allClasses = sourceInfoWithoutFiltering[0].classes.collect { it.name }
        allClasses.size() == 4
        allClasses.contains("UserService")
        allClasses.contains("DataProcessor")
        allClasses.contains("UtilityClass")
        allClasses.contains("MathHelper")
        
        when: "comparing the results"
        def filteredCount = springFilteredClasses.size()
        def totalCount = allClasses.size()
        
        then: "filtering should reduce the number of classes sent for analysis"
        filteredCount < totalCount
        filteredCount == 2  // Only Spring classes
        totalCount == 4     // All classes
        println "✅ FILTERING PROOF: Filtering reduced classes from ${totalCount} to ${filteredCount}"
        println "✅ Spring-only classes: ${springFilteredClasses}"
        println "✅ All classes: ${allClasses}"

        cleanup:
        Files.deleteIfExists(tempFile)
    }
}
