package com.example.scanner.service

import com.example.scanner.model.*
import com.example.scanner.config.ThymeleafTestConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.ContextConfiguration
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * Comprehensive tests for template processing and HTML content validation.
 * Tests the actual Thymeleaf template engine with real data scenarios.
 */
@SpringBootTest
@ContextConfiguration(classes = [ThymeleafTestConfiguration])
@TestPropertySource(locations = "classpath:application-test.properties")
class TemplateProcessingValidationSpec extends Specification {

    @Autowired
    TemplateEngine templateEngine

    ThymeleafTemplateProcessor templateProcessor

    @TempDir
    Path tempDir

    def setup() {
        templateProcessor = new ThymeleafTemplateProcessor(templateEngine)
    }

    def "should process concurrency-report template with comprehensive data"() {
        given: "a context with comprehensive report data"
        def context = new Context()
        
        // Create summary data
        def summary = createReportSummary(5, 3, 2, 8, 12)
        context.setVariable("summary", summary)
        
        // Create class reports
        def classReports = createDetailedClassReports()
        context.setVariable("classReports", classReports)
        
        // Create issues and recommendations
        def allIssues = createVariedIssues()
        def recommendations = createVariedRecommendations()
        context.setVariable("allIssues", allIssues)
        context.setVariable("recommendations", recommendations)
        
        // Add metadata
        context.setVariable("generatedAt", "2025-06-19 14:30:15")
        context.setVariable("totalFiles", 5)

        when: "processing the template"
        def htmlOutput = templateProcessor.process("concurrency-report", context)

        then: "should generate complete HTML with all sections"
        htmlOutput != null
        htmlOutput.length() > 2000 // Should be substantial

        and: "should contain summary statistics"
        htmlOutput.contains("Files Analyzed: 5")
        htmlOutput.contains("Thread-Safe Files: 3")
        htmlOutput.contains("Files with Issues: 2")
        htmlOutput.contains("Total Issues Found: 8")
        htmlOutput.contains("Total Recommendations: 12")

        and: "should contain class-level information"
        htmlOutput.contains("UserService")
        htmlOutput.contains("PaymentProcessor")
        htmlOutput.contains("CacheManager")

        and: "should contain issue details with proper formatting"
        htmlOutput.contains("RACE_CONDITION")
        htmlOutput.contains("DEADLOCK_RISK")
        htmlOutput.contains("CRITICAL")
        htmlOutput.contains("HIGH")
        htmlOutput.contains("MEDIUM")

        and: "should contain recommendation details"
        htmlOutput.contains("Use ConcurrentHashMap")
        htmlOutput.contains("Implement proper locking")
        htmlOutput.contains("HIGH")
        htmlOutput.contains("LARGE")

        and: "should have proper HTML structure"
        verifyHtmlStructure(htmlOutput)
    }

    def "should process empty-report template correctly"() {
        given: "context for empty report"
        def context = new Context()
        context.setVariable("isEmpty", true)
        context.setVariable("generatedAt", "2025-06-19 14:30:15")

        when: "processing empty report template"
        def htmlOutput = templateProcessor.process("empty-report", context)

        then: "should generate proper empty report"
        htmlOutput != null
        htmlOutput.contains("No Java Files Found")
        htmlOutput.contains("No Java files were found")
        htmlOutput.contains("2025-06-19 14:30:15")
        verifyHtmlStructure(htmlOutput)
    }

    def "should process error-report template with exception details"() {
        given: "context with error information"
        def context = new Context()
        def exception = new RuntimeException("Parse error in file UserService.java at line 45", 
            new IllegalArgumentException("Unexpected token"))
        
        context.setVariable("error", exception)
        context.setVariable("errorMessage", exception.message)
        context.setVariable("stackTrace", getStackTraceString(exception))
        context.setVariable("generatedAt", "2025-06-19 14:30:15")

        when: "processing error report template"
        def htmlOutput = templateProcessor.process("error-report", context)

        then: "should generate proper error report"
        htmlOutput != null
        htmlOutput.contains("Analysis Error")
        htmlOutput.contains("Parse error in file UserService.java")
        htmlOutput.contains("RuntimeException")
        htmlOutput.contains("2025-06-19 14:30:15")
        verifyHtmlStructure(htmlOutput)
    }

    @Unroll
    def "should handle template variables for #testCase"() {
        given: "context with specific data"
        def context = new Context()
        variables.each { key, value ->
            context.setVariable(key, value)
        }

        when: "processing concurrency-report template"
        def htmlOutput = templateProcessor.process("concurrency-report", context)

        then: "should contain expected content"
        htmlOutput != null
        expectedInOutput.each { expected ->
            assert htmlOutput.contains(expected)
        }

        where:
        testCase                | variables                                    | expectedInOutput
        "zero issues"          | createZeroIssuesContext()                   | ["Total Issues Found: 0", "No concurrency issues"]
        "high severity only"   | createHighSeverityContext()                 | ["CRITICAL", "HIGH", "Total Issues Found: 2"]
        "many recommendations" | createManyRecommendationsContext()          | ["Total Recommendations: 5", "HIGH", "MEDIUM"]
        "single file"          | createSingleFileContext()                   | ["Files Analyzed: 1", "Thread-Safe Files: 1"]
    }

