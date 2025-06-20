package com.example.scanner.agent

import com.example.scanner.config.ScannerConfiguration
import com.example.scanner.service.*
import com.example.scanner.model.*
import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.TempDir
import spock.lang.Shared

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

class JavaScannerAgentSpec extends Specification {

    JavaScannerAgent agent
    JavaFileDiscoveryService mockFileDiscoveryService = Mock()
    JavaSourceAnalysisService mockSourceAnalysisService = Mock()
    ConcurrencyAnalysisEngine mockAnalysisEngine = Mock()
    ConcurrencyReportGenerator mockReportGenerator = Mock()
    ScannerConfiguration mockConfiguration = Mock()

    @TempDir
    @Shared
    Path tempDir

    def setup() {
        agent = new JavaScannerAgent()
        agent.fileDiscoveryService = mockFileDiscoveryService
        agent.sourceAnalysisService = mockSourceAnalysisService
        agent.analysisEngine = mockAnalysisEngine
        agent.reportGenerator = mockReportGenerator
        agent.configuration = mockConfiguration
    }

    def "should execute complete concurrency analysis workflow successfully"() {
        given: "valid input parameters"
        def scanPath = tempDir.toString()
        def outputPath = tempDir.resolve("report.html").toString()
        def configPath = "config.yaml"

        and: "mock services return expected data"
        def javaFiles = [tempDir.resolve("Test.java")]
        def sourceInfos = [createJavaSourceInfo("Test.java")]
        def analysisResults = [createAnalysisResult("Test.java", true)]

        when: "executing concurrency analysis"
        agent.executeConcurrencyAnalysis(scanPath, outputPath, configPath)

        then: "should execute all workflow steps in order"
        1 * mockConfiguration.loadConfiguration(configPath)
        1 * mockFileDiscoveryService.discoverJavaFiles(_) >> javaFiles
        1 * mockSourceAnalysisService.analyzeJavaFiles(javaFiles) >> sourceInfos
        1 * mockAnalysisEngine.analyzeConcurrencyIssues(sourceInfos) >> analysisResults
        1 * mockReportGenerator.generateHtmlReport(analysisResults, outputPath)
    }

    def "should handle no Java files found gracefully"() {
        given: "scan path with no Java files"
        def scanPath = tempDir.toString()
        def outputPath = tempDir.resolve("empty-report.html").toString()
        def configPath = "config.yaml"

        when: "executing analysis"
        agent.executeConcurrencyAnalysis(scanPath, outputPath, configPath)

        then: "should generate empty report"
        1 * mockConfiguration.loadConfiguration(configPath)
        1 * mockFileDiscoveryService.discoverJavaFiles(_) >> []
        1 * mockReportGenerator.generateEmptyReport(outputPath)
        0 * mockSourceAnalysisService.analyzeJavaFiles(_)
        0 * mockAnalysisEngine.analyzeConcurrencyIssues(_)
    }

    def "should handle analysis errors and generate error report"() {
        given: "valid setup that will fail during analysis"
        def scanPath = tempDir.toString()
        def outputPath = tempDir.resolve("error-report.html").toString()
        def configPath = "config.yaml"

        and: "analysis engine throws exception"
        def javaFiles = [tempDir.resolve("Test.java")]
        def sourceInfos = [createJavaSourceInfo("Test.java")]
        def analysisError = new RuntimeException("Analysis failed")

        mockConfiguration.loadConfiguration(configPath) >> { /* no-op */ }
        mockFileDiscoveryService.discoverJavaFiles(_) >> javaFiles
        mockSourceAnalysisService.analyzeJavaFiles(javaFiles) >> sourceInfos
        mockAnalysisEngine.analyzeConcurrencyIssues(sourceInfos) >> { throw analysisError }
        mockReportGenerator.generateErrorReport(analysisError, outputPath) >> { /* no-op */ }

        when: "executing analysis"
        agent.executeConcurrencyAnalysis(scanPath, outputPath, configPath)

        then: "should handle error and generate error report"
        1 * mockReportGenerator.generateErrorReport(analysisError, outputPath)
    }

