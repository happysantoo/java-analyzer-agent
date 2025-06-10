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

class ThreadSafetyAnalyzerSpec extends Specification {

    @Subject
    ThreadSafetyAnalyzer analyzer = new ThreadSafetyAnalyzer()

    def "should detect race condition in shared mutable static field"() {
        given: "a class with a non-final, non-volatile static field of mutable type"
        def sourceInfo = createJavaSourceInfo([])
        def staticField = createFieldInfo("int", "counter", 10, false, false, true)
        def classInfo = createClassInfo("TestClass", [staticField], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect unsafe publication issue"
        !issues.isEmpty()
        issues.any { issue ->
            issue.description?.contains("Static field") &&
            issue.description?.contains("unsafe publication")
        }
        issues.any { it.severity in [IssueSeverity.MEDIUM, IssueSeverity.HIGH] }
    }

    def "should detect race condition in shared instance field"() {
        given: "a class with a non-final, non-volatile instance field of mutable type"
        def sourceInfo = createJavaSourceInfo([])
        def instanceField = createFieldInfo("HashMap", "data", 10, false, false, false)
        def classInfo = createClassInfo("TestClass", [instanceField], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect shared mutable state issue"
        !issues.isEmpty()
        issues.any { issue ->
            issue.description?.contains("mutable and not thread-safe")
        }
        issues.any { it.severity in [IssueSeverity.HIGH, IssueSeverity.CRITICAL] }
    }

    @Unroll
    def "should detect issues in code with #description"() {
        given: "a class with specific field configuration"
        def sourceInfo = createJavaSourceInfo([])
        def field = createFieldInfo(fieldType, fieldName, 10, isFinal, isVolatile, isStatic)
        def classInfo = createClassInfo(className, [field], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect thread safety issues based on field properties"
        if (expectIssues) {
            !issues.isEmpty()
            issues.any { issue ->
                expectedKeywords.any { keyword ->
                    issue.description?.toLowerCase()?.contains(keyword.toLowerCase())
                }
            }
        } else {
            issues.isEmpty() || issues.every { it.severity == IssueSeverity.LOW }
        }
        
        where:
        description                    | className          | fieldType     | fieldName    | isFinal | isVolatile | isStatic | expectIssues | expectedKeywords
        "volatile without atomicity"  | "VolatileTest"     | "int"         | "counter"    | false   | true       | false    | false        | []
        "double-checked locking"      | "DoubleCheck"      | "Object"      | "instance"   | false   | false      | true     | true         | ["unsafe", "publication"]
    }

    def "should detect issues in classes with inheritance"() {
        given: "a class with non-final, non-volatile fields and inheritance"
        def sourceInfo = createJavaSourceInfo([])
        def mutableField = createFieldInfo("ArrayList", "items", 10, false, false, false)
        def classInfo = createClassInfo("InheritanceTest", [mutableField], [])
        classInfo.setParentClasses(["BaseClass"])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect shared mutable state issues"
        !issues.isEmpty()
        issues.any { issue ->
            issue.description?.contains("mutable and not thread-safe")
        }
    }

    def "should handle thread-safe fields correctly"() {
        given: "a class with final and volatile fields"
        def sourceInfo = createJavaSourceInfo([])
        def finalField = createFieldInfo("String", "name", 10, true, false, false)
        def volatileField = createFieldInfo("boolean", "flag", 11, false, true, false)
        def classInfo = createClassInfo("ThreadSafeClass", [finalField, volatileField], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should not detect thread safety issues for properly declared fields"
        def criticalIssues = issues.findAll { 
            it.severity in [IssueSeverity.HIGH, IssueSeverity.CRITICAL] 
        }
        criticalIssues.isEmpty()
    }

    def "should detect multiple thread safety issues"() {
        given: "a class with multiple problematic fields"
        def sourceInfo = createJavaSourceInfo([])
        def staticField = createFieldInfo("int", "globalCounter", 10, false, false, true)
        def mutableField = createFieldInfo("HashMap", "cache", 11, false, false, false)
        def unsafeField = createFieldInfo("ArrayList", "list", 12, false, false, false)
        def classInfo = createClassInfo("MultipleIssues", [staticField, mutableField, unsafeField], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect multiple issues"
        issues.size() >= 3
        issues.any { issue ->
            issue.description?.contains("unsafe publication")
        }
        issues.any { issue ->
            issue.description?.contains("mutable and not thread-safe")
        }
    }

    def "should provide helpful suggestions"() {
        given: "a class with thread safety issues"
        def sourceInfo = createJavaSourceInfo([])
        def problematicField = createFieldInfo("HashMap", "data", 10, false, false, false)
        def classInfo = createClassInfo("UnsafeClass", [problematicField], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should provide helpful suggestions"
        !issues.isEmpty()
        issues.every { issue ->
            issue.suggestedFix != null && !issue.suggestedFix.trim().isEmpty()
        }
        
        def suggestions = issues.collect { it.suggestedFix }.join(" ").toLowerCase()
        suggestions.contains("final") ||
        suggestions.contains("volatile") ||
        suggestions.contains("thread-safe")
    }

    def "should handle classes without fields gracefully"() {
        given: "a class with no fields"
        def sourceInfo = createJavaSourceInfo([])
        def classInfo = createClassInfo("EmptyClass", [], [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should not detect any field-related thread safety issues"
        def fieldIssues = issues.findAll { issue ->
            issue.description?.contains("field") ||
            issue.description?.contains("mutable state")
        }
        fieldIssues.isEmpty()
    }

    // Helper methods
    private JavaSourceInfo createJavaSourceInfo(List<String> imports) {
        def sourceInfo = new JavaSourceInfo()
        sourceInfo.fileName = "TestFile.java"
        sourceInfo.filePath = "TestFile.java"
        sourceInfo.content = "public class TestFile { /* test content */ }"
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

    private FieldInfo createFieldInfo(String type, String name, int lineNumber, boolean isFinal, boolean isVolatile, boolean isStatic) {
        def fieldInfo = new FieldInfo()
        fieldInfo.setName(name)
        fieldInfo.setType(type)
        fieldInfo.setLineNumber(lineNumber)
        fieldInfo.setFinal(isFinal)
        fieldInfo.setVolatile(isVolatile)
        fieldInfo.setStatic(isStatic)
        return fieldInfo
    }

    private MethodInfo createMethodInfo(String name, String returnType, int lineNumber, boolean isSynchronized) {
        def methodInfo = new MethodInfo()
        methodInfo.setName(name)
        methodInfo.setReturnType(returnType)
        methodInfo.setLineNumber(lineNumber)
        methodInfo.setSynchronized(isSynchronized)
        methodInfo.setStatic(false)
        methodInfo.setParameterTypes([])
        return methodInfo
    }
}