    def "should properly escape HTML characters in template output"() {
        given: "context with special characters"
        def context = new Context()
        
        def issueWithSpecialChars = createConcurrencyIssue("SPECIAL_TEST", IssueSeverity.HIGH,
            "Method <init>() has issue with 'single quotes' and \"double quotes\" & symbols")
        
        def summary = createReportSummary(1, 0, 1, 1, 1)
        context.setVariable("summary", summary)
        context.setVariable("allIssues", [issueWithSpecialChars])
        context.setVariable("recommendations", [])
        context.setVariable("classReports", [])
        context.setVariable("generatedAt", "2025-06-19 14:30:15")
        context.setVariable("totalFiles", 1)

        when: "processing template"
        def htmlOutput = templateProcessor.process("concurrency-report", context)

        then: "should escape special characters properly"
        htmlOutput.contains("&lt;init&gt;")
        htmlOutput.contains("&quot;double quotes&quot;")
        htmlOutput.contains("&amp; symbols")
        !htmlOutput.contains("<init>") // Raw content should be escaped
    }

    def "should handle large datasets in templates without performance issues"() {
        given: "context with large amount of data"
        def context = new Context()
        
        // Create large dataset
        def manyIssues = (1..100).collect { i ->
            createConcurrencyIssue("ISSUE_TYPE_${i}", IssueSeverity.MEDIUM,
                "This is issue number ${i} with detailed description")
        }
        
        def manyRecommendations = (1..50).collect { i ->
            createDetailedRecommendation("Recommendation ${i} for improvement",
                RecommendationPriority.MEDIUM, RecommendationEffort.MEDIUM)
        }
        
        def summary = createReportSummary(20, 15, 5, 100, 50)
        context.setVariable("summary", summary)
        context.setVariable("allIssues", manyIssues)
        context.setVariable("recommendations", manyRecommendations)
        context.setVariable("classReports", [])
        context.setVariable("generatedAt", "2025-06-19 14:30:15")
        context.setVariable("totalFiles", 20)

        when: "processing template with large dataset"
        def startTime = System.currentTimeMillis()
        def htmlOutput = templateProcessor.process("concurrency-report", context)
        def processingTime = System.currentTimeMillis() - startTime

        then: "should complete within reasonable time"
        processingTime < 5000 // Should complete within 5 seconds
        htmlOutput != null
        htmlOutput.length() > 10000 // Should be substantial content
        htmlOutput.contains("Total Issues Found: 100")
        htmlOutput.contains("Total Recommendations: 50")
    }

    def "should validate CSS and JavaScript inclusion in template output"() {
        given: "basic context"
        def context = new Context()
        def summary = createReportSummary(1, 1, 0, 0, 0)
        context.setVariable("summary", summary)
        context.setVariable("allIssues", [])
        context.setVariable("recommendations", [])
        context.setVariable("classReports", [])
        context.setVariable("generatedAt", "2025-06-19 14:30:15")
        context.setVariable("totalFiles", 1)

        when: "processing template"
        def htmlOutput = templateProcessor.process("concurrency-report", context)

        then: "should include CSS styling"
        htmlOutput.contains("<style>")
        htmlOutput.contains("font-family:")
        htmlOutput.contains("background-color:")
        htmlOutput.contains("margin:")
        htmlOutput.contains("padding:")

        and: "should have responsive design elements"
        htmlOutput.contains("max-width:")
        htmlOutput.contains("grid-template-columns:")
        htmlOutput.contains("@media")
    }

    // Helper Methods

    private Object createReportSummary(int totalFiles, int threadSafeClasses, int problematicClasses, 
                                      int totalIssues, int totalRecommendations) {
        // This would match the ReportSummary class structure
        return [
            totalFiles: totalFiles,
            totalClasses: totalFiles, // Assuming each file has one class
            threadSafeClasses: threadSafeClasses,
            problematicClasses: problematicClasses,
            totalIssues: totalIssues,
            totalRecommendations: totalRecommendations,
            criticalIssues: 0,
            highSeverityIssues: 0,
            mediumSeverityIssues: 0,
            lowSeverityIssues: 0
        ]
    }

