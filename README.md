# Java Concurrency Scanner AI Agent

An intelligent AI-powered system for analyzing Java code and detecting concurrency-related issues. This tool uses Spring AI with Anthropic Claude to provide detailed analysis and recommendations for thread safety improvements.

## 🚀 Features

- **Comprehensive Concurrency Analysis**: Detects race conditions, deadlocks, and thread safety issues
- **Spring Annotation Filtering**: Focus analysis on Spring-managed components (@Service, @Component, @Repository)
- **AI-Powered Recommendations**: Uses Anthropic Claude for intelligent suggestions and best practices
- **Detailed HTML Reports**: Beautiful, interactive reports with line-by-line analysis and syntax highlighting
- **Multiple Analysis Engines**: 6 specialized analyzers for different concurrency patterns
- **Java AST Parsing**: Deep code analysis using JavaParser with symbol resolution
- **Spring Boot Integration**: Modern, scalable architecture with configuration management
- **Test Coverage**: Comprehensive unit tests for all analyzer components
- **Sample Files**: Ready-to-use test cases demonstrating various concurrency scenarios

## 🏗️ Architecture

The system follows Anthropic's efficient agent design guidelines with a modular architecture:

```
📁 Java Concurrency Scanner
├── 🤖 JavaScannerAgent (Main Orchestrator)
├── 🔍 Analysis Engines
│   ├── ThreadSafetyAnalyzer
│   ├── SynchronizationAnalyzer  
│   ├── ConcurrentCollectionsAnalyzer
│   ├── ExecutorFrameworkAnalyzer
│   ├── AtomicOperationsAnalyzer
│   └── LockUsageAnalyzer
├── 📊 Report Generation (HTML/Thymeleaf)
├── 🧠 AI Recommendations (Anthropic Claude)
└── ⚙️ Configuration Management
```

### Design Principles

The system follows Anthropic's efficient agent design principles:

1. **Modular Design**: Separate components for scanning, analysis, and reporting
2. **Clear Responsibilities**: Each agent has a specific, well-defined role
3. **Error Handling**: Robust error handling and recovery mechanisms
4. **Extensibility**: Easy to add new analysis rules and output formats
5. **Parallel Processing**: Efficient analysis using fork-join patterns

## 💻 Technology Stack

- **Java 17+**
- **Spring Boot 3.2+**
- **Spring AI 0.8.1** with Anthropic Claude integration
- **JavaParser** for AST analysis
- **Thymeleaf** for HTML report generation
- **Maven/Gradle** for dependency management

## ⚡ Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Anthropic API key

### Setup

1. **Clone and build**
   ```bash
   git clone <repository-url>
   cd java-analyzer-agent
   mvn clean install
   ```

2. **Configure API Key**
   ```bash
   export ANTHROPIC_API_KEY="your-api-key-here"
   ```

3. **Run analysis**
   ```bash
   # Test with included samples
   mvn spring-boot:run -Dspring-boot.run.arguments="test"
   
   # Analyze a specific directory
   mvn spring-boot:run -Dspring-boot.run.arguments="/path/to/java/project"
   
   # Use the JAR
   java -jar target/java-concurrency-scanner-1.0.0.jar ./test-samples
   ```

### Command Line Usage

```bash
# Basic usage
java -jar target/java-concurrency-scanner-1.0.0.jar --scan-path /path/to/java/project --output report.html

# With custom configuration
java -jar target/java-concurrency-scanner-1.0.0.jar \
  --scan-path /path/to/java/project \
  --output concurrency-report.html \
  --config custom-config.yaml
```

### Command Line Options

- `--scan-path`: Path to the Java project directory (required)
- `--output`: Output HTML report file path (default: concurrency-report.html)
- `--config`: Configuration YAML file path (default: scanner_config.yaml)

## 🎯 Spring Annotation Filtering

Focus your analysis on Spring-managed components for better signal-to-noise ratio:

```java
// Enable Spring filtering to analyze only annotated classes
JavaSourceAnalysisService service = new JavaSourceAnalysisService();
service.setSpringFilterEnabled(true);

// Supported annotations: @Service, @Component, @Repository, 
// @Controller, @RestController, @Configuration
```

