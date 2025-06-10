package com.example.scanner.service

import com.example.scanner.model.*
import com.example.scanner.analyzer.*
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import spock.lang.Specification

/**
 * Spock specification for ConcurrencyAnalysisEngine
 */
class ConcurrencyAnalysisEngineSpec extends Specification {

    ConcurrencyAnalysisEngine engine
    ChatClient mockChatClient
    ThreadSafetyAnalyzer mockThreadSafetyAnalyzer
    SynchronizationAnalyzer mockSynchronizationAnalyzer
    ConcurrentCollectionsAnalyzer mockConcurrentCollectionsAnalyzer
    ExecutorFrameworkAnalyzer mockExecutorFrameworkAnalyzer
    AtomicOperationsAnalyzer mockAtomicOperationsAnalyzer
    LockUsageAnalyzer mockLockUsageAnalyzer
    
    def setup() {
        engine = new ConcurrencyAnalysisEngine()
        
        // Create mocks for all dependencies
        mockChatClient = Mock(ChatClient)
        mockThreadSafetyAnalyzer = Mock(ThreadSafetyAnalyzer)
        mockSynchronizationAnalyzer = Mock(SynchronizationAnalyzer)
        mockConcurrentCollectionsAnalyzer = Mock(ConcurrentCollectionsAnalyzer)
        mockExecutorFrameworkAnalyzer = Mock(ExecutorFrameworkAnalyzer)
        mockAtomicOperationsAnalyzer = Mock(AtomicOperationsAnalyzer)
        mockLockUsageAnalyzer = Mock(LockUsageAnalyzer)
        
        // Inject mocks
        engine.chatClient = mockChatClient
        engine.threadSafetyAnalyzer = mockThreadSafetyAnalyzer
        engine.synchronizationAnalyzer = mockSynchronizationAnalyzer
        engine.concurrentCollectionsAnalyzer = mockConcurrentCollectionsAnalyzer
        engine.executorFrameworkAnalyzer = mockExecutorFrameworkAnalyzer
        engine.atomicOperationsAnalyzer = mockAtomicOperationsAnalyzer
        engine.lockUsageAnalyzer = mockLockUsageAnalyzer
    }
    
    def "should analyze concurrency issues for single source file"() {
        given: "a Java source info with classes"
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        def classInfo = createClassInfo("TestClass")
        sourceInfo.classes = [classInfo]
        
        and: "analyzers return various issues"
        def threadSafetyIssue = createConcurrencyIssue("RACE_CONDITION", IssueSeverity.HIGH)
        def syncIssue = createConcurrencyIssue("DEADLOCK_RISK", IssueSeverity.MEDIUM)
        
        mockThreadSafetyAnalyzer.analyze(sourceInfo, classInfo) >> [threadSafetyIssue]
        mockSynchronizationAnalyzer.analyze(sourceInfo, classInfo) >> [syncIssue]
        mockConcurrentCollectionsAnalyzer.analyze(sourceInfo, classInfo) >> []
        mockExecutorFrameworkAnalyzer.analyze(sourceInfo, classInfo) >> []
        mockAtomicOperationsAnalyzer.analyze(sourceInfo, classInfo) >> []
        mockLockUsageAnalyzer.analyze(sourceInfo, classInfo) >> []
        
        and: "AI client returns recommendations"
        def mockChatResponse = Mock(Object) {
            content() >> "1. Fix race condition with synchronization\n2. Review deadlock scenarios"
        }
        def mockChatPrompt = Mock(Object) {
            call() >> mockChatResponse
        }
        mockChatClient.prompt(_) >> mockChatPrompt
        
        when: "analyzing concurrency issues"
        def results = engine.analyzeConcurrencyIssues([sourceInfo])
        
        then: "should return analysis results"
        results.size() == 1
        def result = results[0]
        
        result.filePath.toString().endsWith("TestClass.java")
        result.analyzedClasses == 1
        result.issues.size() == 2
        result.issues.contains(threadSafetyIssue)
        result.issues.contains(syncIssue)
        !result.threadSafe // has high severity issues
        result.recommendations.size() >= 1
    }
    
