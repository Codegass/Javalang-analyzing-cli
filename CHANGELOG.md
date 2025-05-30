# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2024-05-30

### 🐛 Bug Fixes
- **Configurable Output Directory**: Fixed hardcoded output directory issue
  - Added `--output-dir` CLI option to specify custom output directory
  - Default output directory is now `<project>/AAA` instead of hardcoded path
  - Output directory is automatically created if it doesn't exist

### ✨ Improvements
- **Enhanced CLI Interface**: Better user control over output location
- **Cross-platform Compatibility**: Removed system-specific hardcoded paths

## [1.0.0] - 2024-05-30

### 🎉 Initial Release

#### ✨ New Features
- **ParseTestCaseToLlmContext Task**: Parses Java test cases and generates LLM-friendly JSON output
- **Intelligent Build System Detection**: Automatically identifies and configures Maven/Gradle projects
- **Enhanced Dependency Resolution**:
  - Maven: Parses dependencies from `pom.xml`, automatically handles property variables
  - Gradle: Uses Tooling API to get project configuration
  - Automatic search of local caches (`~/.m2`, `~/.gradle/caches`) for missing dependencies
- **Automatic Test Framework Support**: JUnit 4/5, Mockito, Hamcrest, AssertJ
- **Intelligent Statement Classification**: ASSERT, MOCK, NEW, THIRD, GET/SET, PRODUCTION
- **DFS Method Call Analysis**: Recursively expands method calls within test cases
- **Multi-threaded Parallel Processing**: Supports concurrent analysis of multiple test files
- **Extensible Plugin Architecture**: Task and visitor extension mechanism based on Java SPI

#### 🔧 Technical Features
- **Fat JAR Distribution**: Includes all dependencies, ready to use out of the box
- **Detailed Debug Logging**: Complete dependency resolution and configuration detection logs
- **Cross-platform Support**: Windows, macOS, Linux
- **Java 21 Support**: Uses latest JDK features

#### 📋 Output Format
```json
{
  "project": "project name",
  "testClassName": "fully qualified test class name",
  "testCaseName": "test method name",
  "parsedStatementsSequence": ["method call sequence"],
  "productionFunctionImplementations": ["production code implementations"],
  "testCaseSourceCode": "test case source code",
  "importedPackages": ["imported packages list"]
}
```

#### 🏗️ Dependencies
- **parser-core**: Core AST parsing engine
- **picocli**: Command line interface framework
- **Eclipse JDT**: Java development tools core
- **Gson**: JSON serialization library

#### 📝 Command Line Interface
```bash
java -jar Javalang-analyzing-cli-1.0.0-all.jar ParseTestCaseToLlmContext \
    --project /path/to/project \
    --threads 8 \
    --format json
```

### 🐛 Known Issues
- Output directory is currently hardcoded to `results/` folder under project root
- Some complex Maven dependency management scenarios may require manual intervention

### 🔄 Future Plans
- Support for Go and Python project analysis
- Configurable output directory
- More built-in analysis tasks
- Web UI interface

---

## Version Format Description

- **[Major.Minor.Patch]** - Release date
- **Major**: Incompatible API changes
- **Minor**: Backward-compatible functionality additions
- **Patch**: Backward-compatible bug fixes 