package com.example.scanner.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.Modifier;

import com.example.scanner.model.JavaSourceInfo;
import com.example.scanner.model.ClassInfo;
import com.example.scanner.model.MethodInfo;
import com.example.scanner.model.FieldInfo;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;

/**
 * Service responsible for parsing Java source files and extracting structural information.
 * Implements the Java Source Analysis partition from the activity diagram.
 */
@Service
public class JavaSourceAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(JavaSourceAnalysisService.class);
    
    private final JavaParser javaParser = new JavaParser();
    
    // Thread-related imports to identify
    private static final Set<String> THREAD_RELATED_IMPORTS = Set.of(
        "java.util.concurrent",
        "java.lang.Thread",
        "java.util.concurrent.atomic",
        "java.util.concurrent.locks",
        "java.util.concurrent.Executor",
        "java.util.concurrent.Future",
        "java.util.concurrent.CompletableFuture"
    );
    
    // Spring annotations to filter for concurrency analysis
    private static final Set<String> SPRING_MANAGED_ANNOTATIONS = Set.of(
        "Service", "Component", "Repository", "Controller", "RestController", "Configuration"
    );
    
    // Configuration flag to enable/disable Spring filtering
    private boolean enableSpringFilter = false;
    
    /**
     * Analyzes a list of Java files and extracts source information for concurrency analysis.
     */
    public List<JavaSourceInfo> analyzeJavaFiles(List<Path> javaFiles) {
        List<JavaSourceInfo> results = new ArrayList<>();
        
        for (Path javaFile : javaFiles) {
            try {
                JavaSourceInfo sourceInfo = analyzeJavaFile(javaFile);
                if (sourceInfo != null) {
                    results.add(sourceInfo);
                    logger.debug("Successfully analyzed: {}", javaFile);
                }
            } catch (Exception e) {
                logger.error("Failed to analyze Java file: {}", javaFile, e);
                // Continue with other files (graceful degradation)
            }
        }
        
        logger.info("Successfully analyzed {} out of {} Java files", results.size(), javaFiles.size());
        return results;
    }
    
    /**
     * Analyzes a single Java file using JavaParser AST.
     */
    private JavaSourceInfo analyzeJavaFile(Path javaFile) throws IOException {
        String content = Files.readString(javaFile);
        ParseResult<CompilationUnit> parseResult = javaParser.parse(content);
        
        if (!parseResult.isSuccessful()) {
            logger.warn("Failed to parse Java file: {} - {}", javaFile, parseResult.getProblems());
            return null;
        }
        
        CompilationUnit cu = parseResult.getResult().orElse(null);
        if (cu == null) {
            return null;
        }
        
        JavaSourceInfo sourceInfo = new JavaSourceInfo();
        sourceInfo.setFilePath(javaFile.toString());
        sourceInfo.setContent(content);
        
        // Extract thread-related imports
        sourceInfo.setThreadRelatedImports(extractThreadRelatedImports(cu));
        
        // Extract class declarations and inheritance hierarchy
        sourceInfo.setClasses(extractClassInfo(cu));
        
        logger.debug("Analyzed Java file: {} with {} classes", javaFile, sourceInfo.getClasses().size());
        return sourceInfo;
    }
    
    /**
     * Extracts thread-related imports from the compilation unit.
     */
    private Set<String> extractThreadRelatedImports(CompilationUnit cu) {
        Set<String> threadImports = new HashSet<>();
        
        cu.getImports().forEach(importDecl -> {
            String importName = importDecl.getNameAsString();
            for (String threadRelated : THREAD_RELATED_IMPORTS) {
                if (importName.startsWith(threadRelated)) {
                    threadImports.add(importName);
                    logger.debug("Found thread-related import: {}", importName);
                }
            }
        });
        
        return threadImports;
    }
    
    /**
     * Extracts class information including methods and fields.
     * Filters classes based on Spring annotations if enableSpringFilter is true.
     */
    private List<ClassInfo> extractClassInfo(CompilationUnit cu) {
        List<ClassInfo> classes = new ArrayList<>();
        
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            ClassInfo classInfo = new ClassInfo();
            classInfo.setName(classDecl.getNameAsString());
            classInfo.setInterface(classDecl.isInterface());
            classInfo.setLineNumber(classDecl.getBegin().map(pos -> pos.line).orElse(0));
            
            // Extract Spring annotations
            extractSpringAnnotations(classDecl, classInfo);
            
            // Filter based on Spring annotations if enabled
            if (enableSpringFilter && !classInfo.isSpringManaged()) {
                logger.debug("Skipping non-Spring managed class: {}", classInfo.getName());
                return; // Skip this class
            }
            
            // Extract inheritance information
            classDecl.getExtendedTypes().forEach(extendedType -> 
                classInfo.getParentClasses().add(extendedType.getNameAsString()));
            
            classDecl.getImplementedTypes().forEach(implementedType -> 
                classInfo.getImplementedInterfaces().add(implementedType.getNameAsString()));
            
            // Extract methods
            classInfo.setMethods(extractMethodInfo(classDecl));
            
            // Extract fields
            classInfo.setFields(extractFieldInfo(classDecl));
            
            classes.add(classInfo);
            
            if (classInfo.isSpringManaged()) {
                logger.debug("Extracted Spring-managed class: {} with annotations: {} - {} methods and {} fields", 
                    classInfo.getName(), classInfo.getSpringAnnotations(), 
                    classInfo.getMethods().size(), classInfo.getFields().size());
            } else {
                logger.debug("Extracted class: {} with {} methods and {} fields", 
                    classInfo.getName(), classInfo.getMethods().size(), classInfo.getFields().size());
            }
        });
        
        return classes;
    }
    
    /**
     * Extracts method information including synchronized keywords.
     */
    private List<MethodInfo> extractMethodInfo(ClassOrInterfaceDeclaration classDecl) {
        List<MethodInfo> methods = new ArrayList<>();
        
        classDecl.findAll(MethodDeclaration.class).forEach(methodDecl -> {
            MethodInfo methodInfo = new MethodInfo();
            methodInfo.setName(methodDecl.getNameAsString());
            methodInfo.setLineNumber(methodDecl.getBegin().map(pos -> pos.line).orElse(0));
            methodInfo.setSynchronized(methodDecl.hasModifier(Modifier.Keyword.SYNCHRONIZED));
            methodInfo.setStatic(methodDecl.hasModifier(Modifier.Keyword.STATIC));
            methodInfo.setReturnType(methodDecl.getTypeAsString());
            
            // Extract parameter types
            methodDecl.getParameters().forEach(param -> 
                methodInfo.getParameterTypes().add(param.getTypeAsString()));
            
            methods.add(methodInfo);
        });
        
        return methods;
    }
    
    /**
     * Extracts field information including volatile and static modifiers.
     */
    private List<FieldInfo> extractFieldInfo(ClassOrInterfaceDeclaration classDecl) {
        List<FieldInfo> fields = new ArrayList<>();
        
        classDecl.findAll(FieldDeclaration.class).forEach(fieldDecl -> {
            fieldDecl.getVariables().forEach(variable -> {
                FieldInfo fieldInfo = new FieldInfo();
                fieldInfo.setName(variable.getNameAsString());
                fieldInfo.setType(fieldDecl.getElementType().asString());
                fieldInfo.setLineNumber(fieldDecl.getBegin().map(pos -> pos.line).orElse(0));
                fieldInfo.setVolatile(fieldDecl.hasModifier(Modifier.Keyword.VOLATILE));
                fieldInfo.setStatic(fieldDecl.hasModifier(Modifier.Keyword.STATIC));
                fieldInfo.setFinal(fieldDecl.hasModifier(Modifier.Keyword.FINAL));
                
                fields.add(fieldInfo);
            });
        });
        
        return fields;
    }
    
    /**
     * Extracts Spring annotations from a class declaration.
     */
    private void extractSpringAnnotations(ClassOrInterfaceDeclaration classDecl, ClassInfo classInfo) {
        classDecl.getAnnotations().forEach(annotation -> {
            String annotationName = annotation.getNameAsString();
            
            // Check if it's a Spring managed annotation
            if (SPRING_MANAGED_ANNOTATIONS.contains(annotationName)) {
                classInfo.addSpringAnnotation(annotationName);
                logger.debug("Found Spring annotation @{} on class: {}", annotationName, classInfo.getName());
            }
        });
    }
    
    /**
     * Sets whether to filter classes based on Spring annotations.
     * @param enabled true to analyze only Spring-managed classes, false to analyze all classes
     */
    public void setSpringFilterEnabled(boolean enabled) {
        this.enableSpringFilter = enabled;
        logger.info("Spring annotation filtering {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Returns whether Spring annotation filtering is enabled.
     */
    public boolean isSpringFilterEnabled() {
        return enableSpringFilter;
    }
}
