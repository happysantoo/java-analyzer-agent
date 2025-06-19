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
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.regex.Pattern

/**
 * Integration tests for HTML template generation and output verification.
 * Tests actual HTML content generation with real Thymeleaf templates.
 */
@SpringBootTest
@ContextConfiguration(classes = [ThymeleafTestConfiguration])
@TestPropertySource(locations = "classpath:application-test.properties")
class HtmlTemplateGenerationIntegrationSpec extends Specification {

    @Autowired
    TemplateEngine templateEngine

    ConcurrencyReportGenerator generator
    ThymeleafTemplateProcessor templateProcessor

    @TempDir
    Path tempDir

    def setup() {
        templateProcessor = new ThymeleafTemplateProcessor(templateEngine)
        generator = new ConcurrencyReportGenerator(templateProcessor)
    }

    def "should generate complete HTML report with proper structure and content"() {
        given: "comprehensive analysis results with real data"
        def results = createComprehensiveAnalysisResults()
        def outputPath = tempDir.resolve("full-report.html").toString()

        when: "generating HTML report"
        generator.generateHtmlReport(results, outputPath)

        then: "HTML file should be created"
        def reportFile = tempDir.resolve("full-report.html").toFile()
        reportFile.exists()
        reportFile.size() > 1000 // Should be substantial content

        and: "HTML should have proper DOCTYPE and structure"
        def htmlContent = reportFile.text
        htmlContent.startsWith("<!DOCTYPE html>")
        htmlContent.contains("<html")
        htmlContent.contains("</html>")
        htmlContent.contains("<head>")
        htmlContent.contains("<body>")

        and: "should contain report title and metadata"
        htmlContent.contains("Java Concurrency Analysis Report")
        htmlContent.contains("Generated on:")
        htmlContent.contains(LocalDateTime.now().year.toString())

        and: "should contain executive summary section"
        htmlContent.contains("Executive Summary")
        htmlContent.contains("Files Analyzed: 3")
        htmlContent.contains("Thread-Safe Files: 1")
        htmlContent.contains("Files with Issues: 2")
        htmlContent.contains("Total Issues Found: 4")

        and: "should contain class-level reports"
        htmlContent.contains("Class-Level Analysis")
        htmlContent.contains("UserService")
        htmlContent.contains("DataProcessor")
        htmlContent.contains("SafeUtility")

        and: "should contain issue details"
        htmlContent.contains("RACE_CONDITION")
        htmlContent.contains("DEADLOCK_RISK")
        htmlContent.contains("HIGH")
        htmlContent.contains("CRITICAL")

        and: "should contain recommendations"
        htmlContent.contains("Recommendations")
        htmlContent.contains("Use ConcurrentHashMap instead of HashMap")
        htmlContent.contains("Implement proper lock ordering")

        and: "should have proper CSS styling"
        htmlContent.contains("<style>")
        htmlContent.contains("font-family:")
        htmlContent.contains("background-color:")
    }

    def "should generate HTML report with proper issue severity styling"() {
        given: "analysis results with different severity levels"
        def criticalIssue = createConcurrencyIssue("DEADLOCK", IssueSeverity.CRITICAL, 
            "Potential deadlock detected in synchronized methods")
        def highIssue = createConcurrencyIssue("RACE_CONDITION", IssueSeverity.HIGH,
            "Race condition in shared mutable state")
        def mediumIssue = createConcurrencyIssue("UNSAFE_COLLECTION", IssueSeverity.MEDIUM,
            "Using non-thread-safe HashMap in concurrent context")
        def lowIssue = createConcurrencyIssue("OPTIMIZATION", IssueSeverity.LOW,
            "Consider using AtomicInteger for counter")

        def result = createAnalysisResultWithIssues("SeverityTest.java", false, 
            [criticalIssue, highIssue, mediumIssue, lowIssue], [])
        def outputPath = tempDir.resolve("severity-report.html").toString()

        when: "generating HTML report"
        generator.generateHtmlReport([result], outputPath)

        then: "HTML should contain severity-specific styling"
        def htmlContent = tempDir.resolve("severity-report.html").toFile().text
        
        // Should contain all severity levels
        htmlContent.contains("CRITICAL")
        htmlContent.contains("HIGH") 
        htmlContent.contains("MEDIUM")
        htmlContent.contains("LOW")

        and: "should contain issue descriptions"
        htmlContent.contains("Potential deadlock detected")
        htmlContent.contains("Race condition in shared")
        htmlContent.contains("non-thread-safe HashMap")
        htmlContent.contains("Consider using AtomicInteger")
    }