    def "should handle multiple source files"() {
        given: "multiple Java source files"
        def sourceInfo1 = createJavaSourceInfo("ClassA.java", "public class ClassA {}")
        def sourceInfo2 = createJavaSourceInfo("ClassB.java", "public class ClassB {}")
        
        sourceInfo1.classes = [createClassInfo("ClassA")]
        sourceInfo2.classes = [createClassInfo("ClassB")]
        
        and: "analyzers return different issues for each file"
        mockThreadSafetyAnalyzer.analyze(sourceInfo1, _) >> [createConcurrencyIssue("RACE_CONDITION", IssueSeverity.HIGH)]
        mockThreadSafetyAnalyzer.analyze(sourceInfo2, _) >> []
        mockSynchronizationAnalyzer.analyze(_, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(_, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(_, _) >> []
        mockAtomicOperationsAnalyzer.analyze(_, _) >> []
        mockLockUsageAnalyzer.analyze(_, _) >> []
        
        and: "mock AI responses"
        def mockResponse = Mock(Object) {
            content() >> "1. Fix identified issues"
        }
        def mockPrompt = Mock(Object) {
            call() >> mockResponse
        }
        mockChatClient.prompt(_) >> mockPrompt
        
        when: "analyzing multiple files"
        def results = engine.analyzeConcurrencyIssues([sourceInfo1, sourceInfo2])
        
        then: "should return results for all files"
        results.size() == 2
        
        def resultA = results.find { it.filePath.toString().endsWith("ClassA.java") }
        def resultB = results.find { it.filePath.toString().endsWith("ClassB.java") }
        
        resultA.issues.size() == 1
        !resultA.threadSafe
        
        resultB.issues.size() == 0
        resultB.threadSafe
    }
    
    def "should determine thread safety correctly"() {
        given: "source info with various severity issues"
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        sourceInfo.classes = [createClassInfo("TestClass")]
        
        and: "analyzers return issues of different severities"
        mockThreadSafetyAnalyzer.analyze(_, _) >> [
            createConcurrencyIssue("LOW_ISSUE", IssueSeverity.LOW),
            createConcurrencyIssue("MEDIUM_ISSUE", IssueSeverity.MEDIUM)
        ]
        mockSynchronizationAnalyzer.analyze(_, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(_, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(_, _) >> []
        mockAtomicOperationsAnalyzer.analyze(_, _) >> []
        mockLockUsageAnalyzer.analyze(_, _) >> []
        
        and: "mock AI response"
        def mockResponse2 = Mock(Object) {
            content() >> "Review medium severity issues"
        }
        def mockPrompt2 = Mock(Object) {
            call() >> mockResponse2
        }
        mockChatClient.prompt(_) >> mockPrompt2
        
        when: "analyzing concurrency issues"
        def results = engine.analyzeConcurrencyIssues([sourceInfo])
        
        then: "should be considered thread safe (no HIGH/CRITICAL issues)"
        results[0].threadSafe
    }
    
    def "should mark as not thread safe for high severity issues"() {
        given: "source info with high severity issues"
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        sourceInfo.classes = [createClassInfo("TestClass")]
        
        and: "analyzers return high severity issues"
        mockThreadSafetyAnalyzer.analyze(_, _) >> [
            createConcurrencyIssue("CRITICAL_ISSUE", IssueSeverity.CRITICAL)
        ]
        mockSynchronizationAnalyzer.analyze(_, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(_, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(_, _) >> []
        mockAtomicOperationsAnalyzer.analyze(_, _) >> []
        mockLockUsageAnalyzer.analyze(_, _) >> []
        
        and: "mock AI response"
        def mockResponse3 = Mock(Object) {
            content() >> "Critical issue needs immediate attention"
        }
        def mockPrompt3 = Mock(Object) {
            call() >> mockResponse3
        }
        mockChatClient.prompt(_) >> mockPrompt3
        
        when: "analyzing concurrency issues"
        def results = engine.analyzeConcurrencyIssues([sourceInfo])
        
        then: "should be marked as not thread safe"
        !results[0].threadSafe
    }
    
    def "should handle analyzer exceptions gracefully"() {
        given: "source info that causes analyzer to throw exception"
        def sourceInfo = createJavaSourceInfo("ProblematicClass.java", "public class ProblematicClass {}")
        sourceInfo.classes = [createClassInfo("ProblematicClass")]
        
        and: "one analyzer throws exception"
        mockThreadSafetyAnalyzer.analyze(_, _) >> { throw new RuntimeException("Analyzer failed") }
        mockSynchronizationAnalyzer.analyze(_, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(_, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(_, _) >> []
        mockAtomicOperationsAnalyzer.analyze(_, _) >> []
        mockLockUsageAnalyzer.analyze(_, _) >> []
        
        when: "analyzing with failing analyzer"
        def results = engine.analyzeConcurrencyIssues([sourceInfo])
        
        then: "should handle gracefully and return error result"
        results.size() == 1
        def result = results[0]
        result.hasErrors
        result.errorMessage.contains("Analyzer failed")
    }
    
    def "should handle AI service failures"() {
        given: "source info with issues"
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        sourceInfo.classes = [createClassInfo("TestClass")]
        
        and: "analyzers return issues"
        mockThreadSafetyAnalyzer.analyze(_, _) >> [createConcurrencyIssue("RACE_CONDITION", IssueSeverity.HIGH)]
        mockSynchronizationAnalyzer.analyze(_, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(_, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(_, _) >> []
        mockAtomicOperationsAnalyzer.analyze(_, _) >> []
        mockLockUsageAnalyzer.analyze(_, _) >> []
        
        and: "AI service fails"
        mockChatClient.prompt(_) >> { throw new RuntimeException("AI service unavailable") }
        
        when: "analyzing with AI failure"
        def results = engine.analyzeConcurrencyIssues([sourceInfo])
        
        then: "should provide fallback recommendations"
        results.size() == 1
        def result = results[0]
        !result.hasErrors // AI failure shouldn't mark as error
        result.recommendations.size() >= 1 // fallback recommendations
        result.recommendations[0].description.contains("Review and fix")
    }
    
    def "should handle empty source file list"() {
        when: "analyzing empty source file list"
        def results = engine.analyzeConcurrencyIssues([])
        
        then: "should return empty results"
        results.isEmpty()
    }
    
    def "should handle classes with no issues"() {
        given: "source info with thread-safe class"
        def sourceInfo = createJavaSourceInfo("ThreadSafeClass.java", "public class ThreadSafeClass {}")
        sourceInfo.classes = [createClassInfo("ThreadSafeClass")]
        
        and: "all analyzers return no issues"
        mockThreadSafetyAnalyzer.analyze(_, _) >> []
        mockSynchronizationAnalyzer.analyze(_, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(_, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(_, _) >> []
        mockAtomicOperationsAnalyzer.analyze(_, _) >> []
        mockLockUsageAnalyzer.analyze(_, _) >> []
        
        when: "analyzing thread-safe class"
        def results = engine.analyzeConcurrencyIssues([sourceInfo])
        
        then: "should return clean results"
        results.size() == 1
        def result = results[0]
        
        result.issues.isEmpty()
        result.threadSafe
        result.recommendations.isEmpty() // no recommendations for clean code
        !result.hasErrors
    }
    
    def "should call all analyzers for each class"() {
        given: "a Java source info with multiple classes"
        def classA = createClassInfo("ClassA")
        def classB = createClassInfo("ClassB")
        def sourceInfo = createJavaSourceInfo("MultiClass.java", "public class ClassA {} public class ClassB {}")
        sourceInfo.classes = [classA, classB]

        when: "analyzing concurrency issues"
        def results = engine.analyzeConcurrencyIssues([sourceInfo])

        then: "each analyzer should be called twice (once for each class)"
        2 * mockThreadSafetyAnalyzer.analyze(sourceInfo, _) >> []
        2 * mockSynchronizationAnalyzer.analyze(sourceInfo, _) >> []
        2 * mockConcurrentCollectionsAnalyzer.analyze(sourceInfo, _) >> []
        2 * mockExecutorFrameworkAnalyzer.analyze(sourceInfo, _) >> []
        2 * mockAtomicOperationsAnalyzer.analyze(sourceInfo, _) >> []
        2 * mockLockUsageAnalyzer.analyze(sourceInfo, _) >> []
        
        and: "should return results"
        results.size() == 1
        results[0].threadSafe
        results[0].issues.isEmpty()
    }
    
    // Helper methods
    private JavaSourceInfo createJavaSourceInfo(String fileName, String content) {
        def sourceInfo = new JavaSourceInfo()
        sourceInfo.filePath = fileName
        sourceInfo.content = content
        sourceInfo.threadRelatedImports = [] as Set
        sourceInfo.classes = []
        return sourceInfo
    }
    
    private ClassInfo createClassInfo(String className) {
        def classInfo = new ClassInfo()
        classInfo.setName(className)
        classInfo.setFields([])
        classInfo.setMethods([])
        classInfo.setParentClasses([])
        classInfo.setImplementedInterfaces([])
        return classInfo
    }

    private ConcurrencyIssue createConcurrencyIssue(String type, IssueSeverity severity) {
        def issue = new ConcurrencyIssue()
        issue.setType(type)
        issue.setSeverity(severity)
        issue.setClassName("TestClass")
        issue.setLineNumber(10)
        issue.setDescription("Test issue: ${type}")
        issue.setSuggestedFix("Fix suggestion")
        return issue
    }
}
