# Java Concurrency Scanner Agent System

An intelligent Java concurrency analysis system built with Spring AI that scans Java codebases and generates detailed HTML reports with class-level analysis, line-by-line details, and AI-powered recommendations for concurrency issues.

## Features

- **Java-Specific Analysis**: Focuses exclusively on Java files and concurrency patterns
- **Comprehensive Concurrency Scanning**: Analyzes 6 key areas:
  - Thread Safety Issues (race conditions, shared mutable state)
  - Synchronization Problems (deadlocks, synchronized blocks)
  - Concurrent Collections Usage (ConcurrentHashMap vs HashMap)
  - Executor Framework Usage (thread pool management)
  - Atomic Operations (AtomicInteger, AtomicReference)
  - Lock Usage (ReentrantLock, ReadWriteLock patterns)
- **Detailed HTML Reports**: Generates rich, interactive HTML reports with Java syntax highlighting
- **Line-by-Line Analysis**: Provides specific file, class, method, and line number references
- **AI-Powered Recommendations**: Uses Spring AI with Anthropic Claude for intelligent suggestions
- **Anthropic Agent Design**: Built following efficient agent design guidelines

## Architecture

The system follows Anthropic's efficient agent design principles:

1. **Modular Design**: Separate components for scanning, analysis, and reporting
2. **Clear Responsibilities**: Each agent has a specific, well-defined role
3. **Error Handling**: Robust error handling and recovery mechanisms
4. **Extensibility**: Easy to add new analysis rules and output formats
5. **Parallel Processing**: Efficient analysis using fork-join patterns

## Technology Stack

- **Java 17+**
- **Spring Boot 3.2+**
- **Spring AI 0.8.1** with Anthropic Claude integration
- **JavaParser** for AST analysis
- **Thymeleaf** for HTML report generation
- **Maven** for dependency management

## Prerequisites

1. Java 17 or higher
2. Maven 3.6+
3. Anthropic API key for AI recommendations

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd java-concurrency-scanner
```

2. Set up your Anthropic API key:
```bash
export ANTHROPIC_API_KEY="your-api-key-here"
```

3. Build the project:
```bash
mvn clean package
```

## Usage

### Basic Usage

```bash
java -jar target/java-concurrency-scanner-1.0.0.jar --scan-path /path/to/java/project --output report.html
```

### With Custom Configuration

```bash
java -jar target/java-concurrency-scanner-1.0.0.jar \
  --scan-path /path/to/java/project \
  --output concurrency-report.html \
  --config custom-config.yaml
```

### Command Line Options

- `--scan-path`: Path to the Java project directory (required)
- `--output`: Output HTML report file path (default: concurrency-report.html)
- `--config`: Configuration YAML file path (default: scanner_config.yaml)

## Configuration

The system uses YAML configuration files. See `src/main/resources/scanner_config.yaml` for options:

```yaml
scanner:
  exclude-test-files: true
  exclude-generated-code: true
  exclude-patterns:
    - "/target/"
    - "/build/"
    - ".git"

analysis:
  thread-safety:
    check-race-conditions: true
    check-shared-mutable-state: true
  synchronization:
    check-deadlock-potential: true
  # ... more options
```

## Example Output

The tool generates a comprehensive HTML report with:

- **Executive Summary**: Overview of files analyzed, issues found, thread-safe vs problematic classes
- **Issue Details**: Each concurrency issue with:
  - Issue type and severity
  - Exact file, class, method, and line number
  - Code snippet with syntax highlighting
  - Suggested fixes
- **AI Recommendations**: Claude-powered suggestions with priority and effort estimates

## Concurrency Issues Detected

### Thread Safety Issues
- Race conditions in shared mutable state
- Unsafe publication of objects
- Non-thread-safe field access patterns

### Synchronization Problems
- Potential deadlock scenarios
- Improper synchronized block usage
- Double-checked locking anti-patterns

### Collection Usage
- Unsafe collections (HashMap, ArrayList) in concurrent contexts
- Missing thread-safe alternatives

### Executor Framework
- Improper thread pool management
- Missing ExecutorService shutdown calls

### Atomic Operations
- Opportunities to use AtomicInteger/AtomicLong
- Compare-and-swap patterns

### Lock Usage
- ReentrantLock without proper try-finally
- Lock ordering issues

## Development

### Project Structure

```
src/
├── main/java/com/example/scanner/
│   ├── JavaConcurrencyScannerApplication.java  # Main application
│   ├── agent/
│   │   └── JavaScannerAgent.java              # Main orchestrator
│   ├── service/
│   │   ├── JavaFileDiscoveryService.java      # File discovery
│   │   ├── JavaSourceAnalysisService.java     # AST parsing
│   │   ├── ConcurrencyAnalysisEngine.java     # Analysis engine
│   │   └── ConcurrencyReportGenerator.java    # Report generation
│   ├── analyzer/                              # Specialized analyzers
│   ├── model/                                # Data models
│   └── config/                               # Configuration
└── main/resources/
    ├── templates/                            # HTML templates
    ├── application.yaml                      # Spring configuration
    └── scanner_config.yaml                  # Scanner configuration
```

### Adding New Analyzers

1. Create a new analyzer class implementing the analysis interface
2. Add it to the `ConcurrencyAnalysisEngine`
3. Update configuration to enable/disable the analyzer

### Extending Reports

1. Modify the Thymeleaf templates in `src/main/resources/templates/`
2. Update the report generator service
3. Add new data models as needed

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
- Create an issue in the GitHub repository
- Check the logs in `scanner.log` for debugging information

## Usage

```bash
python main.py --scan-path /path/to/code --output report.html
```

## Configuration

See `config/scanner_config.yaml` for configuration options.