    def "should generate HTML report with recommendation priorities and efforts"() {
        given: "analysis results with detailed recommendations"
        def highPriorityRec = createDetailedRecommendation(
            "Fix critical deadlock issue immediately",
            RecommendationPriority.HIGH,
            RecommendationEffort.LARGE
        )
        def mediumPriorityRec = createDetailedRecommendation(
            "Replace HashMap with ConcurrentHashMap",
            RecommendationPriority.MEDIUM,
            RecommendationEffort.SMALL
        )
        def lowPriorityRec = createDetailedRecommendation(
            "Consider using atomic operations for better performance",
            RecommendationPriority.LOW,
            RecommendationEffort.MEDIUM
        )

        def result = createAnalysisResultWithIssues("RecommendationTest.java", false, 
            [], [highPriorityRec, mediumPriorityRec, lowPriorityRec])
        def outputPath = tempDir.resolve("recommendation-report.html").toString()

        when: "generating HTML report"
        generator.generateHtmlReport([result], outputPath)

        then: "HTML should contain recommendation details"
        def htmlContent = tempDir.resolve("recommendation-report.html").toFile().text

        // Should contain priorities
        htmlContent.contains("HIGH")
        htmlContent.contains("MEDIUM")
        htmlContent.contains("LOW")

        // Should contain effort levels
        htmlContent.contains("LARGE")
        htmlContent.contains("SMALL")

        // Should contain recommendation descriptions
        htmlContent.contains("Fix critical deadlock issue")
        htmlContent.contains("Replace HashMap with ConcurrentHashMap")
        htmlContent.contains("Consider using atomic operations")
    }

    def "should generate empty report with proper message and structure"() {
        given: "empty report generation"
        def outputPath = tempDir.resolve("empty-report.html").toString()

        when: "generating empty report"
        generator.generateEmptyReport(outputPath)

        then: "HTML file should be created"
        def reportFile = tempDir.resolve("empty-report.html").toFile()
        reportFile.exists()

        and: "should contain proper empty report structure"
        def htmlContent = reportFile.text
        htmlContent.startsWith("<!DOCTYPE html>")
        htmlContent.contains("No Java Files Found")
        htmlContent.contains("No Java files were found")
        htmlContent.contains("Generated on:")
    }

    def "should generate error report with exception details"() {
        given: "an exception with stack trace"
        def exception = new RuntimeException("Failed to parse Java file: Syntax error at line 42", 
            new IllegalArgumentException("Invalid syntax in class declaration"))
        def outputPath = tempDir.resolve("error-report.html").toString()

        when: "generating error report"
        generator.generateErrorReport(exception, outputPath)

        then: "HTML file should be created"
        def reportFile = tempDir.resolve("error-report.html").toFile()
        reportFile.exists()

        and: "should contain error details"
        def htmlContent = reportFile.text
        htmlContent.contains("Analysis Error")
        htmlContent.contains("Failed to parse Java file")
        htmlContent.contains("Syntax error at line 42")
        htmlContent.contains("RuntimeException")
    }

    @Unroll
    def "should generate valid HTML for #scenario"() {
        given: "specific analysis scenario"
        def outputPath = tempDir.resolve("${scenario.replace(' ', '-')}-report.html").toString()

        when: "generating report"
        generator.generateHtmlReport(results, outputPath)

        then: "should produce valid HTML structure"
        def htmlContent = tempDir.resolve("${scenario.replace(' ', '-')}-report.html").toFile().text
        verifyValidHtmlStructure(htmlContent)

        and: "should contain expected content"
        expectedContent.each { content ->
            assert htmlContent.contains(content)
        }

        where:
        scenario                    | results                              | expectedContent
        "single thread-safe class" | [createThreadSafeResult()]          | ["Thread-Safe Files: 1", "Total Issues Found: 0"]
        "single unsafe class"       | [createUnsafeResult()]              | ["Files with Issues: 1", "RACE_CONDITION"]
        "multiple mixed classes"    | [createThreadSafeResult(), 
                                      createUnsafeResult()]             | ["Files Analyzed: 2", "Thread-Safe Files: 1"]
        "no issues found"           | [createCleanResult()]               | ["Total Issues Found: 0", "No concurrency issues"]
    }

    def "should handle special characters and escaping in HTML output"() {
        given: "analysis result with special characters"
        def specialIssue = createConcurrencyIssue("SPECIAL_CHARS", IssueSeverity.HIGH,
            "Issue in method <init>() with 'quotes' and \"double quotes\" & ampersands")
        def result = createAnalysisResultWithIssues("SpecialChars.java", false, [specialIssue], [])
        def outputPath = tempDir.resolve("special-chars-report.html").toString()

        when: "generating HTML report"
        generator.generateHtmlReport([result], outputPath)

        then: "HTML should properly escape special characters"
        def htmlContent = tempDir.resolve("special-chars-report.html").toFile().text
        
        // Should contain escaped content (Thymeleaf handles this automatically)
        htmlContent.contains("&lt;init&gt;")
        htmlContent.contains("&quot;double quotes&quot;")
        htmlContent.contains("&amp; ampersands")
    }

    def "should generate reports with proper timestamp formatting"() {
        given: "current time for comparison"
        def beforeGeneration = LocalDateTime.now()
        def results = [createThreadSafeResult()]
        def outputPath = tempDir.resolve("timestamp-report.html").toString()

        when: "generating HTML report"
        generator.generateHtmlReport(results, outputPath)

        then: "should contain properly formatted timestamp"
        def htmlContent = tempDir.resolve("timestamp-report.html").toFile().text
        def afterGeneration = LocalDateTime.now()
        
        // Should contain a timestamp in the expected format
        def timestampPattern = /Generated on: \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/
        htmlContent.find(timestampPattern) != null
    }

