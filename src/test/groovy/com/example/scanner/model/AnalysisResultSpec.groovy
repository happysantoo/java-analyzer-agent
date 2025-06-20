package com.example.scanner.model

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Paths

class AnalysisResultSpec extends Specification {

    def "should create AnalysisResult with default values"() {
        when: "creating new AnalysisResult"
        def result = new AnalysisResult()

        then: "should have default values"
        result.filePath == null
        result.directoryPath == null
        result.reportFilePath == null
        result.scanStatistics == null
        result.analyzedClasses == 0
        !result.threadSafe
        !result.hasErrors
        result.errorMessage == null
        result.issues.isEmpty()
        result.recommendations.isEmpty()
    }

    def "should set and get file path correctly"() {
        given: "analysis result and file path"
        def result = new AnalysisResult()
        def filePath = Paths.get("test", "path", "TestClass.java")

        when: "setting file path"
        result.setFilePath(filePath)

        then: "should return correct file path"
        result.getFilePath() == filePath
    }

    def "should set and get directory path correctly"() {
        given: "analysis result and directory path"
        def result = new AnalysisResult()
        def directoryPath = Paths.get("test", "project").toString()

        when: "setting directory path"
        result.setDirectoryPath(directoryPath)

        then: "should return correct directory path"
        result.getDirectoryPath() == directoryPath
    }

    def "should set and get scan statistics correctly"() {
        given: "analysis result and scan statistics"
        def result = new AnalysisResult()
        def stats = new ScanStatistics()
        stats.setTotalJavaFiles(5)
        stats.setTotalIssuesFound(3)

        when: "setting scan statistics"
        result.setScanStatistics(stats)

        then: "should return correct scan statistics"
        result.getScanStatistics() == stats
        result.getScanStatistics().getTotalJavaFiles() == 5
        result.getScanStatistics().getTotalIssuesFound() == 3
    }

    def "should set and get thread safety status correctly"() {
        given: "analysis result"
        def result = new AnalysisResult()

        when: "setting thread safe to true"
        result.setThreadSafe(true)

        then: "should return true"
        result.isThreadSafe()

        when: "setting thread safe to false"
        result.setThreadSafe(false)

        then: "should return false"
        !result.isThreadSafe()
    }

    def "should set and get error status correctly"() {
        given: "analysis result"
        def result = new AnalysisResult()

        when: "setting has errors to true"
        result.setHasErrors(true)

        then: "should return true"
        result.isHasErrors()

        when: "setting has errors to false"
        result.setHasErrors(false)

        then: "should return false"
        !result.isHasErrors()
    }

    def "should set and get error message correctly"() {
        given: "analysis result and error message"
        def result = new AnalysisResult()
        def errorMessage = "Analysis failed due to parsing error"

        when: "setting error message"
        result.setErrorMessage(errorMessage)

        then: "should return correct error message"
        result.getErrorMessage() == errorMessage
    }

    def "should handle concurrency issues correctly"() {
        given: "analysis result and concurrency issues"
        def result = new AnalysisResult()
        def issue1 = createConcurrencyIssue("RACE_CONDITION")
        def issue2 = createConcurrencyIssue("DEADLOCK")
        def issues = [issue1, issue2]

        when: "setting issues"
        result.setIssues(issues)

        then: "should return correct issues"
        result.getIssues() == issues
        result.getIssues().size() == 2
        result.getConcurrencyIssues() == issues // alias method
    }

    def "should handle recommendations correctly"() {
        given: "analysis result and recommendations"
        def result = new AnalysisResult()
        def rec1 = createRecommendation("Use synchronized blocks")
        def rec2 = createRecommendation("Consider atomic operations")
        def recommendations = [rec1, rec2]

        when: "setting recommendations"
        result.setRecommendations(recommendations)

        then: "should return correct recommendations"
        result.getRecommendations() == recommendations
        result.getRecommendations().size() == 2
    }

    def "should set and get analyzed classes count correctly"() {
        given: "analysis result"
        def result = new AnalysisResult()

        when: "setting analyzed classes count"
        result.setAnalyzedClasses(42)

        then: "should return correct count"
        result.getAnalyzedClasses() == 42
    }

