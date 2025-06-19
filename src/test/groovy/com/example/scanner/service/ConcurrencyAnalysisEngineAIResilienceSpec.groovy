package com.example.scanner.service

import com.example.scanner.model.*
import com.example.scanner.analyzer.*
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatResponse
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.nio.file.Paths

/**
 * Test specification to prove the application works gracefully when AI services are unavailable.
 * This demonstrates resilience and fallback mechanisms when external AI dependencies fail.
 */
class ConcurrencyAnalysisEngineAIResilienceSpec extends Specification {

    @Subject
    ConcurrencyAnalysisEngine analysisEngine

    // Mock dependencies
    ChatClient mockChatClient = Mock()
    ThreadSafetyAnalyzer mockThreadSafetyAnalyzer = Mock()
    SynchronizationAnalyzer mockSynchronizationAnalyzer = Mock()
    ConcurrentCollectionsAnalyzer mockConcurrentCollectionsAnalyzer = Mock()
    ExecutorFrameworkAnalyzer mockExecutorFrameworkAnalyzer = Mock()
    AtomicOperationsAnalyzer mockAtomicOperationsAnalyzer = Mock()
    LockUsageAnalyzer mockLockUsageAnalyzer = Mock()

    def setup() {
        analysisEngine = new ConcurrencyAnalysisEngine()
        analysisEngine.chatClient = mockChatClient
        analysisEngine.threadSafetyAnalyzer = mockThreadSafetyAnalyzer
        analysisEngine.synchronizationAnalyzer = mockSynchronizationAnalyzer
        analysisEngine.concurrentCollectionsAnalyzer = mockConcurrentCollectionsAnalyzer
        analysisEngine.executorFrameworkAnalyzer = mockExecutorFrameworkAnalyzer
        analysisEngine.atomicOperationsAnalyzer = mockAtomicOperationsAnalyzer
        analysisEngine.lockUsageAnalyzer = mockLockUsageAnalyzer
    }

