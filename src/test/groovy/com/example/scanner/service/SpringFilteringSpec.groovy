package com.example.scanner.service

import com.example.scanner.model.JavaSourceInfo
import com.github.javaparser.JavaParser
import spock.lang.Specification

/**
 * Spock specification for Spring annotation filtering in JavaSourceAnalysisService
 */
class SpringFilteringSpec extends Specification {

    JavaSourceAnalysisService service
    JavaParser javaParser

    def setup() {
        service = new JavaSourceAnalysisService()
        javaParser = new JavaParser()
    }

    def "should include Spring-managed classes when filtering is enabled"() {
        given: "Spring filtering is enabled"
        service.setSpringFilterEnabled(true)
        
        and: "Java code with Spring annotations"
        def code = '''
            import org.springframework.stereotype.Service;
            import org.springframework.stereotype.Component;
            import org.springframework.stereotype.Repository;
            
            @Service
            public class UserService {
                private String name;
                public void processUser() {}
            }
            
            @Component
            public class DataProcessor {
                private int count;
                public void process() {}
            }
            
            @Repository
            public class UserRepository {
                private Map<String, Object> cache = new HashMap<>();
                public void save() {}
            }
            
            public class PlainClass {
                private String data;
                public void doSomething() {}
            }
        '''
        
        when: "analyzing the source file"
        def tempFile = createTempFile("SpringClasses.java", code)
        def results = service.analyzeJavaFiles([tempFile])
        
        then: "should include only Spring-managed classes"
        results.size() == 1
        def sourceInfo = results[0]
        sourceInfo.classes.size() == 3  // Only @Service, @Component, @Repository classes
        
        def classNames = sourceInfo.classes.collect { it.name }
        classNames.contains("UserService")
        classNames.contains("DataProcessor") 
        classNames.contains("UserRepository")
        !classNames.contains("PlainClass")  // Should be filtered out
        
        and: "Spring annotations should be tracked"
        def userService = sourceInfo.classes.find { it.name == "UserService" }
        userService.isSpringManaged()
        userService.hasSpringAnnotation("Service")
        
        def dataProcessor = sourceInfo.classes.find { it.name == "DataProcessor" }
        dataProcessor.isSpringManaged()
        dataProcessor.hasSpringAnnotation("Component")
    }
    
    def "should include all classes when filtering is disabled"() {
        given: "Spring filtering is disabled"
        service.setSpringFilterEnabled(false)
        
        and: "Java code with mixed annotations"
        def code = '''
            import org.springframework.stereotype.Service;
            
            @Service
            public class UserService {
                private String name;
                public void processUser() {}
            }
            
            public class PlainClass {
                private String data;
                public void doSomething() {}
            }
        '''
        
        when: "analyzing the source file"
        def tempFile = createTempFile("MixedClasses.java", code)
        def results = service.analyzeJavaFiles([tempFile])
        
        then: "should include all classes"
        results.size() == 1
        def sourceInfo = results[0]
        sourceInfo.classes.size() == 2  // Both classes included
        
        def classNames = sourceInfo.classes.collect { it.name }
        classNames.contains("UserService")
        classNames.contains("PlainClass")
        
        and: "Spring annotations should still be tracked"
        def userService = sourceInfo.classes.find { it.name == "UserService" }
        userService.isSpringManaged()
        userService.hasSpringAnnotation("Service")
        
        def plainClass = sourceInfo.classes.find { it.name == "PlainClass" }
        !plainClass.isSpringManaged()
        plainClass.springAnnotations.isEmpty()
    }
    
    def "should detect various Spring annotations"() {
        given: "Spring filtering is enabled"
        service.setSpringFilterEnabled(true)
        
        and: "Java code with different Spring annotations"
        def code = '''
            import org.springframework.stereotype.Service;
            import org.springframework.stereotype.Component;
            import org.springframework.stereotype.Repository;
            import org.springframework.stereotype.Controller;
            import org.springframework.web.bind.annotation.RestController;
            import org.springframework.context.annotation.Configuration;
            
            @Service
            public class MyService {}
            
            @Component  
            public class MyComponent {}
            
            @Repository
            public class MyRepository {}
            
            @Controller
            public class MyController {}
            
            @RestController
            public class MyRestController {}
            
            @Configuration
            public class MyConfiguration {}
        '''
        
        when: "analyzing the source file"
        def tempFile = createTempFile("AllSpringAnnotations.java", code)
        def results = service.analyzeJavaFiles([tempFile])
        
        then: "should detect all Spring annotations"
        results.size() == 1
        def sourceInfo = results[0]
        sourceInfo.classes.size() == 6  // All Spring-annotated classes
        
        def annotations = sourceInfo.classes.collectMany { it.springAnnotations }
        annotations.containsAll(["Service", "Component", "Repository", "Controller", "RestController", "Configuration"])
    }
    
    def "should log filtering decisions appropriately"() {
        given: "Spring filtering is enabled"
        service.setSpringFilterEnabled(true)
        
        when: "Spring filtering status is checked"
        def isEnabled = service.isSpringFilterEnabled()
        
        then: "should return correct status"
        isEnabled == true
        
        when: "Spring filtering is disabled"
        service.setSpringFilterEnabled(false)
        
        then: "should return updated status"
        !service.isSpringFilterEnabled()
    }
    
    private createTempFile(String fileName, String content) {
        def tempDir = File.createTempDir()
        def tempFile = new File(tempDir, fileName)
        tempFile.text = content
        return tempFile.toPath()
    }
}
