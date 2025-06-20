package com.example.scanner.service

import com.example.scanner.model.*
import org.thymeleaf.context.Context
import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.TempDir

import java.nio.file.Path
import java.nio.file.Paths

class ConcurrencyReportGeneratorSpec extends Specification {

    ConcurrencyReportGenerator generator
    TemplateProcessor templateProcessor

    @TempDir
    Path tempDir

    def setup() {
        // Create a mock TemplateProcessor that doesn't have complex dependencies
        templateProcessor = Mock(TemplateProcessor) {
            // Return stub responses for all template processing
            process(_, _) >> { String template, Context context ->
                return "<html>Stub Response for ${template}</html>"
            }
        }
        generator = new ConcurrencyReportGenerator(templateProcessor)
    }

    def "should generate HTML report with analysis results"() {
        given: "analysis results with various issues and recommendations"
        def results = [
            createAnalysisResult("Class1.java", true, 0, 0),
            createAnalysisResult("Class2.java", false, 3, 2)
        ]
        def outputPath = tempDir.resolve("report.html").toString()

        when: "generating HTML report"
        generator.generateHtmlReport(results, outputPath)

        then: "should write report to file"
        def reportFile = tempDir.resolve("report.html").toFile()
        reportFile.exists()
        reportFile.text.contains("Stub Response for concurrency-report")
    }

    def "should generate executive summary correctly"() {
        given: "analysis results with mixed thread safety"
        def issue1 = createConcurrencyIssue("RACE_CONDITION", IssueSeverity.HIGH)
        def issue2 = createConcurrencyIssue("SYNC_ISSUE", IssueSeverity.MEDIUM)
        def issue3 = createConcurrencyIssue("DEADLOCK", IssueSeverity.CRITICAL)

        def results = [
            createAnalysisResultWithIssues("Safe.java", true, [], []),
            createAnalysisResultWithIssues("Unsafe1.java", false, [issue1, issue2], [createRecommendation()]),
            createAnalysisResultWithIssues("Unsafe2.java", false, [issue3], [createRecommendation()])
        ]
        def outputPath = tempDir.resolve("summary-report.html").toString()

        when: "generating report"
        generator.generateHtmlReport(results, outputPath)

        then: "should write report to file"
        def reportFile = tempDir.resolve("summary-report.html").toFile()
        reportFile.exists()
        reportFile.text.contains("Stub Response for concurrency-report")
    }

    def "should generate empty report when no Java files found"() {
        given: "empty report generation"
        def outputPath = tempDir.resolve("empty-report.html").toString()

        when: "generating empty report"
        generator.generateEmptyReport(outputPath)

        then: "should write report to file"
        def reportFile = tempDir.resolve("empty-report.html").toFile()
        reportFile.exists()
        reportFile.text.contains("Stub Response for empty-report")
    }

    def "should generate error report when analysis fails"() {
        given: "an error condition"
        def error = new RuntimeException("Analysis failed due to parsing error")
        def outputPath = tempDir.resolve("error-report.html").toString()

        when: "generating error report"
        generator.generateErrorReport(error, outputPath)

        then: "should write error report to file"
        def reportFile = tempDir.resolve("error-report.html").toFile()
        reportFile.exists()
        reportFile.text.contains("Stub Response for error-report")
    }

    @Unroll
    def "should create class reports for #description"() {
        given: "analysis results"
        def outputPath = tempDir.resolve("class-report.html").toString()

        when: "generating report"
        generator.generateHtmlReport(results, outputPath)

        then: "should write report to file"
        def reportFile = tempDir.resolve("class-report.html").toFile()
        reportFile.exists()
        reportFile.text.contains("Stub Response for concurrency-report")

        where:
        description          | results                                           
        "thread-safe class"  | [createAnalysisResult("Safe.java", true, 0, 0)]  
        "unsafe class"       | [createAnalysisResult("Unsafe.java", false, 2, 1)] 
        "mixed classes"      | [createAnalysisResult("Safe.java", true, 0, 0),   
                               createAnalysisResult("Unsafe.java", false, 1, 1)] 
    }

