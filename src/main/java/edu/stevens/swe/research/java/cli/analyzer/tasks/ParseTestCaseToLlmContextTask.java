package edu.stevens.swe.research.java.cli.analyzer.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.stevens.swe.research.java.cli.analyzer.ProjectCtx;
import edu.stevens.swe.research.java.cli.analyzer.TaskResult;
import edu.stevens.swe.research.java.cli.analyzer.core.AstParserUtil;
import edu.stevens.swe.research.java.cli.analyzer.core.TestCaseAnalyzer;
import edu.stevens.swe.research.java.cli.analyzer.spi.AnalyzerTask;
import edu.stevens.swe.research.java.cli.analyzer.visitors.MethodVisitor;
import edu.stevens.swe.research.java.parser.core.utils.exceptions.ProjectDetectionException;
import edu.stevens.swe.research.java.cli.analyzer.LogData;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.NormalAnnotation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ParseTestCaseToLlmContextTask implements AnalyzerTask {
    private static final String TASK_NAME = "ParseTestCaseToLlmContext";

    @Override
    public String getName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(ProjectCtx projectCtx) {
        System.out.println("Executing task: " + TASK_NAME);
        AstParserUtil astParserUtil = new AstParserUtil(projectCtx);
        TestCaseAnalyzer testCaseAnalyzer = new TestCaseAnalyzer(astParserUtil, projectCtx);
        LogData logData = projectCtx.getLogData(); // Get log data from project context
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
        List<String> processedFiles = new ArrayList<>();
        int testCasesFound = 0;
        int testCasesProcessed = 0;

        // Get output directory from ProjectCtx
        Path outputDir = projectCtx.getOutputDirectory();
        if (outputDir == null) {
            // Fallback to default if not set (should not happen with current implementation)
            outputDir = projectCtx.getProjectPath().resolve("AAA");
        }
        System.out.println("Using output directory: " + outputDir);

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("Error creating output directory: " + outputDir + " - " + e.getMessage());
            return new TaskResult(projectCtx.getProjectPath().toString(), TASK_NAME + " [Failed to create output directory]");
        }

        // Find all test directories in the project (supports mono repo)
        List<Path> testSourceRoots = findAllTestDirectories(projectCtx.getProjectPath());
        if (testSourceRoots.isEmpty()) {
            System.out.println("No test source roots found in project: " + projectCtx.getProjectPath() + ". Skipping task.");
            if (logData != null) {
                logData.setTotalTestCases(0);
                logData.setProcessedTestCases(0);
            }
            return new TaskResult(projectCtx.getProjectPath().toString(), TASK_NAME + " [No test source roots found]");
        }

        System.out.println("Found " + testSourceRoots.size() + " test source directories:");
        for (Path testRoot : testSourceRoots) {
            System.out.println("  - " + testRoot);
        }

        // Process all test directories
        for (Path testSourceRoot : testSourceRoots) {
            try (Stream<Path> javaFiles = Files.walk(testSourceRoot)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))) {
                
                for (Path javaFile : javaFiles.collect(java.util.stream.Collectors.toList())) {
                    System.out.println("Processing file: " + javaFile);
                    try {
                        AstParserUtil.ParseResult parseResult = astParserUtil.parse(javaFile.toString());
                        CompilationUnit cu = parseResult.compilationUnit;
                        String originalSource = parseResult.originalSource;

                        if (cu == null) {
                            System.err.println("Failed to parse file: " + javaFile);
                            continue;
                        }

                        MethodVisitor methodVisitor = new MethodVisitor();
                        cu.accept(methodVisitor);

                        for (MethodDeclaration md : methodVisitor.getMethods()) {
                            if (isTestMethod(md)) {
                                testCasesFound++;
                                System.out.println("  Found test method: " + md.getName().getIdentifier());
                                
                                try {
                                    TestCaseAnalyzer.AnalysisResult analysisResult = testCaseAnalyzer.analyzeTestCase(cu, md, originalSource);
                                    
                                    // Check for unresolved invocations
                                    List<String> unresolvedInvocations = findUnresolvedInvocations(analysisResult);
                                    if (!unresolvedInvocations.isEmpty() && logData != null) {
                                        String className = analysisResult.testClassName;
                                        String methodName = analysisResult.testCaseName;
                                        // Use relative path to avoid Windows path separator issues in JSON
                                        String fileName = projectCtx.getProjectPath().relativize(javaFile).toString();
                                        int startLine = cu.getLineNumber(md.getStartPosition());
                                        int endLine = cu.getLineNumber(md.getStartPosition() + md.getLength() - 1);
                                        
                                        logData.addUnresolvedCase(className, methodName, fileName, startLine, endLine, unresolvedInvocations);
                                        System.out.println("    Found " + unresolvedInvocations.size() + " unresolved invocations");
                                    }
                                    
                                    // Enhanced filename sanitization for Windows compatibility
                                    String jsonFileName = analysisResult.getJsonFileName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".json";
                                    Path outputPath = outputDir.resolve(jsonFileName);

                                    try (FileWriter writer = new FileWriter(outputPath.toFile())) {
                                        gson.toJson(analysisResult, writer);
                                        processedFiles.add(outputPath.toString());
                                        testCasesProcessed++;
                                        System.out.println("    Successfully wrote: " + outputPath);
                                    } catch (IOException e) {
                                        System.err.println("    Error writing JSON for " + analysisResult.getJsonFileName() + ": " + e.getMessage());
                                    }
                                } catch (Exception e) {
                                    System.err.println("    Error analyzing test method " + md.getName().getIdentifier() + ": " + e.getMessage());
                                }
                            }
                        }
                    } catch (IOException | ProjectDetectionException e) {
                        System.err.println("Error processing file " + javaFile + ": " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Error walking through test source files in " + testSourceRoot + ": " + e.getMessage());
            }
        }

        // Update log data with statistics
        if (logData != null) {
            logData.setTotalTestCases(testCasesFound);
            logData.setProcessedTestCases(testCasesProcessed);
        }

        // TaskResult might need to be enhanced to store these details
        // For now, just returning a basic success/failure message based on file processing.
        String summaryMessage = String.format("%s: Found %d test cases in %d directories, Generated %d JSON files. Output dir: %s", 
                                        TASK_NAME, testCasesFound, testSourceRoots.size(), processedFiles.size(), outputDir.toString());
        boolean success = !processedFiles.isEmpty() || testCasesFound == 0; // Consider success if files were made or no tests to process
        
        TaskResult result = new TaskResult(projectCtx.getProjectPath().toString(), summaryMessage);
        System.out.println(summaryMessage);
        return result;
    }

    private boolean isTestMethod(MethodDeclaration md) {
        // Check for @Test annotation (JUnit 4 & 5, TestNG)
        // This is a simplified check. A more robust check would resolve bindings to ensure it's the correct @Test annotation.
        boolean isTest = false;
        for (Object modifier : md.modifiers()) {
            if (modifier instanceof Annotation) {
                Annotation annotation = (Annotation) modifier;
                String annotationName = annotation.getTypeName().getFullyQualifiedName();
                
                // Check for @Disabled (JUnit 5)
                if ("Disabled".equals(annotationName) || "org.junit.jupiter.api.Disabled".equals(annotationName)) {
                    System.out.println("  Skipping disabled test method: " + md.getName().getIdentifier() + " due to @Disabled annotation.");
                    return false; // Explicitly not a test method to be processed if disabled
                }
                
                if ("Test".equals(annotationName) || 
                    "org.junit.Test".equals(annotationName) || 
                    "org.junit.jupiter.api.Test".equals(annotationName) || 
                    "org.testng.annotations.Test".equals(annotationName)) {
                    isTest = true;
                    // Don't return true immediately, continue checking for @Disabled
                }
            }
        }
        // Optionally, add checks for public void methods starting with "test" as a convention, if desired.
        return isTest;
    }

    /**
     * Recursively find all test directories in the project.
     * Supports mono repo by searching for all directories matching test patterns.
     */
    private List<Path> findAllTestDirectories(Path projectRoot) {
        List<Path> testDirectories = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(projectRoot)) {
            List<Path> candidateDirectories = paths.filter(Files::isDirectory)
                 .filter(this::isTestDirectory)
                 .collect(java.util.stream.Collectors.toList());
            
            // Remove nested directories - only keep the most specific src/test/java directories
            for (Path candidate : candidateDirectories) {
                boolean isNested = false;
                for (Path other : candidateDirectories) {
                    if (!candidate.equals(other) && candidate.startsWith(other)) {
                        isNested = true;
                        break;
                    }
                }
                if (!isNested) {
                    testDirectories.add(candidate);
                }
            }
        } catch (IOException e) {
            System.err.println("Error walking project directory: " + e.getMessage());
        }
        
        return testDirectories;
    }

    /**
     * Check if a directory is a test directory based on common patterns.
     */
    private boolean isTestDirectory(Path dir) {
        String dirName = dir.getFileName().toString();
        String pathString = dir.toString();
        
        // Normalize path separators for cross-platform compatibility
        String normalizedPath = pathString.replace('\\', '/');
        
        // Skip hidden directories and common non-source directories
        if (dirName.startsWith(".") || 
            normalizedPath.contains("/.") ||
            normalizedPath.contains("/target/") ||
            normalizedPath.contains("/build/") ||
            normalizedPath.contains("/bin/") ||
            normalizedPath.contains("/out/") ||
            normalizedPath.contains("/node_modules/") ||
            normalizedPath.contains("/.git/") ||
            normalizedPath.contains("/.gradle/") ||
            normalizedPath.contains("/.m2/")) {
            return false;
        }
        
        // Standard Maven/Gradle test directory patterns (cross-platform)
        if (normalizedPath.endsWith("/src/test/java")) {
            return true;
        }
        
        // Additional test directory patterns for different project structures
        // Be more specific to avoid false positives
        if (normalizedPath.contains("/src/test/") && normalizedPath.endsWith("/java") && 
            !normalizedPath.contains("/build/") && !normalizedPath.contains("/target/")) {
            return true;
        }
        
        // Check for Maven/Gradle multi-module patterns like module-name/src/test/java
        if (dirName.equals("java")) {
            Path parent = dir.getParent();
            if (parent != null && parent.getFileName().toString().equals("test")) {
                Path srcParent = parent.getParent();
                if (srcParent != null && srcParent.getFileName().toString().equals("src")) {
                    // Ensure it's not in a build directory (using normalized path)
                    if (!normalizedPath.contains("/build/") && !normalizedPath.contains("/target/") && !normalizedPath.contains("/bin/")) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Find unresolved invocations in the analysis result
     */
    private List<String> findUnresolvedInvocations(TestCaseAnalyzer.AnalysisResult analysisResult) {
        List<String> unresolvedInvocations = new ArrayList<>();
        
        for (String statement : analysisResult.parsedStatementsSequence) {
            if (statement.contains("UNRESOLVED_INVOCATION") || 
                statement.contains("UNRESOLVED_CONSTRUCTOR") ||
                statement.contains("UNRESOLVED_METHOD_REF") ||
                statement.contains("METHOD_NOT_FOUND_IN_SOURCE") ||
                statement.contains("SOURCE_FILE_NOT_FOUND")) {
                unresolvedInvocations.add(statement);
            }
        }
        
        return unresolvedInvocations;
    }
} 