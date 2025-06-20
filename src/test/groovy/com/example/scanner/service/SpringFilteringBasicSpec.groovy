package com.example.scanner.service

import com.example.scanner.model.JavaSourceInfo
import spock.lang.Specification
import spock.lang.Subject

import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * Very simple test to check basic service functionality.
 */
class SpringFilteringBasicSpec extends Specification {

    @Subject
    JavaSourceAnalysisService sourceAnalysisService
    
    def setup() {
        sourceAnalysisService = new JavaSourceAnalysisService()
    }
    
    def "should inject service correctly"() {
        expect:
        sourceAnalysisService != null
    }
    
    def "should analyze a simple Java file"() {
        given: "a simple Java file"
        String testContent = '''
package com.example.test;

public class SimpleClass {
    private String name;
    
    public String getName() {
        return name;
    }
}
'''
        
        Path tempFile = Files.createTempFile("SimpleClass", ".java")
        Files.write(tempFile, testContent.bytes, StandardOpenOption.WRITE)
        
        when: "analyzing the file"
        List<JavaSourceInfo> results = sourceAnalysisService.analyzeJavaFiles([tempFile])
        
        then: "should get valid results"
        results != null
        results.size() == 1
        
        cleanup:
        Files.deleteIfExists(tempFile)
    }
}