**Benefits:**
- **Focused Analysis**: Target business-critical Spring components
- **Performance**: Faster analysis by processing fewer classes
- **Better Results**: Higher quality findings in your service layer

**Demo:**
```bash
./gradlew runSpringFilterDemo
```

See [SPRING_FILTERING.md](SPRING_FILTERING.md) for detailed documentation.

## 🔍 Analysis Categories

### 1. Thread Safety Issues
- Race conditions in shared mutable state
- Non-atomic read-modify-write operations
- Improper use of volatile keyword
- Visibility problems in multi-threaded access
- Unsafe publication of objects

### 2. Synchronization Problems
- Potential deadlock scenarios
- Nested synchronization blocks
- Synchronization on `this` or class objects
- Missing synchronization for shared data
- Double-checked locking anti-patterns

### 3. Concurrent Collections Usage
- Use of non-thread-safe collections (ArrayList, HashMap)
- Legacy synchronized collections (Vector, Hashtable)
- Improper iteration over synchronized collections
- Recommendations for modern concurrent collections

### 4. Executor Framework Patterns
- ExecutorService not properly shut down
- Inappropriate thread pool sizes
- Missing error handling in tasks
- Resource leak detection
- Improper thread pool management

### 5. Atomic Operations
- Opportunities to use atomic classes
- Compound operations that should be atomic
- Performance improvements with lock-free operations
- Compare-and-swap patterns

### 6. Lock Usage Patterns
- Proper ReentrantLock usage
- ReadWriteLock optimization opportunities
- Lock ordering to prevent deadlocks
- Try-with-resources for automatic lock management
- ReentrantLock without proper try-finally

## ⚙️ Configuration

### Scanner Configuration (`scanner_config.yaml`)

```yaml
scanner:
  analysis:
    max-file-size-mb: 10
    max-files: 1000
    include-patterns:
      - "**/*.java"
    exclude-patterns:
      - "**/target/**"
      - "**/build/**"
      - "**/*Test.java"
  
  exclude-test-files: true
  exclude-generated-code: true
  exclude-patterns:
    - "/target/"
    - "/build/"
    - ".git"
  
  # Spring annotation filtering
  spring-filter:
    enabled: false  # Set to true to analyze only Spring-managed classes
    annotations:
      - "Service"
      - "Component"
      - "Repository"
      - "Controller"
      - "RestController"
      - "Configuration"
  
  rules:
    thread-safety:
      check-static-fields: true
      check-instance-fields: true
      detect-race-conditions: true
      check-shared-mutable-state: true
    
    synchronization:
      detect-deadlocks: true
      check-nested-sync: true
      warn-sync-on-this: true
      check-deadlock-potential: true
```

### AI Configuration (`application.yaml`)

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-3-sonnet-20240229
          temperature: 0.3
          max-tokens: 1000
```

## 📊 Report Generation

The scanner generates comprehensive HTML reports with:

- **Executive Summary**: High-level overview of findings with statistics
- **Class-Level Analysis**: Per-file breakdown of thread safety status
- **Issue Details**: Line-by-line analysis with severity levels
- **AI Recommendations**: Intelligent suggestions for improvements with priority and effort estimates
- **Code Snippets**: Syntax-highlighted problematic code sections
- **Statistics**: Scan metrics and performance data
- **Interactive Navigation**: Easy browsing between sections

Reports are generated in the project root with timestamps for easy tracking.

## 📊 Sample Output

### Console Output
```
=== Java Concurrency Scanner Analysis ===
Directory: ./test-samples
Total Issues: 12
├── HIGH severity: 4
├── MEDIUM severity: 6
└── LOW severity: 2

Files Scanned: 2
Lines Analyzed: 298
Scan Duration: 1,250ms

=== Top Issues ===
1. Race condition in shared counter (ConcurrencyIssuesExample.java:42)
2. Potential deadlock scenario (ConcurrencyIssuesExample.java:156)
3. Non-thread-safe ArrayList usage (ConcurrencyIssuesExample.java:78)

