package com.example.scanner.service

import com.example.scanner.model.JavaSourceInfo
import com.example.scanner.model.ClassInfo
import spock.lang.Specification
import spock.lang.Subject

import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * Simple test to debug Spring filtering issues.
 */
class SpringFilteringDebugSpec extends Specification {

    @Subject
    JavaSourceAnalysisService sourceAnalysisService
    
    def setup() {
        sourceAnalysisService = new JavaSourceAnalysisService()
    }
    
    def "should test basic Spring annotation detection"() {
        given: "a simple test file with Spring annotation"
        String testContent = '''
package com.example.test;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class TestService {
    private Map<String, String> cache = new HashMap<>();
    
    public void updateCache(String key, String value) {
        cache.put(key, value);
    }
}
'''
        
        Path tempFile = Files.createTempFile("TestService", ".java")
        Files.write(tempFile, testContent.bytes, StandardOpenOption.WRITE)
        
        when: "analyzing with Spring filtering disabled"
        sourceAnalysisService.setSpringFilterEnabled(false)
        List<JavaSourceInfo> results = sourceAnalysisService.analyzeJavaFiles([tempFile])
        
        then: "should get results"
        results != null
        results.size() == 1
        
        JavaSourceInfo sourceInfo = results[0]
        sourceInfo != null
        sourceInfo.classes != null
        sourceInfo.classes.size() == 1
        
        ClassInfo classInfo = sourceInfo.classes[0]
        classInfo != null
        classInfo.name == "TestService"
        
        when: "analyzing with Spring filtering enabled"
        sourceAnalysisService.setSpringFilterEnabled(true)
        sourceAnalysisService.setSpringAnnotations(["Service"])
        List<JavaSourceInfo> filteredResults = sourceAnalysisService.analyzeJavaFiles([tempFile])
        
        then: "should still get the Spring class"
        filteredResults != null
        filteredResults.size() == 1
        
        JavaSourceInfo filteredSourceInfo = filteredResults[0]
        filteredSourceInfo != null
        filteredSourceInfo.classes != null
        filteredSourceInfo.classes.size() == 1
        
        ClassInfo filteredClassInfo = filteredSourceInfo.classes[0]
        filteredClassInfo != null
        filteredClassInfo.name == "TestService"
        filteredClassInfo.springAnnotations != null
        filteredClassInfo.isSpringManaged()
        filteredClassInfo.springAnnotations.contains("Service")
        
        cleanup:
        Files.deleteIfExists(tempFile)
    }
}