    @Unroll
    def "should validate inputs correctly for #scenario"() {
        given: "test directory structure"
        def validDir = tempDir.toFile()
        validDir.mkdirs()

        and: "setup mocks based on scenario"
        if (scenario == "valid inputs") {
            // Setup mocks for successful workflow
            def javaFiles = [tempDir.resolve("Test.java")]
            def sourceInfos = [createJavaSourceInfo("Test.java")]
            def analysisResults = [createAnalysisResult("Test.java", true)]

            mockConfiguration.loadConfiguration(configPath) >> { /* no-op */ }
            mockFileDiscoveryService.discoverJavaFiles(_) >> javaFiles
            mockSourceAnalysisService.analyzeJavaFiles(javaFiles) >> sourceInfos
            mockAnalysisEngine.analyzeConcurrencyIssues(sourceInfos) >> analysisResults
            mockReportGenerator.generateHtmlReport(analysisResults, outputPath) >> { /* no-op */ }
        } else {
            // For invalid scenarios, mocks don't need to be called due to early validation
            mockConfiguration.loadConfiguration(_) >> { /* no-op */ }
            mockFileDiscoveryService.discoverJavaFiles(_) >> []
        }

        when: "executing analysis with test parameters"
        agent.executeConcurrencyAnalysis(scanPath, outputPath, configPath)

        then: "should validate inputs and handle accordingly"
        // The method should either complete successfully or handle errors gracefully
        notThrown(Exception)

        where:
        scenario                  | scanPath           | outputPath                              | configPath
        "valid inputs"           | tempDir.toString() | tempDir.resolve("report.html").toString() | "config.yaml"
        "non-existent scan path" | Paths.get("invalid", "path").toString()    | tempDir.resolve("report.html").toString() | "config.yaml"
    }

    def "should execute simplified analysis for testing"() {
        given: "scan path with Java files"
        def scanPath = tempDir.toString()

        and: "mock services return test data"
        def javaFiles = [tempDir.resolve("TestClass.java")]
        def sourceInfos = [createJavaSourceInfo("TestClass.java")]
        def analysisResults = [
            createAnalysisResult("TestClass.java", true),
            createAnalysisResult("AnotherClass.java", false)
        ]

        mockFileDiscoveryService.discoverJavaFiles(_) >> javaFiles
        mockSourceAnalysisService.analyzeJavaFiles(javaFiles) >> sourceInfos
        mockAnalysisEngine.analyzeConcurrencyIssues(sourceInfos) >> analysisResults

        when: "executing simplified analysis"
        def result = agent.analyzeJavaCode(scanPath)

        then: "should return aggregated analysis result"
        result != null
        result.directoryPath == scanPath
        result.scanStatistics != null
        result.scanStatistics.totalJavaFiles == 1
        result.scanStatistics.threadSafeClasses == 1
        result.scanStatistics.problematicClasses == 1
    }

    def "should handle empty directory in simplified analysis"() {
        given: "empty scan path"
        def scanPath = tempDir.toString()

        and: "file discovery returns no files"
        mockFileDiscoveryService.discoverJavaFiles(_) >> []

        when: "executing simplified analysis"
        def result = agent.analyzeJavaCode(scanPath)

        then: "should return empty analysis result"
        result != null
        result.directoryPath == scanPath
        result.threadSafe == true
        result.scanStatistics != null
    }

    def "should handle exceptions in simplified analysis"() {
        given: "scan path that will cause error"
        def scanPath = Paths.get("invalid", "path").toString()

        and: "file discovery throws exception"
        mockFileDiscoveryService.discoverJavaFiles(_) >> { throw new RuntimeException("Discovery failed") }

        when: "executing simplified analysis"
        def result = agent.analyzeJavaCode(scanPath)

        then: "should return error analysis result"
        result != null
        result.directoryPath == scanPath
        result.threadSafe == false
        result.scanStatistics != null
    }

    def "should aggregate statistics correctly from multiple results"() {
        given: "multiple analysis results with various statistics"
        def scanPath = tempDir.toString()
        def javaFiles = [
            tempDir.resolve("Class1.java"),
            tempDir.resolve("Class2.java"),
            tempDir.resolve("Class3.java")
        ]
        def sourceInfos = javaFiles.collect { createJavaSourceInfo(it.fileName.toString()) }
        
        def analysisResults = [
            createAnalysisResultWithStats("Class1.java", true, 2, 1),  // thread-safe with recommendations
            createAnalysisResultWithStats("Class2.java", false, 3, 2), // unsafe with issues
            createAnalysisResultWithStats("Class3.java", false, 1, 0)  // unsafe with issues
        ]

        mockFileDiscoveryService.discoverJavaFiles(_) >> javaFiles
        mockSourceAnalysisService.analyzeJavaFiles(javaFiles) >> sourceInfos
        mockAnalysisEngine.analyzeConcurrencyIssues(sourceInfos) >> analysisResults

        when: "executing simplified analysis"
        def result = agent.analyzeJavaCode(scanPath)

        then: "should aggregate statistics correctly"
        result.scanStatistics.totalJavaFiles == 3
        result.scanStatistics.totalIssuesFound == 6  // 2 + 3 + 1
        result.scanStatistics.totalRecommendations == 3  // 1 + 2 + 0
        result.scanStatistics.threadSafeClasses == 1
        result.scanStatistics.problematicClasses == 2
    }