Report generated: concurrency-analysis-2025-06-19-143022.html
```

### HTML Report Features
- **Executive Summary**: Overview of files analyzed, issues found, thread-safe vs problematic classes
- **Issue Details**: Each concurrency issue with:
  - Issue type and severity
  - Exact file, class, method, and line number
  - Code snippet with syntax highlighting
  - Suggested fixes
- **AI Recommendations**: Claude-powered suggestions with priority and effort estimates

## 🧪 Testing

### Test the Scanner with Samples
```bash
# Run analysis on included problematic Java files
mvn spring-boot:run -Dspring-boot.run.arguments="test"
```

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Run All Tests
```bash
./gradlew test --continue
```

## 📁 Project Structure

```
src/
├── main/java/com/example/scanner/
│   ├── JavaConcurrencyScannerApplication.java  # Main Spring Boot app
│   ├── ScannerTestRunner.java                  # Test runner component
│   ├── agent/
│   │   └── JavaScannerAgent.java              # Main orchestrator agent
│   ├── service/
│   │   ├── JavaFileDiscoveryService.java      # File discovery & filtering
│   │   ├── JavaSourceAnalysisService.java     # AST parsing & analysis
│   │   ├── ConcurrencyAnalysisEngine.java     # Analysis coordination
│   │   └── ConcurrencyReportGenerator.java    # HTML report generation
│   ├── analyzer/                              # Specialized analyzers
│   │   ├── ThreadSafetyAnalyzer.java
│   │   ├── SynchronizationAnalyzer.java
│   │   ├── ConcurrentCollectionsAnalyzer.java
│   │   ├── ExecutorFrameworkAnalyzer.java
│   │   ├── AtomicOperationsAnalyzer.java
│   │   └── LockUsageAnalyzer.java
│   ├── model/                                 # Data models
│   │   ├── AnalysisResult.java
│   │   ├── ConcurrencyIssue.java
│   │   ├── ConcurrencyRecommendation.java
│   │   ├── JavaSourceInfo.java
│   │   └── ScanStatistics.java
│   └── config/
│       └── ScannerConfiguration.java         # Configuration management
└── main/resources/
    ├── templates/                            # Thymeleaf HTML templates
    │   ├── concurrency-report.html          # Main report template
    │   ├── empty-report.html                # No issues found template
    │   └── error-report.html                # Error reporting template
    ├── application.yaml                      # Spring configuration
    └── scanner_config.yaml                  # Scanner rules & settings

test-samples/                                 # Sample Java files for testing
├── ConcurrencyIssuesExample.java           # File with various concurrency problems
├── SpringAnnotatedClasses.java             # Spring-annotated classes for filtering demo
└── ThreadSafeExample.java                  # File demonstrating good practices
```

## 🔧 Development

### Adding New Analyzers

1. Create a new analyzer class in the `analyzer` package
2. Implement the analysis logic for your specific concurrency pattern
3. Add it to the `ConcurrencyAnalysisEngine`
4. Create unit tests for the new analyzer
5. Update configuration to enable/disable the analyzer

Example analyzer structure:
```java
@Component
public class MyCustomAnalyzer {
    public List<ConcurrencyIssue> analyze(CompilationUnit cu, JavaSourceInfo sourceInfo) {
        // Your analysis logic here
        return issues;
    }
}
```

### Extending Reports

1. Modify the Thymeleaf templates in `src/main/resources/templates/`
2. Update the `ConcurrencyReportGenerator` service
3. Add new data models as needed

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Add tests for new functionality
4. Ensure all tests pass (`mvn test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For issues and questions:
- Create an issue in the repository
- Include sample code and error logs
- Specify Java version and project details
- Check the application logs for detailed error information
- Check the logs in `scanner.log` for debugging information

## 🙏 Acknowledgments

- Built with [Spring AI](https://spring.io/projects/spring-ai) for intelligent analysis
- Uses [JavaParser](https://github.com/javaparser/javaparser) for AST parsing
- Powered by [Anthropic Claude](https://www.anthropic.com/) for AI recommendations
- Following [Anthropic's Agent Design Guidelines](https://docs.anthropic.com/claude/docs/agent-design)
