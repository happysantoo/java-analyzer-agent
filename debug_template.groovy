#!/usr/bin/env groovy

// Quick script to debug the template generation
import com.example.scanner.service.ConcurrencyReportGenerator
import com.example.scanner.config.ThymeleafTemplateProcessor
import org.thymeleaf.TemplateEngine
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

// Setup template engine
def templateResolver = new ClassLoaderTemplateResolver()
templateResolver.setPrefix("templates/")
templateResolver.setSuffix(".html")
templateResolver.setTemplateMode("HTML")

def templateEngine = new SpringTemplateEngine()
templateEngine.setTemplateResolver(templateResolver)

def templateProcessor = new ThymeleafTemplateProcessor(templateEngine)
def generator = new ConcurrencyReportGenerator(templateProcessor)

// Create test data similar to the test
println "Generating test HTML to debug template content..."

try {
    generator.generateEmptyReport("/tmp/test-empty.html")
    println "Empty report generated at /tmp/test-empty.html"
    
    def content = new File("/tmp/test-empty.html").text
    println "Generated content contains:"
    println "- 'No Java Files Found': ${content.contains('No Java Files Found')}"
    println "- 'Generated on:': ${content.contains('Generated on:')}"
    
} catch (Exception e) {
    println "Error: ${e.message}"
    e.printStackTrace()
}
