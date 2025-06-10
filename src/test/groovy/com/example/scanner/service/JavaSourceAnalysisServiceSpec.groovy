package com.example.scanner.service

import com.example.scanner.model.*
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

/**
 * Spock specification for JavaSourceAnalysisService
 */
class JavaSourceAnalysisServiceSpec extends Specification {

    JavaSourceAnalysisService service
    
    @TempDir
    Path tempDir
    
    def setup() {
        service = new JavaSourceAnalysisService()
    }
    
    def "should analyze simple Java class"() {
        given: "a simple Java class file"
        def javaCode = '''
            package com.example.test;
            
            public class SimpleClass {
                private String name;
                private int value;
                
                public void setName(String name) {
                    this.name = name;
                }
                
                public String getName() {
                    return name;
                }
            }
        '''
        def javaFile = createJavaFile("SimpleClass.java", javaCode)
        
        when: "analyzing the Java file"
        def results = service.analyzeJavaFiles([javaFile])
        
        then: "should extract correct information"
        results.size() == 1
        def sourceInfo = results[0]
        sourceInfo.filePath.endsWith("SimpleClass.java")
        sourceInfo.content == javaCode
        sourceInfo.classes.size() == 1
        
        def classInfo = sourceInfo.classes[0]
        classInfo.name == "SimpleClass"
        !classInfo.interface
        classInfo.fields.size() == 2
        classInfo.methods.size() == 2
        
        def nameField = classInfo.fields.find { it.name == "name" }
        nameField.type == "String"
        !nameField.final
        !nameField.volatile
        !nameField.static
        
        def setNameMethod = classInfo.methods.find { it.name == "setName" }
        setNameMethod.returnType == "void"
        !setNameMethod.synchronized
        !setNameMethod.static
        setNameMethod.parameterTypes == ["String"]
    }
    
    def "should detect thread-related imports"() {
        given: "Java class with thread-related imports"
        def javaCode = '''
            package com.example.test;
            
            import java.util.concurrent.ExecutorService;
            import java.util.concurrent.atomic.AtomicInteger;
            import java.util.concurrent.locks.ReentrantLock;
            import java.util.concurrent.ConcurrentHashMap;
            import java.lang.Thread;
            
            public class ConcurrentClass {
                private ExecutorService executor;
                private AtomicInteger counter;
                private ReentrantLock lock;
            }
        '''
        def javaFile = createJavaFile("ConcurrentClass.java", javaCode)
        
        when: "analyzing the Java file"
        def results = service.analyzeJavaFiles([javaFile])
        
        then: "should detect thread-related imports"
        results.size() == 1
        def sourceInfo = results[0]
        sourceInfo.threadRelatedImports.size() == 5
        sourceInfo.threadRelatedImports.contains("java.util.concurrent.ExecutorService")
        sourceInfo.threadRelatedImports.contains("java.util.concurrent.atomic.AtomicInteger")
        sourceInfo.threadRelatedImports.contains("java.util.concurrent.locks.ReentrantLock")
        sourceInfo.threadRelatedImports.contains("java.util.concurrent.ConcurrentHashMap")
        sourceInfo.threadRelatedImports.contains("java.lang.Thread")
    }
    
    def "should analyze synchronized methods"() {
        given: "Java class with synchronized methods"
        def javaCode = '''
            public class SynchronizedClass {
                private int value;
                
                public synchronized void setValue(int value) {
                    this.value = value;
                }
                
                public synchronized int getValue() {
                    return value;
                }
                
                public void regularMethod() {
                    // not synchronized
                }
            }
        '''
        def javaFile = createJavaFile("SynchronizedClass.java", javaCode)
        
        when: "analyzing the Java file"
        def results = service.analyzeJavaFiles([javaFile])
        
        then: "should detect synchronized methods"
        results.size() == 1
        def classInfo = results[0].classes[0]
        
        def setValueMethod = classInfo.methods.find { it.name == "setValue" }
        setValueMethod.synchronized
        
        def getValueMethod = classInfo.methods.find { it.name == "getValue" }
        getValueMethod.synchronized
        
        def regularMethod = classInfo.methods.find { it.name == "regularMethod" }
        !regularMethod.synchronized
    }
    
    def "should analyze volatile and final fields"() {
        given: "Java class with volatile and final fields"
        def javaCode = '''
            public class FieldModifiersClass {
                private volatile boolean flag;
                private final String CONSTANT = "value";
                private static int staticValue;
                private volatile long volatileCounter;
                private final AtomicInteger atomicValue = new AtomicInteger();
            }
        '''
        def javaFile = createJavaFile("FieldModifiersClass.java", javaCode)
        
        when: "analyzing the Java file"
        def results = service.analyzeJavaFiles([javaFile])
        
        then: "should detect field modifiers correctly"
        results.size() == 1
        def classInfo = results[0].classes[0]
        classInfo.fields.size() == 5
        
        def flagField = classInfo.fields.find { it.name == "flag" }
        flagField.volatile
        !flagField.final
        !flagField.static
        
        def constantField = classInfo.fields.find { it.name == "CONSTANT" }
        !constantField.volatile
        constantField.final
        !constantField.static
        
        def staticField = classInfo.fields.find { it.name == "staticValue" }
        !staticField.volatile
        !staticField.final
        staticField.static
        
        def volatileCounterField = classInfo.fields.find { it.name == "volatileCounter" }
        volatileCounterField.volatile
        !volatileCounterField.final
        
        def atomicField = classInfo.fields.find { it.name == "atomicValue" }
        !atomicField.volatile
        atomicField.final
    }
    
