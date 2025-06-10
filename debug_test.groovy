@Grab('com.github.javaparser:javaparser-core:3.24.4')

import com.github.javaparser.JavaParser

def code = '''
import java.util.*;

public class UnsafeHashMap {
    private Map<String, String> cache = new HashMap<>();
    
    public void put(String key, String value) {
        cache.put(key, value);
    }
}
'''

def javaParser = new JavaParser()
def compilationUnit = javaParser.parse(code).result.orElseThrow()

println "Compilation unit parsed successfully"
println "Classes found: " + compilationUnit.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class).size()

// Look for fields
def fields = compilationUnit.findAll(com.github.javaparser.ast.body.FieldDeclaration.class)
println "Fields found: " + fields.size()

fields.each { fieldDecl ->
    println "Field: " + fieldDecl.toString()
    println "Element type: " + fieldDecl.getElementType().asString()
    fieldDecl.getVariables().each { variable ->
        println "  Variable: " + variable.getNameAsString()
    }
}
