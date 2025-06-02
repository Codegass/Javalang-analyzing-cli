# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.2] - 2025-06-02

### 🐛 Critical Bug Fix - Inner Class Source Resolution
- **Fixed SOURCE_FILE_NOT_FOUND for Inner Classes**: Major fix for source file detection of inner classes
  - **Problem**: Classes like `org.apache.commons.cli.HelpFormatter.Builder` were causing SOURCE_FILE_NOT_FOUND errors
  - **Root Cause**: Method was searching for `org/apache/commons/cli/HelpFormatter/Builder.java` instead of `org/apache/commons/cli/HelpFormatter.java`
  - **Solution**: Enhanced `getSourceFilePathForClass()` to detect inner classes and map them to their outer class source files
  - **Impact**: Dramatically reduces SOURCE_FILE_NOT_FOUND errors in projects with inner classes

### 🔧 Technical Improvements
- **Smart Inner Class Detection**: Added logic to identify inner classes by uppercase naming patterns
- **Outer Class Mapping**: Automatically strips inner class portions to find the correct source file
- **Enhanced Debug Logging**: Added detailed logging for source file resolution process

### 🧪 Verification
- **commons-cli Project**: Tested on Apache Commons CLI project - eliminated most SOURCE_FILE_NOT_FOUND errors
- **Before**: Multiple SOURCE_FILE_NOT_FOUND errors for `HelpFormatter.Builder` methods  
- **After**: All inner class methods now correctly resolve to their source files

This fix significantly improves analysis accuracy for projects using inner classes, which are common in Java projects.

## [1.3.1] - 2025-06-02

### 🐛 Bug Fixes - Windows Path Compatibility
- **Log File Path Handling**: Fixed Windows path compatibility issues in logging system
  - **Relative Path Recording**: Changed from absolute to relative paths in unresolved case logs
  - **Cross-platform File Names**: Enhanced filename sanitization to handle Windows path separators
  - **Log File Creation**: Added project name sanitization for safe log file creation
  
### 🔧 Technical Improvements  
- **Enhanced Filename Sanitization**: Updated regex pattern from `[^a-zA-Z0-9.-]` to `[^a-zA-Z0-9._-]` 
- **Relative Path Storage**: Log entries now store `src/test/java/com/example/Test.java` instead of full paths
- **Project Name Cleaning**: Automatic removal of invalid characters from project names in log filenames

### 🌍 Cross-Platform Benefits
- ✅ **Windows**: Log files now correctly handle `C:\Users\...` style paths  
- ✅ **Unix/Linux/macOS**: Maintains full backward compatibility
- ✅ **JSON Compatibility**: Eliminates escaping issues with backslashes in JSON output

### 📋 Log File Improvements
```json
{
  "unresolvedCases": [
    {
      "fileName": "src/test/java/com/example/Test.java",  // ✅ Relative path
      "className": "com.example.Test",
      "methodName": "testMethod"
    }
  ]
}
```

**Before (v1.3.0)**: `"fileName": "C:\\Users\\user\\project\\src\\test\\java\\Test.java"` ❌  
**After (v1.3.1)**: `"fileName": "src/test/java/Test.java"` ✅

## [1.3.0] - 2025-06-02

### 🚀 Major Features
- **Runtime Logging System**: Added comprehensive analysis session logging
  - **Execution Time Tracking**: Automatic recording of start time, end time, and duration
  - **Test Case Statistics**: Total and processed test case counts
  - **Unresolved Invocation Detection**: Automatic detection and detailed tracking of unresolved method calls
  - **Structured Log Output**: JSON format log files saved to `<output_dir>/<project_name>-log.json`

### ✨ New Features
- **LogData Class**: Dedicated data structure for logging analysis session information
- **Smart Status Tracking**: RUNNING → COMPLETED/FAILED/INTERRUPTED status progression
- **Unresolved Code Analysis**: Identifies and catalogues:
  - `UNRESOLVED_INVOCATION` - Method calls that couldn't be resolved
  - `UNRESOLVED_CONSTRUCTOR` - Constructor calls that couldn't be resolved
  - `UNRESOLVED_METHOD_REF` - Method references that couldn't be resolved
  - `METHOD_NOT_FOUND_IN_SOURCE` - Methods not found in source code
  - `SOURCE_FILE_NOT_FOUND` - Source files that couldn't be located