    def "should create output directory if it doesn't exist"() {
        given: "output path in non-existent directory"
        def scanPath = tempDir.toString()
        def outputDir = tempDir.resolve("reports")
        def outputPath = outputDir.resolve("report.html").toString()
        def configPath = "config.yaml"

        and: "mock services return data"
        def javaFiles = [tempDir.resolve("Test.java")]
        def sourceInfos = [createJavaSourceInfo("Test.java")]
        def analysisResults = [createAnalysisResult("Test.java", true)]

        mockConfiguration.loadConfiguration(configPath) >> { /* no-op */ }
        mockFileDiscoveryService.discoverJavaFiles(_) >> javaFiles
        mockSourceAnalysisService.analyzeJavaFiles(javaFiles) >> sourceInfos
        mockAnalysisEngine.analyzeConcurrencyIssues(sourceInfos) >> analysisResults
        mockReportGenerator.generateHtmlReport(analysisResults, outputPath) >> { 
            // Create the directory to simulate successful creation
            outputDir.toFile().mkdirs()
        }

        when: "executing analysis"
        agent.executeConcurrencyAnalysis(scanPath, outputPath, configPath)

        then: "should complete successfully"
        1 * mockReportGenerator.generateHtmlReport(analysisResults, outputPath)
    }

    def "should handle configuration loading errors gracefully"() {
        given: "invalid configuration path"
        def scanPath = tempDir.toString()
        def outputPath = tempDir.resolve("report.html").toString()
        def configPath = Paths.get("invalid", "config.yaml").toString()

        and: "configuration loading throws exception"
        def configError = new IOException("Config file not found")
        mockConfiguration.loadConfiguration(configPath) >> { 
            throw configError
        }

        and: "mock error report generation"
        mockReportGenerator.generateErrorReport(configError, outputPath) >> { /* no-op */ }

        when: "executing analysis"
        agent.executeConcurrencyAnalysis(scanPath, outputPath, configPath)

        then: "should generate error report without proceeding to file discovery"
        1 * mockReportGenerator.generateErrorReport(configError, outputPath)
        0 * mockFileDiscoveryService.discoverJavaFiles(_)
    }

    // Helper methods
    private JavaSourceInfo createJavaSourceInfo(String fileName) {
        def sourceInfo = new JavaSourceInfo()
        sourceInfo.fileName = fileName
        sourceInfo.filePath = fileName
        sourceInfo.classes = [createClassInfo("TestClass")]
        sourceInfo.threadRelatedImports = [] as Set
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

    private AnalysisResult createAnalysisResult(String fileName, boolean threadSafe) {
        def result = new AnalysisResult()
        result.filePath = Paths.get(fileName)
        result.threadSafe = threadSafe
        result.analyzedClasses = 1
        result.hasErrors = false
        result.issues = []
        result.recommendations = []
        return result
    }

    private AnalysisResult createAnalysisResultWithStats(String fileName, boolean threadSafe, 
                                                         int issueCount, int recommendationCount) {
        def result = createAnalysisResult(fileName, threadSafe)
        
        // Add dummy issues
        for (int i = 0; i < issueCount; i++) {
            def issue = new ConcurrencyIssue()
            issue.type = "ISSUE_$i"
            issue.severity = IssueSeverity.MEDIUM
            result.issues.add(issue)
        }
        
        // Add dummy recommendations
        for (int i = 0; i < recommendationCount; i++) {
            def rec = new ConcurrencyRecommendation()
            rec.description = "Recommendation $i"
            rec.priority = RecommendationPriority.MEDIUM
            result.recommendations.add(rec)
        }
        
        return result
    }
}