    def "should analyze class inheritance"() {
        given: "Java class with inheritance"
        def javaCode = '''
            import java.util.concurrent.Callable;
            import java.io.Serializable;
            
            public class InheritanceClass extends Thread implements Callable<String>, Serializable {
                @Override
                public void run() {
                    // thread implementation
                }
                
                @Override
                public String call() throws Exception {
                    return "result";
                }
            }
        '''
        def javaFile = createJavaFile("InheritanceClass.java", javaCode)
        
        when: "analyzing the Java file"
        def results = service.analyzeJavaFiles([javaFile])
        
        then: "should detect inheritance relationships"
        results.size() == 1
        def classInfo = results[0].classes[0]
        
        classInfo.parentClasses.contains("Thread")
        classInfo.implementedInterfaces.contains("Callable")
        classInfo.implementedInterfaces.contains("Serializable")
    }
    
    def "should analyze interface declarations"() {
        given: "Java interface file"
        def javaCode = '''
            public interface ConcurrentInterface {
                void processAsync();
                String getResult();
            }
        '''
        def javaFile = createJavaFile("ConcurrentInterface.java", javaCode)
        
        when: "analyzing the Java file"
        def results = service.analyzeJavaFiles([javaFile])
        
        then: "should recognize interface"
        results.size() == 1
        def classInfo = results[0].classes[0]
        
        classInfo.name == "ConcurrentInterface"
        classInfo.interface
        classInfo.methods.size() == 2
    }
    
    def "should handle multiple classes in single file"() {
        given: "Java file with multiple classes"
        def javaCode = '''
            public class MainClass {
                private String name;
            }
            
            class HelperClass {
                private int value;
            }
            
            class AnotherHelper {
                private boolean flag;
            }
        '''
        def javaFile = createJavaFile("MainClass.java", javaCode)
        
        when: "analyzing the Java file"
        def results = service.analyzeJavaFiles([javaFile])
        
        then: "should extract all classes"
        results.size() == 1
        def sourceInfo = results[0]
        sourceInfo.classes.size() == 3
        
        def classNames = sourceInfo.classes.collect { it.name }
        classNames.contains("MainClass")
        classNames.contains("HelperClass")
        classNames.contains("AnotherHelper")
    }
    
    def "should handle parsing errors gracefully"() {
        given: "invalid Java code"
        def invalidJavaCode = '''
            public class InvalidClass {
                // missing closing brace
                private String name
        '''
        def javaFile = createJavaFile("InvalidClass.java", invalidJavaCode)
        
        when: "analyzing the invalid Java file"
        def results = service.analyzeJavaFiles([javaFile])
        
        then: "should handle gracefully and continue"
        // Should not crash, may return empty or partial results
        results != null
    }
    
    def "should analyze multiple Java files"() {
        given: "multiple Java files"
        def classA = '''
            public class ClassA {
                private String name;
                public synchronized void setName(String name) {
                    this.name = name;
                }
            }
        '''
        def classB = '''
            import java.util.concurrent.atomic.AtomicInteger;
            
            public class ClassB {
                private AtomicInteger counter = new AtomicInteger();
            }
        '''
        
        def fileA = createJavaFile("ClassA.java", classA)
        def fileB = createJavaFile("ClassB.java", classB)
        
        when: "analyzing multiple Java files"
        def results = service.analyzeJavaFiles([fileA, fileB])
        
        then: "should analyze all files"
        results.size() == 2
        
        def resultA = results.find { it.filePath.endsWith("ClassA.java") }
        def resultB = results.find { it.filePath.endsWith("ClassB.java") }
        
        resultA.classes[0].name == "ClassA"
        resultA.classes[0].methods.find { it.name == "setName" }.synchronized
        
        resultB.classes[0].name == "ClassB"
        resultB.threadRelatedImports.contains("java.util.concurrent.atomic.AtomicInteger")
    }
    
    def "should handle empty file list"() {
        when: "analyzing empty file list"
        def results = service.analyzeJavaFiles([])
        
        then: "should return empty results"
        results.isEmpty()
    }
    
    @Unroll
    def "should handle various method signatures: #methodCode"() {
        given: "Java class with various method signatures"
        def javaCode = """
            public class MethodVariationsClass {
                $methodCode
            }
        """
        def javaFile = createJavaFile("MethodVariationsClass.java", javaCode)
        
        when: "analyzing the Java file"
        def results = service.analyzeJavaFiles([javaFile])
        
        then: "should parse method correctly"
        results.size() == 1
        def classInfo = results[0].classes[0]
        classInfo.methods.size() >= 1
        
        where:
        methodCode << [
            "public static void staticMethod() {}",
            "private synchronized int syncMethod() { return 0; }",
            "protected void methodWithParams(String arg1, int arg2) {}",
            "public <T> T genericMethod() { return null; }",
            "void packagePrivateMethod() {}"
        ]
    }
    
    // Helper method
    private Path createJavaFile(String fileName, String content) {
        def file = tempDir.resolve(fileName)
        Files.write(file, content.bytes)
        return file
    }
}