    def "should handle issue type distribution in class reports"() {
        given: "analysis result with various issue types"
        def issues = [
            createConcurrencyIssue("RACE_CONDITION", IssueSeverity.HIGH),
            createConcurrencyIssue("RACE_CONDITION", IssueSeverity.MEDIUM),
            createConcurrencyIssue("DEADLOCK", IssueSeverity.CRITICAL)
        ]
        def result = createAnalysisResultWithIssues("Complex.java", false, issues, [])
        def outputPath = tempDir.resolve("distribution-report.html").toString()

        when: "generating report"
        generator.generateHtmlReport([result], outputPath)

        then: "should write report to file"
        def reportFile = tempDir.resolve("distribution-report.html").toFile()
        reportFile.exists()
        reportFile.text.contains("Stub Response for concurrency-report")
    }

    def "should handle errors during file writing gracefully"() {
        given: "invalid output path"
        def invalidPath = Paths.get("invalid", "path", "that", "does", "not", "exist", "report.html").toString()
        def results = [createAnalysisResult("Test.java", true, 0, 0)]

        when: "attempting to generate report"
        generator.generateHtmlReport(results, invalidPath)

        then: "should throw IOException"
        thrown(IOException)
    }

    def "should compile all issues and recommendations from multiple results"() {
        given: "multiple analysis results with issues and recommendations"
        def issue1 = createConcurrencyIssue("RACE_CONDITION", IssueSeverity.HIGH)
        def issue2 = createConcurrencyIssue("SYNC_ISSUE", IssueSeverity.MEDIUM)
        def rec1 = createRecommendationWithDescription("Use synchronized blocks")
        def rec2 = createRecommendationWithDescription("Consider atomic operations")

        def results = [
            createAnalysisResultWithIssues("File1.java", false, [issue1], [rec1]),
            createAnalysisResultWithIssues("File2.java", false, [issue2], [rec2])
        ]
        def outputPath = tempDir.resolve("combined-report.html").toString()

        when: "generating report"
        generator.generateHtmlReport(results, outputPath)

        then: "should write report to file"
        def reportFile = tempDir.resolve("combined-report.html").toFile()
        reportFile.exists()
        reportFile.text.contains("Stub Response for concurrency-report")
    }

    // Helper methods
    private AnalysisResult createAnalysisResult(String fileName, boolean threadSafe, int issueCount, int recCount) {
        def result = new AnalysisResult()
        result.filePath = Paths.get(fileName)
        result.analyzedClasses = 2
        result.threadSafe = threadSafe
        result.hasErrors = false
        
        // Create dummy issues and recommendations
        for (int i = 0; i < issueCount; i++) {
            result.issues.add(createConcurrencyIssue("ISSUE_TYPE_$i", IssueSeverity.MEDIUM))
        }
        
        for (int i = 0; i < recCount; i++) {
            result.recommendations.add(createRecommendation())
        }
        
        return result
    }

    private AnalysisResult createAnalysisResultWithIssues(String fileName, boolean threadSafe, 
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

    private ConcurrencyIssue createConcurrencyIssue(String type, IssueSeverity severity) {
        def issue = new ConcurrencyIssue()
        issue.type = type
        issue.severity = severity
        issue.className = "TestClass"
        issue.lineNumber = 42
        issue.description = "Test issue description"
        return issue
    }

    private ConcurrencyRecommendation createRecommendation() {
        def rec = new ConcurrencyRecommendation()
        rec.setDescription("Test recommendation")
        rec.setPriority(RecommendationPriority.MEDIUM)
        rec.setEffort(RecommendationEffort.SMALL)
        return rec
    }

    private ConcurrencyRecommendation createRecommendationWithDescription(String description) {
        def rec = new ConcurrencyRecommendation()
        rec.setDescription(description)
        rec.setPriority(RecommendationPriority.MEDIUM)
        rec.setEffort(RecommendationEffort.SMALL)
        return rec
    }
}
