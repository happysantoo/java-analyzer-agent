package com.example.scanner.analyzer

import com.example.scanner.model.*
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Spock specification for AtomicOperationsAnalyzer
 */
class AtomicOperationsAnalyzerSpec extends Specification {

    AtomicOperationsAnalyzer analyzer
    
    def setup() {
        analyzer = new AtomicOperationsAnalyzer()
    }
    
    @Unroll
    def "should detect atomic opportunities for primitive counters: #fieldType #fieldName"() {
        given: "a class with primitive counter fields"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo(fieldName, fieldType, false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should suggest atomic alternatives"
        issues.size() == expectedIssues
        if (expectedIssues > 0) {
            issues.every { issue ->
                issue.type == "ATOMIC_OPPORTUNITY" &&
                issue.severity == IssueSeverity.LOW &&
                issue.description.contains("could benefit from atomic operations")
            }
        }
        
        where:
        fieldType | fieldName     | expectedIssues
        "int"     | "counter"     | 1
        "int"     | "index"       | 1
        "long"    | "count"       | 1
        "long"    | "size"        | 1
        "boolean" | "flag"        | 0  // boolean not considered counter
        "String"  | "counter"     | 0  // String not primitive
        "int"     | "value"       | 0  // name doesn't suggest counter
    }
    
    def "should not suggest atomics for volatile fields"() {
        given: "a class with volatile counter field"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("counter", "int", false, true, false) // volatile
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should not suggest atomic alternatives for volatile fields"
        issues.isEmpty()
    }
    
    def "should provide appropriate atomic alternatives"() {
        given: "a class with different primitive types"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("intCounter", "int", false, false, false),
            createFieldInfo("longIndex", "long", false, false, false),
            createFieldInfo("booleanFlag", "boolean", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should provide appropriate suggestions"
        def intIssue = issues.find { it.description.contains("intCounter") }
        def longIssue = issues.find { it.description.contains("longIndex") }
        
        intIssue.suggestedFix.contains("AtomicInteger")
        longIssue.suggestedFix.contains("AtomicLong")
    }
    
    def "should handle classes with no primitive counters"() {
        given: "a class with no primitive counter fields"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("name", "String", false, false, false),
            createFieldInfo("data", "List<String>", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should not find any atomic opportunities"
        issues.isEmpty()
    }
    
    def "should handle classes with final fields appropriately"() {
        given: "a class with final counter field"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("counter", "int", true, false, false) // final
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should not suggest atomics for final fields"
        issues.isEmpty()
    }
    
    def "should detect multiple atomic opportunities in single class"() {
        given: "a class with multiple primitive counter fields"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("counter", "int", false, false, false),
            createFieldInfo("index", "int", false, false, false),
            createFieldInfo("size", "long", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should find all atomic opportunities"
        issues.size() == 3
        issues.every { it.type == "ATOMIC_OPPORTUNITY" }
    }
    
    def "should handle edge cases gracefully"() {
        given: "various edge case scenarios"
        def classInfo = createClassInfo("TestClass", [])
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        
        when: "analyzing empty class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should handle gracefully"
        issues.isEmpty()
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
    
    private ClassInfo createClassInfo(String className, List<FieldInfo> fields) {
        def classInfo = new ClassInfo()
        classInfo.setName(className)
        classInfo.setFields(fields)
        classInfo.setMethods([])
        classInfo.setParentClasses([])
        classInfo.setImplementedInterfaces([])
        return classInfo
    }
    
    private FieldInfo createFieldInfo(String name, String type, boolean isFinal, boolean isVolatile, boolean isStatic) {
        def fieldInfo = new FieldInfo()
        fieldInfo.setName(name)
        fieldInfo.setType(type)
        fieldInfo.setFinal(isFinal)
        fieldInfo.setVolatile(isVolatile)
        fieldInfo.setStatic(isStatic)
        fieldInfo.setLineNumber(10)
        return fieldInfo
    }
}