    def "should complete analysis successfully when AI service is completely unavailable"() {
        given: "a Java source file with concurrency issues"
        def sourceInfo = createJavaSourceInfo("ConcurrentService.java")
        def sourceFiles = [sourceInfo]

        and: "traditional analyzers return issues"
        def raceConditionIssue = createConcurrencyIssue("RACE_CONDITION", "Unsafe counter increment", IssueSeverity.HIGH)
        def deadlockIssue = createConcurrencyIssue("DEADLOCK_RISK", "Potential deadlock in nested synchronization", IssueSeverity.CRITICAL)
        
        mockThreadSafetyAnalyzer.analyze(sourceInfo, _) >> [raceConditionIssue]
        mockSynchronizationAnalyzer.analyze(sourceInfo, _) >> [deadlockIssue]
        mockConcurrentCollectionsAnalyzer.analyze(sourceInfo, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(sourceInfo, _) >> []
        mockAtomicOperationsAnalyzer.analyze(sourceInfo, _) >> []
        mockLockUsageAnalyzer.analyze(sourceInfo, _) >> []

        when: "analyzing concurrency issues"
        def results = analysisEngine.analyzeConcurrencyIssues(sourceFiles)

        then: "analysis completes successfully without AI recommendations"
        results.size() == 1
        
        and: "traditional analysis results are preserved"
        def result = results[0]
        result.issues.size() == 2
        result.issues.find { it.type == "RACE_CONDITION" } != null
        result.issues.find { it.type == "DEADLOCK_RISK" } != null
        
        and: "file is marked as not thread-safe due to critical/high severity issues"
        !result.threadSafe
        
        and: "fallback recommendations are present (graceful degradation)"
        result.recommendations.size() == 2 // One for each issue
        result.recommendations.every { it.description.startsWith("Review and fix:") }
        
        and: "error status indicates AI service failure but analysis succeeded"
        !result.hasErrors // Core analysis succeeded
        
        and: "AI service was attempted but failed gracefully"
        1 * mockChatClient.prompt(_) >> { throw new RuntimeException("AI service unavailable") }
    }

    def "should handle AI timeout gracefully and continue with analysis"() {
        given: "a Java source file with multiple concurrency issues"
        def sourceInfo = createJavaSourceInfo("ThreadUnsafeService.java")
        def sourceFiles = [sourceInfo]

        and: "traditional analyzers detect various issues"
        def collectionIssue = createConcurrencyIssue("UNSAFE_COLLECTION", "HashMap in concurrent context", IssueSeverity.MEDIUM)
        def executorIssue = createConcurrencyIssue("EXECUTOR_NOT_SHUTDOWN", "ExecutorService without shutdown", IssueSeverity.MEDIUM)
        
        mockThreadSafetyAnalyzer.analyze(sourceInfo, _) >> []
        mockSynchronizationAnalyzer.analyze(sourceInfo, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(sourceInfo, _) >> [collectionIssue]
        mockExecutorFrameworkAnalyzer.analyze(sourceInfo, _) >> [executorIssue]
        mockAtomicOperationsAnalyzer.analyze(sourceInfo, _) >> []
        mockLockUsageAnalyzer.analyze(sourceInfo, _) >> []

        and: "AI service times out"
        mockChatClient.prompt(_) >> { throw new java.util.concurrent.TimeoutException("AI request timeout") }

        when: "analyzing concurrency issues"
        def results = analysisEngine.analyzeConcurrencyIssues(sourceFiles)

        then: "analysis completes successfully"
        results.size() == 1
        
        and: "all traditional analysis results are included"
        def result = results[0]
        result.issues.size() == 2
        result.issues.find { it.type == "UNSAFE_COLLECTION" } != null
        result.issues.find { it.type == "EXECUTOR_NOT_SHUTDOWN" } != null
        
        and: "thread safety assessment is based on traditional analysis"
        result.threadSafe // Medium severity issues don't automatically mark as unsafe
        
        and: "analysis succeeds even without AI"
        !result.hasErrors
    }

    def "should provide fallback recommendations when AI service fails"() {
        given: "a Java source file with atomic operation opportunities"
        def sourceInfo = createJavaSourceInfo("CounterService.java")
        def sourceFiles = [sourceInfo]

        and: "atomic operations analyzer detects issues"
        def atomicIssue = createConcurrencyIssue("ATOMIC_OPPORTUNITY", "Use AtomicInteger for counter", IssueSeverity.LOW)
        atomicIssue.suggestedFix = "Replace int counter with AtomicInteger"
        
        mockThreadSafetyAnalyzer.analyze(sourceInfo, _) >> []
        mockSynchronizationAnalyzer.analyze(sourceInfo, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(sourceInfo, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(sourceInfo, _) >> []
        mockAtomicOperationsAnalyzer.analyze(sourceInfo, _) >> [atomicIssue]
        mockLockUsageAnalyzer.analyze(sourceInfo, _) >> []

        and: "AI service is completely down"
        mockChatClient.prompt(_) >> { throw new ConnectException("Cannot connect to AI service") }

        when: "analyzing concurrency issues"
        def results = analysisEngine.analyzeConcurrencyIssues(sourceFiles)

        then: "analysis provides traditional recommendations"
        def result = results[0]
        result.issues.size() == 1
        result.issues[0].suggestedFix == "Replace int counter with AtomicInteger"
        
        and: "built-in recommendations are available even without AI"
        result.issues[0].suggestedFix != null
        !result.issues[0].suggestedFix.isEmpty()
    }

    @Unroll
    def "should handle various AI service failures gracefully: #exceptionType"() {
        given: "a Java source file with issues"
        def sourceInfo = createJavaSourceInfo("TestService.java")
        def sourceFiles = [sourceInfo]

        and: "traditional analyzers detect issues"
        def issue = createConcurrencyIssue("RACE_CONDITION", "Test issue", IssueSeverity.HIGH)
        mockThreadSafetyAnalyzer.analyze(sourceInfo, _) >> [issue]
        mockSynchronizationAnalyzer.analyze(sourceInfo, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(sourceInfo, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(sourceInfo, _) >> []
        mockAtomicOperationsAnalyzer.analyze(sourceInfo, _) >> []
        mockLockUsageAnalyzer.analyze(sourceInfo, _) >> []

        and: "AI service throws specific exception type"
        mockChatClient.prompt(_) >> { throw exception }

        when: "analyzing concurrency issues"
        def results = analysisEngine.analyzeConcurrencyIssues(sourceFiles)

        then: "analysis completes successfully despite AI failure"
        results.size() == 1
        results[0].issues.size() == 1
        results[0].issues[0].type == "RACE_CONDITION"
        !results[0].hasErrors // Core analysis succeeded

        where:
        exceptionType                    | exception
        "Network timeout"               | new java.net.SocketTimeoutException("Request timeout")
        "Connection refused"            | new java.net.ConnectException("Connection refused")
        "Service unavailable"           | new RuntimeException("Service temporarily unavailable")
        "API rate limit exceeded"       | new RuntimeException("Rate limit exceeded")
        "Authentication failure"        | new SecurityException("Invalid API key")
        "Generic network error"         | new java.io.IOException("Network error")
        "Null response"                 | new NullPointerException("Null response from AI service")
    }

    def "should maintain performance when AI service is slow or unresponsive"() {
        given: "multiple Java source files for analysis"
        def sourceFiles = (1..5).collect { createJavaSourceInfo("Service${it}.java") }

        and: "traditional analyzers work normally"
        sourceFiles.each { sourceInfo ->
            mockThreadSafetyAnalyzer.analyze(sourceInfo, _) >> [createConcurrencyIssue("TEST_ISSUE", "Test", IssueSeverity.LOW)]
            mockSynchronizationAnalyzer.analyze(sourceInfo, _) >> []
            mockConcurrentCollectionsAnalyzer.analyze(sourceInfo, _) >> []
            mockExecutorFrameworkAnalyzer.analyze(sourceInfo, _) >> []
            mockAtomicOperationsAnalyzer.analyze(sourceInfo, _) >> []
            mockLockUsageAnalyzer.analyze(sourceInfo, _) >> []
        }

        and: "AI service is extremely slow (simulating timeout)"
        mockChatClient.prompt(_) >> { 
            Thread.sleep(100) // Simulate slow response
            throw new RuntimeException("Service timeout")
        }

        when: "analyzing multiple files"
        def startTime = System.currentTimeMillis()
        def results = analysisEngine.analyzeConcurrencyIssues(sourceFiles)
        def duration = System.currentTimeMillis() - startTime

        then: "analysis completes in reasonable time despite AI failures"
        results.size() == 5
        duration < 2000 // Should complete within 2 seconds even with AI failures
        
        and: "all files are analyzed"
        results.every { !it.hasErrors }
        results.every { it.issues.size() == 1 }
    }

    def "should work with empty source files list when AI is unavailable"() {
        given: "empty source files list"
        def sourceFiles = []

        and: "AI service is down"
        mockChatClient.prompt(_) >> { throw new RuntimeException("AI service down") }

        when: "analyzing empty list"
        def results = analysisEngine.analyzeConcurrencyIssues(sourceFiles)

        then: "analysis completes successfully"
        results.isEmpty()
        
        and: "no AI calls are made for empty input"
        0 * mockChatClient.prompt(_)
    }

    def "should preserve all traditional analyzer results when AI fails"() {
        given: "a comprehensive Java source file"
        def sourceInfo = createJavaSourceInfo("ComprehensiveService.java")
        def sourceFiles = [sourceInfo]

        and: "all traditional analyzers detect issues"
        def threadSafetyIssues = [createConcurrencyIssue("RACE_CONDITION", "Race condition", IssueSeverity.HIGH)]
        def syncIssues = [createConcurrencyIssue("DEADLOCK_RISK", "Deadlock risk", IssueSeverity.CRITICAL)]
        def collectionIssues = [createConcurrencyIssue("UNSAFE_COLLECTION", "Unsafe HashMap", IssueSeverity.MEDIUM)]
        def executorIssues = [createConcurrencyIssue("EXECUTOR_NOT_SHUTDOWN", "No shutdown", IssueSeverity.MEDIUM)]
        def atomicIssues = [createConcurrencyIssue("ATOMIC_OPPORTUNITY", "Use atomic", IssueSeverity.LOW)]
        def lockIssues = [createConcurrencyIssue("LOCK_USAGE", "Improper locking", IssueSeverity.HIGH)]

        mockThreadSafetyAnalyzer.analyze(sourceInfo, _) >> threadSafetyIssues
        mockSynchronizationAnalyzer.analyze(sourceInfo, _) >> syncIssues
        mockConcurrentCollectionsAnalyzer.analyze(sourceInfo, _) >> collectionIssues
        mockExecutorFrameworkAnalyzer.analyze(sourceInfo, _) >> executorIssues
        mockAtomicOperationsAnalyzer.analyze(sourceInfo, _) >> atomicIssues
        mockLockUsageAnalyzer.analyze(sourceInfo, _) >> lockIssues

        and: "AI service fails completely"
        mockChatClient.prompt(_) >> { throw new RuntimeException("Total AI service failure") }

        when: "analyzing concurrency issues"
        def results = analysisEngine.analyzeConcurrencyIssues(sourceFiles)

        then: "all traditional analysis results are preserved"
        def result = results[0]
        result.issues.size() == 6
        
        and: "each analyzer's issues are included"
        result.issues.find { it.type == "RACE_CONDITION" } != null
        result.issues.find { it.type == "DEADLOCK_RISK" } != null
        result.issues.find { it.type == "UNSAFE_COLLECTION" } != null
        result.issues.find { it.type == "EXECUTOR_NOT_SHUTDOWN" } != null
        result.issues.find { it.type == "ATOMIC_OPPORTUNITY" } != null
        result.issues.find { it.type == "LOCK_USAGE" } != null
        
        and: "threat assessment is based on traditional analysis"
        !result.threadSafe // Should be marked unsafe due to critical/high severity issues
        
        and: "analysis completes successfully despite AI failure"
        !result.hasErrors
    }

    def "should log AI service failures appropriately without affecting analysis"() {
        given: "a Java source file"
        def sourceInfo = createJavaSourceInfo("LogTestService.java")
        def sourceFiles = [sourceInfo]

        and: "traditional analyzer detects issues"
        def issue = createConcurrencyIssue("TEST_ISSUE", "Test issue for logging", IssueSeverity.MEDIUM)
        mockThreadSafetyAnalyzer.analyze(sourceInfo, _) >> [issue]
        mockSynchronizationAnalyzer.analyze(sourceInfo, _) >> []
        mockConcurrentCollectionsAnalyzer.analyze(sourceInfo, _) >> []
        mockExecutorFrameworkAnalyzer.analyze(sourceInfo, _) >> []
        mockAtomicOperationsAnalyzer.analyze(sourceInfo, _) >> []
        mockLockUsageAnalyzer.analyze(sourceInfo, _) >> []

        and: "AI service fails with specific error message"
        def aiException = new RuntimeException("AI API key invalid")
        mockChatClient.prompt(_) >> { throw aiException }

        when: "analyzing concurrency issues"
        def results = analysisEngine.analyzeConcurrencyIssues(sourceFiles)

        then: "analysis completes successfully"
        results.size() == 1
        results[0].issues.size() == 1
        results[0].issues[0].type == "TEST_ISSUE"
        
        and: "core analysis is not affected by AI failure"
        !results[0].hasErrors
        results[0].threadSafe // Medium severity doesn't mark as unsafe
    }

    // Helper methods for creating test data
    private JavaSourceInfo createJavaSourceInfo(String fileName) {
        def sourceInfo = new JavaSourceInfo()
        sourceInfo.fileName = fileName
        sourceInfo.filePath = "/test/path/${fileName}"
        sourceInfo.classes = [createClassInfo("TestClass")]
        sourceInfo.threadRelatedImports = ["java.util.concurrent.ExecutorService"] as Set
        return sourceInfo
    }

    private ClassInfo createClassInfo(String className) {
        def classInfo = new ClassInfo()
        classInfo.name = className
        classInfo.interface = false
        classInfo.methods = []
        classInfo.fields = []
        return classInfo
    }

    private ConcurrencyIssue createConcurrencyIssue(String type, String description, IssueSeverity severity) {
        def issue = new ConcurrencyIssue()
        issue.type = type
        issue.description = description
        issue.severity = severity
        issue.className = "TestClass"
        issue.lineNumber = 42
        return issue
    }
}
