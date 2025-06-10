package com.example.scanner.service;

import org.thymeleaf.context.Context;

/**
 * Interface for template processing to enable easier testing and abstraction
 * from the concrete TemplateEngine implementation.
 */
public interface TemplateProcessor {
    
    /**
     * Process a template with the given context and return the rendered result.
     * 
     * @param templateName the name of the template to process
     * @param context the context containing variables for template rendering
     * @return the rendered template content as a string
     */
    String process(String templateName, Context context);
}
