package com.example.scanner.analyzer

import com.example.scanner.model.ConcurrencyIssue
import com.example.scanner.model.JavaSourceInfo
import com.example.scanner.model.ClassInfo
import com.example.scanner.model.FieldInfo
import com.example.scanner.model.IssueSeverity

def analyzer = new ConcurrentCollectionsAnalyzer()

// Test safe collections
def classInfo = new ClassInfo()
classInfo.setName("SafeCollections")

def field1 = new FieldInfo()
field1.setName("cache")
field1.setType("ConcurrentHashMap<String, String>")
field1.setFinal(false)
field1.setVolatile(false)
field1.setStatic(false)
field1.setLineNumber(10)

def field2 = new FieldInfo()
field2.setName("items")
field2.setType("CopyOnWriteArrayList<String>")
field2.setFinal(false)
field2.setVolatile(false)
field2.setStatic(false)
field2.setLineNumber(11)

def field3 = new FieldInfo()
field3.setName("queue")
field3.setType("LinkedBlockingQueue<String>")
field3.setFinal(false)
field3.setVolatile(false)
field3.setStatic(false)
field3.setLineNumber(12)

classInfo.setFields([field1, field2, field3])

def sourceInfo = new JavaSourceInfo()
sourceInfo.fileName = "SafeCollections.java"
sourceInfo.filePath = "SafeCollections.java"

def issues = analyzer.analyze(sourceInfo, classInfo)

println "Number of issues found: ${issues.size()}"
issues.each { issue ->
    println "Issue: ${issue.type} - ${issue.description}"
}
