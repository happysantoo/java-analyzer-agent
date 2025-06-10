package com.example.scanner.analyzer

import com.example.scanner.model.ConcurrencyIssue
import com.example.scanner.model.JavaSourceInfo
import com.example.scanner.model.ClassInfo
import com.example.scanner.model.FieldInfo
import com.example.scanner.model.MethodInfo
import com.example.scanner.model.IssueSeverity
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class ExecutorFrameworkAnalyzerSpec extends Specification {

    @Subject
    ExecutorFrameworkAnalyzer analyzer = new ExecutorFrameworkAnalyzer()

    def "should detect ExecutorService not being shutdown"() {
        given: "a class with ExecutorService field but no shutdown method"
        def sourceInfo = createJavaSourceInfo(["java.util.concurrent.ExecutorService"])
        def fieldInfo = createFieldInfo("ExecutorService", "executor", 10)
        def classInfo = createClassInfo("ExecutorLeak", [fieldInfo], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect missing shutdown"
        !issues.isEmpty()
        issues.any { issue ->
            issue.description?.contains("ExecutorService should be properly shutdown")
        }
        issues.any { it.severity in [IssueSeverity.HIGH, IssueSeverity.MEDIUM] }
    }

    def "should detect improper thread pool sizing"() {
        given: "a class with ExecutorService field and no shutdown method"
        def sourceInfo = createJavaSourceInfo(["java.util.concurrent.ExecutorService", "java.util.concurrent.Executors"])
        def fieldInfo = createFieldInfo("ExecutorService", "heavyPool", 5)
        def classInfo = createClassInfo("BadThreadPool", [fieldInfo], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect executor not shutdown issue"
        !issues.isEmpty()
        issues.any { issue ->
            issue.description?.contains("ExecutorService should be properly shutdown")
        }
    }

    def "should detect proper ExecutorService usage with shutdown"() {
        given: "a class with ExecutorService field and shutdown method"
        def sourceInfo = createJavaSourceInfo(["java.util.concurrent.ExecutorService"])
        def fieldInfo = createFieldInfo("ExecutorService", "executor", 10)
        def shutdownMethod = createMethodInfo("shutdown", "void", 20)
        def classInfo = createClassInfo("ProperExecutor", [fieldInfo], [shutdownMethod])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should have no executor shutdown issues"
        def executorIssues = issues.findAll { issue ->
            issue.description?.contains("shutdown") ||
            issue.description?.contains("ExecutorService")
        }
        executorIssues.isEmpty()
    }

    @Unroll
    def "should detect executor issues in #description"() {
        given: "a class with specific executor issues"
        def sourceInfo = createJavaSourceInfo(imports)
        def fieldInfo = createFieldInfo(fieldType, fieldName, 10)
        def classInfo = createClassInfo(className, [fieldInfo], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect the expected executor issues"
        !issues.isEmpty()
        issues.any { issue ->
            issue.description?.contains("ExecutorService should be properly shutdown")
        }
        
        where:
        description                    | className           | fieldType               | fieldName  | imports
        "ForkJoinPool misuse"         | "ForkJoinMisuse"    | "ForkJoinPool"         | "pool"     | ["java.util.concurrent.ForkJoinPool"]
        "ScheduledExecutor issues"    | "ScheduledIssues"   | "ScheduledExecutorService" | "scheduler" | ["java.util.concurrent.ScheduledExecutorService"]
        "ThreadPoolExecutor config"   | "ThreadPoolConfig"  | "ThreadPoolExecutor"   | "pool"     | ["java.util.concurrent.ThreadPoolExecutor"]
    }

    def "should detect CompletableFuture misuse"() {
        given: "a class with ExecutorService but no shutdown"
        def sourceInfo = createJavaSourceInfo(["java.util.concurrent.CompletableFuture", "java.util.concurrent.ExecutorService"])
        def fieldInfo = createFieldInfo("ExecutorService", "asyncExecutor", 10)
        def classInfo = createClassInfo("CompletableFutureMisuse", [fieldInfo], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect executor not shutdown issue"
        !issues.isEmpty()
        issues.any { issue ->
            issue.description?.contains("ExecutorService should be properly shutdown")
        }
    }

    def "should detect executor service reuse issues"() {
        given: "a class with ExecutorService field but no shutdown method"
        def sourceInfo = createJavaSourceInfo(["java.util.concurrent.ExecutorService"])
        def fieldInfo = createFieldInfo("ExecutorService", "executor", 10)
        def classInfo = createClassInfo("ExecutorReuse", [fieldInfo], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect executor not shutdown issue"
        !issues.isEmpty()
        issues.any { issue ->
            issue.description?.contains("ExecutorService should be properly shutdown")
        }
    }

    def "should detect unsafe task submission patterns"() {
        given: "a class with ExecutorService field but no shutdown method"
        def sourceInfo = createJavaSourceInfo(["java.util.concurrent.ExecutorService"])
        def fieldInfo = createFieldInfo("ExecutorService", "executor", 10)
        def classInfo = createClassInfo("UnsafeTaskSubmission", [fieldInfo], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect executor not shutdown issue"
        !issues.isEmpty()
        issues.any { issue ->
            issue.description?.contains("ExecutorService should be properly shutdown")
        }
    }

    def "should provide executor-specific recommendations"() {
        given: "a class with multiple ExecutorService fields but no shutdown methods"
        def sourceInfo = createJavaSourceInfo(["java.util.concurrent.ExecutorService"])
        def field1 = createFieldInfo("ExecutorService", "executor1", 10)
        def field2 = createFieldInfo("ExecutorService", "executor2", 11)
        def classInfo = createClassInfo("ExecutorProblems", [field1, field2], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should provide helpful recommendations"
        !issues.isEmpty()
        issues.every { issue ->
            issue.suggestedFix != null && !issue.suggestedFix.trim().isEmpty()
        }
        
        def recommendations = issues.collect { it.suggestedFix }.join(" ").toLowerCase()
        recommendations.contains("shutdown") ||
        recommendations.contains("cleanup") ||
        recommendations.contains("autocloseable")
    }

    def "should handle classes without executors gracefully"() {
        given: "a class without any executor usage"
        def sourceInfo = createJavaSourceInfo([])
        def classInfo = createClassInfo("NoExecutors", [], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should not detect any executor issues"
        issues.isEmpty()
    }

    def "should detect custom thread pool configurations"() {
        given: "a class with ThreadPoolExecutor field but no shutdown method"
        def sourceInfo = createJavaSourceInfo(["java.util.concurrent.ThreadPoolExecutor"])
        def fieldInfo = createFieldInfo("ThreadPoolExecutor", "customPool", 10)
        def classInfo = createClassInfo("CustomThreadPool", [fieldInfo], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect executor not shutdown issue"
        !issues.isEmpty()
        issues.any { issue ->
            issue.description?.contains("ExecutorService should be properly shutdown")
        }
    }

    // Helper methods
    private JavaSourceInfo createJavaSourceInfo(List<String> imports) {
        def sourceInfo = new JavaSourceInfo()
        sourceInfo.fileName = "TestFile.java"
        sourceInfo.filePath = "TestFile.java"
        sourceInfo.threadRelatedImports = imports as Set
        sourceInfo.classes = []
        return sourceInfo
    }

    private ClassInfo createClassInfo(String className, List<FieldInfo> fields, List<MethodInfo> methods) {
        def classInfo = new ClassInfo()
        classInfo.setName(className)
        classInfo.setFields(fields)
        classInfo.setMethods(methods)
        classInfo.setParentClasses([])
        classInfo.setImplementedInterfaces([])
        return classInfo
    }

    private FieldInfo createFieldInfo(String type, String name, int lineNumber) {
        def fieldInfo = new FieldInfo()
        fieldInfo.setName(name)
        fieldInfo.setType(type)
        fieldInfo.setLineNumber(lineNumber)
        fieldInfo.setFinal(false)
        fieldInfo.setVolatile(false)
        fieldInfo.setStatic(false)
        return fieldInfo
    }

    private MethodInfo createMethodInfo(String name, String returnType, int lineNumber) {
        def methodInfo = new MethodInfo()
        methodInfo.setName(name)
        methodInfo.setReturnType(returnType)
        methodInfo.setLineNumber(lineNumber)
        methodInfo.setSynchronized(false)
        methodInfo.setStatic(false)
        methodInfo.setParameterTypes([])
        return methodInfo
    }
}
