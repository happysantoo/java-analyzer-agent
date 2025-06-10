package com.example.scanner.analyzer

import com.example.scanner.model.ConcurrencyIssue
import com.example.scanner.model.JavaSourceInfo
import com.example.scanner.model.ClassInfo
import com.example.scanner.model.FieldInfo
import com.example.scanner.model.IssueSeverity
import com.github.javaparser.JavaParser
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class ConcurrentCollectionsAnalyzerSpec extends Specification {

    @Subject
    ConcurrentCollectionsAnalyzer analyzer = new ConcurrentCollectionsAnalyzer()
    
    JavaParser javaParser = new JavaParser()

    def "should detect unsafe HashMap usage in concurrent environment"() {
        given: "a class using HashMap in a concurrent context"
        def classInfo = createClassInfo("UnsafeHashMap", [
            createFieldInfo("cache", "HashMap<String, String>", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("UnsafeHashMap.java", "public class UnsafeHashMap {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect HashMap concurrency issues"
        !issues.isEmpty()
        issues.any { issue ->
            (issue.description?.toLowerCase()?.contains("hashmap")) ||
            (issue.type?.toLowerCase()?.contains("unsafe"))
        }
        issues.any { it.severity in [IssueSeverity.HIGH, IssueSeverity.MEDIUM] }
    }

    def "should detect unsafe ArrayList usage"() {
        given: "a class using ArrayList in concurrent context"
        def classInfo = createClassInfo("UnsafeList", [
            createFieldInfo("items", "ArrayList<String>", false, false, true)
        ])
        def sourceInfo = createJavaSourceInfo("UnsafeList.java", "public class UnsafeList {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect ArrayList concurrency issues"
        !issues.isEmpty()
        issues.any { issue ->
            (issue.description?.toLowerCase()?.contains("arraylist")) ||
            (issue.type?.toLowerCase()?.contains("unsafe"))
        }
    }

    def "should recommend ConcurrentHashMap over HashMap"() {
        given: "a class using HashMap"
        def classInfo = createClassInfo("MapUser", [
            createFieldInfo("scores", "HashMap<String, Integer>", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("MapUser.java", "public class MapUser {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should recommend ConcurrentHashMap"
        !issues.isEmpty()
        def recommendations = issues.collect { it.suggestedFix }.join(" ").toLowerCase()
        recommendations.contains("concurrenthashmap") ||
        recommendations.contains("concurrent") && recommendations.contains("map")
    }

    @Unroll
    def "should detect unsafe collection: #collectionType"() {
        given: "a class with unsafe collection field"
        def classInfo = createClassInfo("TestClass", [
            createFieldInfo("data", fieldType, false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("TestClass.java", "public class TestClass {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect concurrency issues"
        !issues.isEmpty()
        issues.any { issue ->
            expectedKeywords.any { keyword ->
                (issue.description?.toLowerCase()?.contains(keyword.toLowerCase())) ||
                (issue.suggestedFix?.toLowerCase()?.contains(keyword.toLowerCase()))
            }
        }
        
        where:
        collectionType | fieldType               | expectedKeywords
        "HashSet"      | "HashSet<String>"       | ["hashset", "concurrent", "thread-safe"]
        "TreeMap"      | "TreeMap<String, String>" | ["treemap", "concurrent", "sorted"]
        "Vector"       | "Vector<String>"        | ["vector", "legacy", "concurrent"]
    }

    def "should approve thread-safe collections"() {
        given: "a class using thread-safe collections"
        def classInfo = createClassInfo("SafeCollections", [
            createFieldInfo("cache", "ConcurrentHashMap<String, String>", false, false, false),
            createFieldInfo("items", "CopyOnWriteArrayList<String>", false, false, false),
            createFieldInfo("queue", "LinkedBlockingQueue<String>", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("SafeCollections.java", "public class SafeCollections {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should have no collection-related issues for safe collections"
        def collectionIssues = issues?.findAll { issue ->
            (issue.description?.toLowerCase()?.contains("collection")) ||
            (issue.type?.toLowerCase()?.contains("unsafe"))
        }
        collectionIssues.isEmpty() // Safe collections should not generate issues
    }

    def "should detect mixed safe and unsafe collection usage"() {
        given: "a class mixing safe and unsafe collections"
        def code = '''
            import java.util.*;
            import java.util.concurrent.*;
            
            public class MixedCollections {
                private ConcurrentHashMap<String, String> safeMap = new ConcurrentHashMap<>();
                private HashMap<String, String> unsafeMap = new HashMap<>();
                private List<String> unsafeList = new ArrayList<>();
                
                public void operations() {
                    safeMap.put("safe", "value");
                    unsafeMap.put("unsafe", "value");
                    unsafeList.add("item");
                }
            }
        '''
        
        when: "analyzing the code"
        def issues = analyzeCode(code, "MixedCollections.java")
        
        then: "it should detect issues with unsafe collections only"
        !issues.isEmpty()
        issues.any { issue ->
            (issue.description && issue.description.toLowerCase().contains("hashmap")) ||
            (issue.description && issue.description.toLowerCase().contains("arraylist")) ||
            (issue.type && issue.type.toLowerCase().contains("unsafe"))
        }
        
        and: "should not flag the safe collections"
        issues.every { issue ->
            !(issue.description && issue.description.toLowerCase().contains("concurrenthashmap"))
        }
    }

    def "should detect unsafe iteration patterns"() {
        given: "a class with unsafe ArrayList field (potential iteration issues)"
        def classInfo = createClassInfo("UnsafeIteration", [
            createFieldInfo("items", "ArrayList<String>", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("UnsafeIteration.java", "public class UnsafeIteration {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should detect ArrayList usage issues"
        !issues.isEmpty()
        issues.any { issue ->
            (issue.description?.toLowerCase()?.contains("arraylist")) ||
            (issue.type?.toLowerCase()?.contains("unsafe"))
        }
    }

    def "should provide collection-specific recommendations"() {
        given: "a class with multiple collection issues"
        def classInfo = createClassInfo("CollectionIssues", [
            createFieldInfo("userCache", "HashMap<String, String>", false, false, false),
            createFieldInfo("tasks", "ArrayList<String>", false, false, false),
            createFieldInfo("processed", "HashSet<String>", false, false, false)
        ])
        def sourceInfo = createJavaSourceInfo("CollectionIssues.java", "public class CollectionIssues {}")
        
        when: "analyzing the class"
        def issues = analyzer.analyze(sourceInfo, classInfo)
        
        then: "it should provide specific collection recommendations"
        !issues.isEmpty()
        issues.every { issue ->
            issue.suggestedFix != null && !issue.suggestedFix.trim().isEmpty()
        }
        
        def recommendations = issues.collect { it.suggestedFix }.join(" ").toLowerCase()
        recommendations.contains("concurrenthashmap") ||
        recommendations.contains("copyonwritearraylist") ||
        recommendations.contains("concurrentskiplistset") ||
        recommendations.contains("synchroniz")
    }

    def "should handle classes without collections gracefully"() {
        given: "a class without any collections"
        def code = '''
            public class NoCollections {
                private String name;
                private int value;
                
                public void setName(String name) {
                    this.name = name;
                }
                
                public int getValue() {
                    return value;
                }
            }
        '''
        
        when: "analyzing the code"
        def issues = analyzeCode(code, "NoCollections.java")
        
        then: "it should not throw exceptions and may return empty results"
        issues != null
        // Classes without collections might have no collection-specific issues
    }

    private List<ConcurrencyIssue> analyzeCode(String code, String fileName) {
        def compilationUnit = javaParser.parse(code).result.orElseThrow()
        def sourceInfo = createSourceInfo(fileName, code)
        def classInfo = extractClassInfo(compilationUnit, fileName)
        return analyzer.analyze(sourceInfo, classInfo)
    }

    private JavaSourceInfo createSourceInfo(String fileName, String content) {
        def sourceInfo = new JavaSourceInfo()
        sourceInfo.fileName = fileName
        sourceInfo.content = content
        sourceInfo.filePath = "/test/path/${fileName}"
        sourceInfo.totalLines = content.split('\n').length
        return sourceInfo
    }

    private ClassInfo extractClassInfo(def compilationUnit, String fileName) {
        def classInfo = new ClassInfo()
        classInfo.setName(fileName.replace('.java', ''))
        classInfo.setLineNumber(1)
        
        // Extract fields from compilation unit
        def fields = []
        compilationUnit.findAll(com.github.javaparser.ast.body.FieldDeclaration.class).each { fieldDecl ->
            fieldDecl.variables.each { variable ->
                def fieldInfo = new com.example.scanner.model.FieldInfo()
                fieldInfo.setName(variable.getNameAsString())
                fieldInfo.setType(fieldDecl.getElementType().asString())
                fieldInfo.setLineNumber(fieldDecl.getBegin().map { it.line }.orElse(1))
                fieldInfo.setFinal(fieldDecl.getModifiers().any { it.getKeyword().asString() == "final" })
                fieldInfo.setVolatile(fieldDecl.getModifiers().any { it.getKeyword().asString() == "volatile" })
                fieldInfo.setStatic(fieldDecl.getModifiers().any { it.getKeyword().asString() == "static" })
                fields.add(fieldInfo)
            }
        }
        classInfo.setFields(fields)
        
        return classInfo
    }

    // Helper methods
    private JavaSourceInfo createJavaSourceInfo(String fileName, String content) {
        def sourceInfo = new JavaSourceInfo()
        sourceInfo.fileName = fileName
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
