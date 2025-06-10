package com.example.scanner.service;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Default implementation of TemplateProcessor that wraps Thymeleaf's TemplateEngine.
 */
@Component
public class ThymeleafTemplateProcessor implements TemplateProcessor {
    
    private final TemplateEngine templateEngine;
    
    public ThymeleafTemplateProcessor(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }
    
    @Override
    public String process(String templateName, Context context) {
        return templateEngine.process(templateName, context);
    }
}