    def "should set and get report file path correctly"() {
        given: "analysis result and report path"
        def result = new AnalysisResult()
        def reportPath = Paths.get("output", "concurrency-report.html").toString()

        when: "setting report file path"
        result.setReportFilePath(reportPath)

        then: "should return correct report path"
        result.getReportFilePath() == reportPath
    }

    @Unroll
    def "should handle complex analysis result with #description"() {
        given: "complex analysis result"
        def result = new AnalysisResult()
        result.setFilePath(Paths.get(filePath))
        result.setDirectoryPath(directoryPath)
        result.setAnalyzedClasses(analyzedClasses)
        result.setThreadSafe(threadSafe)
        result.setHasErrors(hasErrors)

        if (hasErrors) {
            result.setErrorMessage(errorMessage)
        }

        def issues = []
        for (int i = 0; i < issueCount; i++) {
            issues.add(createConcurrencyIssue("ISSUE_$i"))
        }
        result.setIssues(issues)

        def recommendations = []
        for (int i = 0; i < recommendationCount; i++) {
            recommendations.add(createRecommendation("Recommendation $i"))
        }
        result.setRecommendations(recommendations)

        when: "checking all properties"
        def allPropertiesCorrect = (
            result.getFilePath().toString() == filePath &&
            result.getDirectoryPath() == directoryPath &&
            result.getAnalyzedClasses() == analyzedClasses &&
            result.isThreadSafe() == threadSafe &&
            result.isHasErrors() == hasErrors &&
            result.getIssues().size() == issueCount &&
            result.getRecommendations().size() == recommendationCount
        )

        then: "all properties should be set correctly"
        allPropertiesCorrect

        where:
        description           | filePath              | directoryPath | analyzedClasses | threadSafe | hasErrors | errorMessage        | issueCount | recommendationCount
        "thread-safe class"   | "/test/Safe.java"     | "/test"       | 1              | true       | false     | null               | 0          | 0
        "unsafe class"        | "/test/Unsafe.java"   | "/test"       | 2              | false      | false     | null               | 3          | 2
        "error case"          | "/test/Error.java"    | "/test"       | 0              | false      | true      | "Parsing failed"   | 0          | 0
        "mixed case"          | "/test/Mixed.java"    | "/test"       | 5              | false      | false     | null               | 2          | 4
    }

    def "should initialize lists properly to avoid null pointer exceptions"() {
        given: "new analysis result"
        def result = new AnalysisResult()

        when: "accessing lists directly"
        def issuesSize = result.getIssues().size()
        def recommendationsSize = result.getRecommendations().size()

        then: "should not throw null pointer exception"
        issuesSize == 0
        recommendationsSize == 0
        result.getIssues() != null
        result.getRecommendations() != null
    }

    def "should handle null values gracefully"() {
        given: "analysis result"
        def result = new AnalysisResult()

        when: "setting null values"
        result.setFilePath(null)
        result.setDirectoryPath(null)
        result.setErrorMessage(null)
        result.setScanStatistics(null)

        then: "should handle null values without exceptions"
        result.getFilePath() == null
        result.getDirectoryPath() == null
        result.getErrorMessage() == null
        result.getScanStatistics() == null
    }

    def "should maintain list references correctly"() {
        given: "analysis result with initial lists"
        def result = new AnalysisResult()
        def originalIssues = result.getIssues()
        def originalRecommendations = result.getRecommendations()

        when: "adding items to lists"
        originalIssues.add(createConcurrencyIssue("TEST_ISSUE"))
        originalRecommendations.add(createRecommendation("Test recommendation"))

        then: "original lists should be modified"
        result.getIssues().size() == 1
        result.getRecommendations().size() == 1
        result.getIssues().is(originalIssues)
        result.getRecommendations().is(originalRecommendations)
    }

    // Helper methods
    private ConcurrencyIssue createConcurrencyIssue(String type) {
        def issue = new ConcurrencyIssue()
        issue.setType(type)
        issue.setClassName("TestClass")
        issue.setLineNumber(42)
        issue.setSeverity(IssueSeverity.MEDIUM)
        issue.setDescription("Test issue description")
        return issue
    }

    private ConcurrencyRecommendation createRecommendation(String description) {
        def rec = new ConcurrencyRecommendation()
        rec.setDescription(description)
        rec.setPriority(RecommendationPriority.MEDIUM)
        rec.setEffort(RecommendationEffort.SMALL)
        return rec
    }
}
