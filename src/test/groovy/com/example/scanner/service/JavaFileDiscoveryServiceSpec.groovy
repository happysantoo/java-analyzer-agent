package com.example.scanner.service

import com.example.scanner.config.ScannerConfiguration
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

/**
 * Spock specification for JavaFileDiscoveryService
 */
class JavaFileDiscoveryServiceSpec extends Specification {

    JavaFileDiscoveryService service
    ScannerConfiguration mockConfiguration
    
    @TempDir
    Path tempDir
    
    def setup() {
        mockConfiguration = Mock(ScannerConfiguration)
        service = new JavaFileDiscoveryService()
        service.setConfiguration(mockConfiguration)
    }
    
    def "should discover Java files in directory"() {
        given: "a directory with Java files"
        createJavaFile("TestClass.java", "public class TestClass {}")
        createJavaFile("AnotherClass.java", "public class AnotherClass {}")
        createTextFile("NotJava.txt", "This is not Java")
        
        and: "configuration allows all files"
        mockConfiguration.isExcludeTestFiles() >> false
        mockConfiguration.isExcludeGeneratedCode() >> false
        mockConfiguration.getExcludePatterns() >> []
        
        when: "discovering Java files"
        def javaFiles = service.discoverJavaFiles(tempDir)
        
        then: "should find only Java files"
        javaFiles.size() == 2
        javaFiles.collect { it.fileName.toString() }.sort() == ['AnotherClass.java', 'TestClass.java']
    }
    
    def "should discover Java files recursively"() {
        given: "a directory structure with Java files at various levels"
        createJavaFile("RootClass.java", "public class RootClass {}")
        def subDir = Files.createDirectory(tempDir.resolve("subpackage"))
        Files.write(subDir.resolve("SubClass.java"), "public class SubClass {}".bytes)
        def deepDir = Files.createDirectory(subDir.resolve("deep"))
        Files.write(deepDir.resolve("DeepClass.java"), "public class DeepClass {}".bytes)
        
        and: "configuration allows all files"
        mockConfiguration.isExcludeTestFiles() >> false
        mockConfiguration.isExcludeGeneratedCode() >> false
        mockConfiguration.getExcludePatterns() >> []
        
        when: "discovering Java files"
        def javaFiles = service.discoverJavaFiles(tempDir)
        
        then: "should find all Java files recursively"
        javaFiles.size() == 3
        javaFiles.any { it.fileName.toString() == 'RootClass.java' }
        javaFiles.any { it.fileName.toString() == 'SubClass.java' }
        javaFiles.any { it.fileName.toString() == 'DeepClass.java' }
    }
    
    @Unroll
    def "should exclude test files when configured: #filePath"() {
        given: "test files in various locations"
        Files.createDirectories(tempDir.resolve(filePath).parent ?: tempDir)
        Files.write(tempDir.resolve(filePath), "public class TestFile {}".bytes)
        createJavaFile("RegularClass.java", "public class RegularClass {}")
        
        and: "configuration excludes test files"
        mockConfiguration.isExcludeTestFiles() >> true
        mockConfiguration.isExcludeGeneratedCode() >> false
        mockConfiguration.getExcludePatterns() >> []
        
        when: "discovering Java files"
        def javaFiles = service.discoverJavaFiles(tempDir)
        
        then: "should exclude test files"
        javaFiles.size() == 1
        javaFiles[0].fileName.toString() == 'RegularClass.java'
        
        where:
        filePath << [
            "src/test/java/TestClass.java",
            "test/SomeTest.java",
            "TestClass.java",
            "SomeTests.java",
            "MyClassTest.java"
        ]
    }
    
