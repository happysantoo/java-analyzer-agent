@Grab('com.github.javaparser:javaparser-core:3.25.0')

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit

def code = '''
import java.util.*;

public class UnsafeHashMap {
    private HashMap<String, String> cache = new HashMap<>();
    
    public void put(String key, String value) {
        cache.put(key, value);
    }
}
'''

def parser = new JavaParser()
def parseResult = parser.parse(code)
def compilationUnit = parseResult.getResult().orElse(null)

if (compilationUnit) {
    println "Compilation successful"
    
    // Extract fields
    def fields = []
    compilationUnit.findAll(com.github.javaparser.ast.body.FieldDeclaration.class).each { fieldDecl ->
        fieldDecl.variables.each { variable ->
            def fieldName = variable.getNameAsString()
            def fieldType = fieldDecl.getElementType().asString()
            def isFinal = fieldDecl.getModifiers().any { it.getKeyword().asString() == "final" }
            
            println "Found field: $fieldName, type: $fieldType, final: $isFinal"
            
            // Check if type contains HashMap
            if (fieldType.contains("HashMap")) {
                println "  -> Contains HashMap: YES"
            } else {
                println "  -> Contains HashMap: NO"
            }
        }
    }
} else {
    println "Failed to parse code"
}