    private List<Object> createDetailedClassReports() {
        return [
            [
                fileName: "UserService.java",
                filePath: "com/example/UserService.java",
                threadSafe: false,
                issueCount: 3,
                recommendationCount: 2,
                issues: [
                    createConcurrencyIssue("RACE_CONDITION", IssueSeverity.HIGH, "Unsync access to HashMap"),
                    createConcurrencyIssue("DEADLOCK_RISK", IssueSeverity.CRITICAL, "Lock ordering issue")
                ]
            ],
            [
                fileName: "PaymentProcessor.java", 
                filePath: "com/example/PaymentProcessor.java",
                threadSafe: false,
                issueCount: 2,
                recommendationCount: 1,
                issues: [
                    createConcurrencyIssue("UNSAFE_COLLECTION", IssueSeverity.MEDIUM, "ArrayList without sync")
                ]
            ],
            [
                fileName: "CacheManager.java",
                filePath: "com/example/CacheManager.java", 
                threadSafe: true,
                issueCount: 0,
                recommendationCount: 0,
                issues: []
            ]
        ]
    }

    private List<ConcurrencyIssue> createVariedIssues() {
        return [
            createConcurrencyIssue("RACE_CONDITION", IssueSeverity.HIGH, "Shared state access without sync"),
            createConcurrencyIssue("DEADLOCK_RISK", IssueSeverity.CRITICAL, "Potential deadlock detected"),
            createConcurrencyIssue("UNSAFE_COLLECTION", IssueSeverity.MEDIUM, "Non-thread-safe collection usage"),
            createConcurrencyIssue("VOLATILE_MISUSE", IssueSeverity.LOW, "Volatile field in compound operation")
        ]
    }

    private List<ConcurrencyRecommendation> createVariedRecommendations() {
        return [
            createDetailedRecommendation("Use ConcurrentHashMap instead of HashMap",
                RecommendationPriority.HIGH, RecommendationEffort.SMALL),
            createDetailedRecommendation("Implement proper locking strategy",
                RecommendationPriority.CRITICAL, RecommendationEffort.LARGE),
            createDetailedRecommendation("Replace ArrayList with CopyOnWriteArrayList",
                RecommendationPriority.MEDIUM, RecommendationEffort.MEDIUM)
        ]
    }

    private Map<String, Object> createZeroIssuesContext() {
        def summary = createReportSummary(3, 3, 0, 0, 0)
        return [
            summary: summary,
            allIssues: [],
            recommendations: [],
            classReports: [],
            generatedAt: "2025-06-19 14:30:15",
            totalFiles: 3
        ]
    }

    private Map<String, Object> createHighSeverityContext() {
        def issues = [
            createConcurrencyIssue("CRITICAL_ISSUE", IssueSeverity.CRITICAL, "Critical problem"),
            createConcurrencyIssue("HIGH_ISSUE", IssueSeverity.HIGH, "High priority problem")
        ]
        def summary = createReportSummary(2, 0, 2, 2, 2)
        return [
            summary: summary,
            allIssues: issues,
            recommendations: [],
            classReports: [],
            generatedAt: "2025-06-19 14:30:15",
            totalFiles: 2
        ]
    }

    private Map<String, Object> createManyRecommendationsContext() {
        def recommendations = (1..5).collect { i ->
            createDetailedRecommendation("Recommendation ${i}",
                i <= 2 ? RecommendationPriority.HIGH : RecommendationPriority.MEDIUM,
                RecommendationEffort.MEDIUM)
        }
        def summary = createReportSummary(3, 1, 2, 3, 5)
        return [
            summary: summary,
            allIssues: [],
            recommendations: recommendations,
            classReports: [],
            generatedAt: "2025-06-19 14:30:15",
            totalFiles: 3
        ]
    }

    private Map<String, Object> createSingleFileContext() {
        def summary = createReportSummary(1, 1, 0, 0, 0)
        return [
            summary: summary,
            allIssues: [],
            recommendations: [],
            classReports: [],
            generatedAt: "2025-06-19 14:30:15",
            totalFiles: 1
        ]
    }

    private ConcurrencyIssue createConcurrencyIssue(String type, IssueSeverity severity, String description) {
        def issue = new ConcurrencyIssue()
        issue.type = type
        issue.severity = severity
        issue.className = "TestClass"
        issue.lineNumber = 42
        issue.description = description
        return issue
    }

    private ConcurrencyRecommendation createDetailedRecommendation(String description,
                                                                  RecommendationPriority priority,
                                                                  RecommendationEffort effort) {
        def rec = new ConcurrencyRecommendation()
        rec.setDescription(description)
        rec.setPriority(priority)
        rec.setEffort(effort)
        return rec
    }

    private String getStackTraceString(Exception e) {
        def sw = new StringWriter()
        def pw = new PrintWriter(sw)
        e.printStackTrace(pw)
        return sw.toString()
    }

    private void verifyHtmlStructure(String htmlOutput) {
        assert htmlOutput.contains("<!DOCTYPE html>")
        assert htmlOutput.contains("<html")
        assert htmlOutput.contains("</html>")
        assert htmlOutput.contains("<head>")
        assert htmlOutput.contains("</head>")
        assert htmlOutput.contains("<body>")
        assert htmlOutput.contains("</body>")
        
        // Verify some balance of common tags
        def openDivs = htmlOutput.count("<div")
        def closeDivs = htmlOutput.count("</div>")
        assert openDivs == closeDivs : "Unbalanced div tags"
    }
}