- **Graceful Interruption Handling**: Shutdown hooks to save logs even when interrupted
- **Project Context Integration**: LogData integrated into ProjectCtx for task access

### 🔧 Technical Improvements
- **Exception Handling**: Robust error handling with detailed error message logging
- **Cross-platform Compatibility**: All logging functionality works on Windows/Linux/macOS
- **Memory Efficient**: Streaming JSON output without memory overhead
- **Thread Safe**: Volatile flags for proper multi-threading support

### 📊 Log File Structure
```json
{
  "projectName": "example-project",
  "taskName": "ParseTestCaseToLlmContext",
  "startTime": "2025-06-02T01:42:33",
  "endTime": "2025-06-02T01:42:34", 
  "durationMs": 1243,
  "totalTestCases": 3,
  "processedTestCases": 3,
  "unresolvedInvocationCount": 2,
  "unresolvedCases": [
    {
      "className": "com.example.SampleTest",
      "methodName": "testWithUnresolvedCode",
      "fileName": "/path/to/SampleTest.java",
      "startLine": 14,
      "endLine": 22,
      "unresolvedCount": 2,
      "unresolvedInvocations": [
        "UNRESOLVED_CONSTRUCTOR: new SomeUnknownClass()#[17-17]",
        "UNRESOLVED_INVOCATION: unknown.someMethod()#[18-18]"
      ]
    }
  ],
  "status": "COMPLETED"
}
```

### 🎯 Use Cases
- **Performance Monitoring**: Track analysis execution time across different projects
- **Quality Assessment**: Identify test cases with resolution issues
- **Problem Debugging**: Pinpoint exact locations of unresolved method calls
- **Project Analytics**: Generate reports on test coverage and code quality

## [1.2.2] - 2024-12-08

## [1.1.2] - 2024-05-30

### 🐛 Bug Fixes
- **Windows Path Compatibility**: Fixed cross-platform path handling issues
  - **Test Directory Detection**: Enhanced `isTestDirectory()` to normalize Windows backslashes to forward slashes
  - **Production/Test Code Classification**: Fixed `isProductionCode()` and `isTestCode()` methods to handle Windows paths
  - **Source File Path Resolution**: Updated `getSourceFilePathForClass()` to use `Path.resolve()` instead of hardcoded path strings
  - **Root Cause**: Previous implementation only handled Unix-style paths (`/`), causing "No test source roots found" on Windows
  - **Solution**: Added path normalization (`pathString.replace('\\', '/')`) throughout path comparison logic

### 🔧 Technical Details
- Modified `ParseTestCaseToLlmContextTask.isTestDirectory()` method
- Enhanced `TestCaseAnalyzer.isProductionCode()` and `isTestCode()` methods
- Updated `TestCaseAnalyzer.getSourceFilePathForClass()` to use proper `Path.resolve()` chains
- All path comparisons now use normalized forward slash format internally
- Maintains full backward compatibility on Unix/Linux/macOS systems

### 🌍 Cross-Platform Support
- ✅ **Windows**: Now correctly identifies test directories like `D:\project\src\test\java`
- ✅ **Unix/Linux**: Continues to work with `/project/src/test/java`  
- ✅ **macOS**: Continues to work with `/project/src/test/java`

## [1.1.1] - 2024-05-30

### 🐛 Bug Fixes
- **Duplicate JSON File Extension**: Fixed issue where output files had double `.json.json` extension
  - Problem: `getJsonFileName()` was returning filename with `.json` extension, then `.json` was added again during file creation
  - Solution: Modified `getJsonFileName()` to return base filename without extension
  - Files now correctly named as `project_class_method.json` instead of `project_class_method.json.json`

### 🔧 Technical Details
- Modified `TestCaseAnalyzer.AnalysisResult.getJsonFileName()` method
- Maintains backward compatibility - no changes to JSON content format
- File sanitization (replacing invalid characters with `_`) still works correctly

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