    @Unroll
    def "should exclude generated code when configured: #filePath"() {
        given: "generated files in various locations"
        Files.createDirectories(tempDir.resolve(filePath).parent ?: tempDir)
        Files.write(tempDir.resolve(filePath), "public class GeneratedFile {}".bytes)
        createJavaFile("RegularClass.java", "public class RegularClass {}")
        
        and: "configuration excludes generated code"
        mockConfiguration.isExcludeTestFiles() >> false
        mockConfiguration.isExcludeGeneratedCode() >> true
        mockConfiguration.getExcludePatterns() >> []
        
        when: "discovering Java files"
        def javaFiles = service.discoverJavaFiles(tempDir)
        
        then: "should exclude generated files"
        javaFiles.size() == 1
        javaFiles[0].fileName.toString() == 'RegularClass.java'
        
        where:
        filePath << [
            "target/generated-sources/annotations/Generated.java",
            "build/generated/sources/annotationProcessor/java/main/Generated.java",
            "generated/Generated.java"
        ]
    }
    
    def "should handle exclude patterns from configuration"() {
        given: "various Java files"
        createJavaFile("Good.java", "public class Good {}")
        createJavaFile("TemporaryFile.java", "public class TemporaryFile {}")
        createJavaFile("BackupFile.java", "public class BackupFile {}")
        
        and: "configuration with exclude patterns"
        mockConfiguration.isExcludeTestFiles() >> false
        mockConfiguration.isExcludeGeneratedCode() >> false
        mockConfiguration.getExcludePatterns() >> ["temporary", "backup"]
        
        when: "discovering Java files"
        def javaFiles = service.discoverJavaFiles(tempDir)
        
        then: "should exclude files matching patterns"
        javaFiles.size() == 1
        javaFiles[0].fileName.toString() == 'Good.java'
    }
    
    def "should handle complex exclude combinations"() {
        given: "complex directory structure with various file types"
        createJavaFile("GoodClass.java", "public class GoodClass {}")
        
        // Test file
        def testDir = Files.createDirectories(tempDir.resolve("src/test/java"))
        Files.write(testDir.resolve("TestClass.java"), "public class TestClass {}".bytes)
        
        // Generated file
        def genDir = Files.createDirectories(tempDir.resolve("target/generated-sources"))
        Files.write(genDir.resolve("Generated.java"), "public class Generated {}".bytes)
        
        // Pattern-excluded file
        createJavaFile("TempClass.java", "public class TempClass {}")
        
        and: "configuration excludes everything"
        mockConfiguration.isExcludeTestFiles() >> true
        mockConfiguration.isExcludeGeneratedCode() >> true
        mockConfiguration.getExcludePatterns() >> ["temp"]
        
        when: "discovering Java files"
        def javaFiles = service.discoverJavaFiles(tempDir)
        
        then: "should only find regular files"
        javaFiles.size() == 1
        javaFiles[0].fileName.toString() == 'GoodClass.java'
    }
    
    def "should preserve file paths correctly"() {
        given: "Java files in nested structure"
        def packageDir = Files.createDirectories(tempDir.resolve("com/example/test"))
        Files.write(packageDir.resolve("MyClass.java"), 
            "package com.example.test;\npublic class MyClass {}".bytes)
        
        and: "configuration allows all files"
        mockConfiguration.isExcludeTestFiles() >> false
        mockConfiguration.isExcludeGeneratedCode() >> false
        mockConfiguration.getExcludePatterns() >> []
        
        when: "discovering Java files"
        def javaFiles = service.discoverJavaFiles(tempDir)
        
        then: "should preserve complete file paths"
        javaFiles.size() == 1
        def foundFile = javaFiles[0]
        foundFile.fileName.toString() == 'MyClass.java'
        foundFile.toString().contains("com/example/test/MyClass.java")
    }
    
    def "should handle empty directories gracefully"() {
        given: "an empty directory"
        mockConfiguration.isExcludeTestFiles() >> false
        mockConfiguration.isExcludeGeneratedCode() >> false
        mockConfiguration.getExcludePatterns() >> []
        
        when: "discovering Java files"
        def javaFiles = service.discoverJavaFiles(tempDir)
        
        then: "should return empty list"
        javaFiles.isEmpty()
    }

    // Helper methods
    private void createJavaFile(String fileName, String content) {
        Files.write(tempDir.resolve(fileName), content.bytes)
    }
    
    private void createTextFile(String fileName, String content) {
        Files.write(tempDir.resolve(fileName), content.bytes)
    }
}
