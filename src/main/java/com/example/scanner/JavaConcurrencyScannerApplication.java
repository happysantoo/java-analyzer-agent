package com.example.scanner;

import com.example.scanner.agent.JavaScannerAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

@SpringBootApplication
public class JavaConcurrencyScannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaConcurrencyScannerApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(JavaScannerAgent scannerAgent) {
        return args -> {
            if (args.length == 0) {
                System.out.println("Usage: java -jar scanner.jar --scan-path <path> --output <output.html>");
                System.out.println("Example: java -jar scanner.jar --scan-path ./src --output ./report.html");
                return;
            }
            
            String scanPath = null;
            String outputPath = null;
            String configPath = "scanner_config.yaml"; // Default config
            
            // Parse command line arguments
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--scan-path":
                        if (i + 1 < args.length) scanPath = args[++i];
                        break;
                    case "--output":
                        if (i + 1 < args.length) outputPath = args[++i];
                        break;
                    case "--config":
                        if (i + 1 < args.length) configPath = args[++i];
                        break;
                }
            }
            
            if (scanPath == null || outputPath == null) {
                System.err.println("Error: Both --scan-path and --output are required");
                return;
            }
            
            try {
                scannerAgent.executeConcurrencyAnalysis(scanPath, outputPath, configPath);
            } catch (Exception e) {
                System.err.println("Error during analysis: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
    
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        // This will be auto-configured by Spring Boot
        return ChatClient.builder(chatModel).build();
    }
}
