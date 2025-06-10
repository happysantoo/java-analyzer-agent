package com.example.scanner.analyzer

import com.example.scanner.model.*
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Spock specification for LockUsageAnalyzer
 */
class LockUsageAnalyzerSpec extends Specification {

    LockUsageAnalyzer analyzer
    
    def setup() {
        analyzer = new LockUsageAnalyzer()
    }
    
    @Unroll
    def "should detect lock usage patterns for: #lockType"() {
        given: "a class with lock fields"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("lock", lockType, false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", 
            "public class TestClass {}", ["java.util.concurrent.locks"])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should suggest proper lock usage patterns"
        issues.size() == 1
        issues[0].type == "LOCK_USAGE_PATTERN"
        issues[0].severity == IssueSeverity.MEDIUM
        issues[0].description.contains("try-finally pattern")
        issues[0].suggestedFix.contains("lock.lock(); try { ... } finally { lock.unlock(); }")
        
        where:
        lockType << ["Lock", "ReentrantLock", "ReadWriteLock", "ReentrantReadWriteLock"]
    }
    
    def "should not analyze classes without lock-related imports"() {
        given: "a class with Lock field but no lock imports"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("lock", "ReentrantLock", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", 
            "public class TestClass {}", [])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should not detect any issues"
        issues.isEmpty()
    }
    
    def "should detect multiple lock fields"() {
        given: "a class with multiple lock fields"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("readLock", "ReentrantReadWriteLock", false, false, false),
            createFieldInfo("writeLock", "ReentrantLock", false, false, false),
            createFieldInfo("syncLock", "Lock", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", 
            "public class TestClass {}", ["java.util.concurrent.locks"])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should detect all lock usage patterns"
        issues.size() == 3
        issues.every { 
            it.type == "LOCK_USAGE_PATTERN" &&
            it.severity == IssueSeverity.MEDIUM &&
            it.description.contains("try-finally pattern")
        }
    }
    
    def "should handle classes with no lock fields"() {
        given: "a class with no lock fields but lock imports"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("name", "String", false, false, false),
            createFieldInfo("count", "int", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", 
            "public class TestClass {}", ["java.util.concurrent.locks"])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should not find any lock usage issues"
        issues.isEmpty()
    }
    
    def "should handle custom Lock implementations"() {
        given: "a class with custom lock type"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("customLock", "CustomReentrantLock", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", 
            "public class TestClass {}", ["java.util.concurrent.locks"])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should detect custom lock usage"
        issues.size() == 1
        issues[0].type == "LOCK_USAGE_PATTERN"
    }
    
    def "should provide consistent recommendations"() {
        given: "a class with various lock types"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("mainLock", "ReentrantLock", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", 
            "public class TestClass {}", ["java.util.concurrent.locks"])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should provide consistent recommendations"
        issues.size() == 1
        def issue = issues[0]
        issue.className == "TestClass"
        issue.lineNumber == 10
        issue.suggestedFix == "Use lock.lock(); try { ... } finally { lock.unlock(); } pattern"
    }
    
    def "should handle static lock fields"() {
        given: "a class with static lock field"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("STATIC_LOCK", "ReentrantLock", false, false, true) // static
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", 
            "public class TestClass {}", ["java.util.concurrent.locks"])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should still detect lock usage pattern"
        issues.size() == 1
        issues[0].type == "LOCK_USAGE_PATTERN"
    }
    
    def "should handle final lock fields"() {
        given: "a class with final lock field"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("lock", "ReentrantLock", true, false, false) // final
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", 
            "public class TestClass {}", ["java.util.concurrent.locks"])
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should detect lock usage pattern for final fields too"
        issues.size() == 1
        issues[0].type == "LOCK_USAGE_PATTERN"
    }
    
    def "should handle edge cases gracefully"() {
        given: "various edge case scenarios"
        def classInfo = createClassInfo("TestClass", [])
        def sourceInfo = createJavaSourceInfo("TestClass.java", 
            "public class TestClass {}", ["java.util.concurrent.locks"])
        
        when: "analyzing empty class with lock imports"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "should handle gracefully"
        issues.isEmpty()
    }
    
    // Helper methods
    private JavaSourceInfo createJavaSourceInfo(String fileName, String content, List<String> imports) {
        def sourceInfo = new JavaSourceInfo()
        sourceInfo.filePath = fileName
        sourceInfo.content = content
        sourceInfo.threadRelatedImports = imports as Set
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
