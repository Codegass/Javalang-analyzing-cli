# Javalang-analyzing-cli

> A powerful Java code analysis command-line tool designed for research and code quality analysis.

[![License](https://img.shields.io/github/license/your-username/Javalang-analyzing-cli)](LICENSE)
[![Release](https://img.shields.io/github/v/release/your-username/Javalang-analyzing-cli)](https://github.com/your-username/Javalang-analyzing-cli/releases)

## 🚀 Quick Start

### Download

Download the latest `Javalang-analyzing-cli-all.jar` from the [Releases](https://github.com/your-username/Javalang-analyzing-cli/releases) page.

This is a **Fat JAR** that includes all dependencies - no additional installation required.

### System Requirements

- **Java 21** or higher
- Supported OS: Windows, macOS, Linux

### Basic Usage

```bash
# Check version
java -jar Javalang-analyzing-cli-all.jar --version

# Show help
java -jar Javalang-analyzing-cli-all.jar --help

# Analyze test cases and generate LLM context
java -jar Javalang-analyzing-cli-all.jar ParseTestCaseToLlmContext \
    --project /path/to/your/java/project \
    --format json
```

## 📋 Supported Tasks

### ParseTestCaseToLlmContext

Parses Java test cases and outputs structured JSON files for LLM code analysis.

**Output includes**:
- 📝 Test case source code
- 🔍 Method invocation sequence (DFS traversal)
- 📦 Production code implementations
- 🏷️ Statement classification (ASSERT, MOCK, NEW, third-party calls, etc.)
- 📂 Imported packages list

**Example**:
```bash
java -jar Javalang-analyzing-cli-all.jar ParseTestCaseToLlmContext \
    --project /Users/john/projects/commons-cli \
    --threads 8 \
    --format json
```

**Output file format**: `project_name:testclass_name:testcase_name.json`

## 📄 Output Example

```json
{
  "project": "commons-cli",
  "testClassName": "org.apache.commons.cli.ArgumentIsOptionTest", 
  "testCaseName": "testOption",
  "parsedStatementsSequence": [
    "org.apache.commons.cli.CommandLineParser.parse(Options, String[])",
    "ASSERT org.junit.jupiter.api.Assertions.assertTrue(boolean)",
    "GET org.apache.commons.cli.CommandLine.hasOption(String)"
  ],
  "productionFunctionImplementations": [
    "public boolean hasOption(String opt) { ... }"
  ],
  "testCaseSourceCode": "@Test\npublic void testOption() { ... }",
  "importedPackages": [
    "org.junit.jupiter.api.Test",
    "org.junit.jupiter.api.Assertions"
  ]
}
```

## ⚙️ Command Line Options

| Option | Description | Default | Example |
|--------|-------------|---------|---------|
| `--project` | Project root directory (absolute path) | **Required** | `/home/user/my-project` |
| `--threads` | Number of threads to use | `0` (auto-detect CPU cores) | `8` |
| `--format` | Output format | `json` | `json`, `csv`, `md`, `console` |
| `--lang` | Programming language | `java` | `java` |
| `--output-file` | Output file path | Console output | `/tmp/results.json` |
| `--config` | Configuration file path | None | `config.yml` |
| `--plugin-path` | Plugin directory path | None | `/path/to/plugins` |

## 🏗️ Supported Build Systems

The tool automatically detects the project's build system and configures the appropriate classpath:

- ✅ **Maven**: Automatically parses `pom.xml` and dependencies
- ✅ **Gradle**: Uses Gradle Tooling API to get project configuration
- 🔄 **Automatic dependency resolution**: Searches `~/.m2` and `~/.gradle/caches` for missing test dependencies

### Automatically Handled Test Frameworks

- **JUnit 5** (Jupiter)
- **JUnit 4**
- **Mockito**
- **Hamcrest**
- **AssertJ**

## 🛠️ Advanced Usage

### Configuration File

Create an `analyzer.yml` configuration file:

```yaml
project: /path/to/project
threads: 8
lang: java
output:
  format: json
  file: results.json

# Task-specific configuration
ParseTestCaseToLlmContext:
  outputDirectory: /custom/output/path
  includeDisabledTests: false
```

Using configuration file:
```bash
java -jar Javalang-analyzing-cli-all.jar ParseTestCaseToLlmContext --config analyzer.yml
```

### Multi-threaded Processing

```bash
# Use 16 threads for parallel analysis
java -jar Javalang-analyzing-cli-all.jar ParseTestCaseToLlmContext \
    --project /large/project \
    --threads 16
```

### Plugin Extensions

The tool supports extension through Java SPI mechanism:

```bash
# Use custom plugins
java -jar Javalang-analyzing-cli-all.jar CustomTask \
    --plugin-path /path/to/plugins \
    --project /path/to/project
```

## 🏷️ Statement Classification

The tool automatically identifies and classifies statements in test code:

| Classification | Description | Examples |
|----------------|-------------|----------|
| `ASSERT` | Assertion statements | `assertTrue()`, `assertEquals()` |
| `MOCK` | Mock operations | `when()`, `verify()` |
| `NEW` | Object creation | `new Object()` |
| `THIRD` | Third-party library calls | External library method calls |
| `GET/SET` | Property access | `getProperty()`, `setProperty()` |
| `PRODUCTION` | Production code calls | Business logic methods being tested |

## 🐛 Troubleshooting

### Common Issues

**Q: "No suitable build tool found" error**
```
A: Ensure the project root contains pom.xml (Maven) or build.gradle (Gradle) files.
```

**Q: Test dependencies cannot be resolved**
```
A: Ensure the project has been built (mvn compile or gradle build), 
   the tool will automatically search local caches.
```

**Q: Out of memory errors**
```
A: Increase JVM heap memory:
java -Xmx4g -jar Javalang-analyzing-cli-all.jar ...
```

**Q: Unicode escape characters in output**
```
A: This is normal - the tool is configured to output readable characters 
   instead of Unicode escapes.
```

### Debug Mode

The tool outputs detailed debug information including:
- Detected build system
- Parsed dependencies
- Classpath configuration
- List of processed files

This information helps diagnose issues.

## 🔧 Development

### Building the Project

```bash
git clone https://github.com/your-username/Javalang-analyzing-cli.git
cd Javalang-analyzing-cli
./gradlew shadowJar
```

The generated Fat JAR is located at: `build/libs/Javalang-analyzing-cli-*-all.jar`

### Adding New Tasks

1. Implement the `AnalyzerTask` interface
2. Register in `META-INF/services/edu.stevens.swe.research.java.cli.analyzer.spi.AnalyzerTask`
3. Rebuild and test

## 📄 License

This project is open source under the [Apache License 2.0](LICENSE).

## 🤝 Contributing

Issues and Pull Requests are welcome!

## 📞 Support

For issues, please report them in [GitHub Issues](https://github.com/your-username/Javalang-analyzing-cli/issues).