    def "should generate HTML with proper meta tags and encoding"() {
        given: "basic analysis results"
        def results = [createThreadSafeResult()]
        def outputPath = tempDir.resolve("meta-tags-report.html").toString()

        when: "generating HTML report"
        generator.generateHtmlReport(results, outputPath)

        then: "HTML should contain proper meta tags"
        def htmlContent = tempDir.resolve("meta-tags-report.html").toFile().text
        
        htmlContent.contains('<meta charset="UTF-8">')
        htmlContent.contains('<meta name="viewport"')
        htmlContent.contains('lang="en"')
    }

    // Helper Methods

    private List<AnalysisResult> createComprehensiveAnalysisResults() {
        def issue1 = createConcurrencyIssue("RACE_CONDITION", IssueSeverity.HIGH,
            "Unsynchronized access to shared HashMap in UserService")
        def issue2 = createConcurrencyIssue("DEADLOCK_RISK", IssueSeverity.CRITICAL,
            "Potential deadlock between lock1 and lock2")
        def issue3 = createConcurrencyIssue("UNSAFE_COLLECTION", IssueSeverity.MEDIUM,
            "ArrayList used without synchronization")
        def issue4 = createConcurrencyIssue("VOLATILE_MISUSE", IssueSeverity.LOW,
            "Volatile field used in compound operation")

        def rec1 = createDetailedRecommendation("Use ConcurrentHashMap instead of HashMap",
            RecommendationPriority.HIGH, RecommendationEffort.SMALL)
        def rec2 = createDetailedRecommendation("Implement proper lock ordering to prevent deadlocks",                RecommendationPriority.HIGH, RecommendationEffort.LARGE)
        def rec3 = createDetailedRecommendation("Replace ArrayList with Collections.synchronizedList() or use CopyOnWriteArrayList",
            RecommendationPriority.MEDIUM, RecommendationEffort.MEDIUM)

        return [
            createAnalysisResultWithDetails("UserService.java", false, [issue1, issue2], [rec1, rec2]),
            createAnalysisResultWithDetails("DataProcessor.java", false, [issue3, issue4], [rec3]),
            createAnalysisResultWithDetails("SafeUtility.java", true, [], [])
        ]
    }

    private AnalysisResult createAnalysisResultWithDetails(String fileName, boolean threadSafe,
                                                          List<ConcurrencyIssue> issues,
                                                          List<ConcurrencyRecommendation> recommendations) {
        def result = new AnalysisResult()
        result.filePath = Paths.get(fileName)
        result.analyzedClasses = 2
        result.threadSafe = threadSafe
        result.hasErrors = false
        result.issues = issues ?: []
        result.recommendations = recommendations ?: []
        return result
    }

    private AnalysisResult createAnalysisResultWithIssues(String fileName, boolean threadSafe,
                                                         List<ConcurrencyIssue> issues,
                                                         List<ConcurrencyRecommendation> recommendations) {
        return createAnalysisResultWithDetails(fileName, threadSafe, issues, recommendations)
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

    private AnalysisResult createThreadSafeResult() {
        def result = new AnalysisResult()
        result.filePath = Paths.get("ThreadSafeClass.java")
        result.analyzedClasses = 1
        result.threadSafe = true
        result.hasErrors = false
        result.issues = []
        result.recommendations = []
        return result
    }

    private AnalysisResult createUnsafeResult() {
        def issue = createConcurrencyIssue("RACE_CONDITION", IssueSeverity.HIGH,
            "Shared mutable state without synchronization")
        def rec = createDetailedRecommendation("Add synchronization or use thread-safe alternatives",
            RecommendationPriority.HIGH, RecommendationEffort.MEDIUM)

        def result = new AnalysisResult()
        result.filePath = Paths.get("UnsafeClass.java")
        result.analyzedClasses = 1
        result.threadSafe = false
        result.hasErrors = false
        result.issues = [issue]
        result.recommendations = [rec]
        return result
    }

    private AnalysisResult createCleanResult() {
        def result = new AnalysisResult()
        result.filePath = Paths.get("CleanClass.java")
        result.analyzedClasses = 1
        result.threadSafe = true
        result.hasErrors = false
        result.issues = []
        result.recommendations = []
        return result
    }

    private void verifyValidHtmlStructure(String htmlContent) {
        // Basic HTML structure validation
        assert htmlContent.contains("<!DOCTYPE html>")
        assert htmlContent.contains("<html")
        assert htmlContent.contains("</html>")
        assert htmlContent.contains("<head>")
        assert htmlContent.contains("</head>")
        assert htmlContent.contains("<body>")
        assert htmlContent.contains("</body>")
        assert htmlContent.contains("<title>")
        
        // Check for balanced tags (simplified check)
        def openDivs = htmlContent.count("<div")
        def closeDivs = htmlContent.count("</div>")
        assert openDivs == closeDivs : "Unbalanced div tags: ${openDivs} open, ${closeDivs} close"
    }
}
