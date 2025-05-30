# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2024-05-30

### ✨ New Features
- **JUnit Lifecycle Annotation Detection**: Added support for detecting and extracting JUnit lifecycle methods
  - `@Before`/`@BeforeEach`: Methods executed before each test
  - `@BeforeAll`/`@BeforeClass`: Methods executed once before all tests
  - `@After`/`@AfterEach`: Methods executed after each test  
  - `@AfterAll`/`@AfterClass`: Methods executed once after all tests
  - Supports both JUnit 4 and JUnit 5 annotations
  - Lifecycle method source code is included in JSON output with field names: `beforeMethods`, `beforeAllMethods`, `afterMethods`, `afterAllMethods`

- **🏗️ Mono Repository (Mono Repo) Support**: Major enhancement for multi-module projects
  - **Recursive Test Discovery**: Automatically finds all test directories across the entire project tree
  - **Multi-Module Detection**: Supports Maven and Gradle multi-module project structures
  - **Smart Filtering**: Intelligently skips build output directories (`/build/`, `/target/`, `/bin/`, `/out/`)
  - **Nested Directory Handling**: Removes duplicate/nested test directories, keeping only the most specific ones
  - **Enhanced Output**: Shows count of discovered test directories in summary
  - **Backward Compatible**: Single-module projects continue to work as before

### 🔧 Technical Improvements
- Enhanced `ParseTestCaseToLlmContextTask` with `findAllTestDirectories()` method
- Improved `isTestDirectory()` logic with comprehensive filtering rules
- Added deduplication algorithm to prevent processing nested test directories
- Better error handling for individual module failures in mono repos

### 📋 Supported Project Structures
- Single module: `project-root/src/test/java/`
- Multi-module Maven: `module-*/src/test/java/`
- Multi-module Gradle: `subproject-*/src/test/java/`
- Nested modules: `backend/api/src/test/java/`, `backend/core/src/test/java/`

### 🚀 Performance
- Processes multiple test directories in sequence
- Maintains individual module error isolation
- Optimized directory traversal with early filtering

### 🔧 Technical Enhancements
- Enhanced `TestCaseAnalyzer` with `extractLifecycleMethods()` function
- Added `extractMethodSourceCode()` helper method for consistent source code extraction
- Extended `AnalysisResult` class with new fields for lifecycle methods

### 📋 Updated Output Format
```json
{
  "beforeMethods": ["@BeforeEach source code"],
  "beforeAllMethods": ["@BeforeAll/@BeforeClass source code"], 
  "afterMethods": ["@AfterEach source code"],
  "afterAllMethods": ["@AfterAll/@AfterClass source code"],
  // ... existing fields
}
```

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