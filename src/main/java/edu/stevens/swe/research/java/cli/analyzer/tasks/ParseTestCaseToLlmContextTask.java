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
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
        List<String> processedFiles = new ArrayList<>();
        int testCasesFound = 0;

        // Define output directory for JSON files - TEMPORARILY HARDCODED FOR TESTING
        Path outputDir = Paths.get("/Users/chenhaowei/Documents/research-related-data/commons-cli/results");
        System.out.println("[TESTING] Using hardcoded output directory: " + outputDir);

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("Error creating output directory: " + outputDir + " - " + e.getMessage());
            return new TaskResult(projectCtx.getProjectPath().toString(), TASK_NAME + " [Failed to create output directory]");
        }

        // Heuristic: Look for test files in src/test/java
        // TODO: Make source roots configurable via ProjectCtx
        Path testSourceRoot = projectCtx.getProjectPath().resolve("src").resolve("test").resolve("java");
        if (!Files.exists(testSourceRoot)){
            System.out.println("Test source root not found: " + testSourceRoot + ". Skipping task.");
             return new TaskResult(projectCtx.getProjectPath().toString(), TASK_NAME + " [Test source root not found]");
        }

        try (Stream<Path> javaFiles = Files.walk(testSourceRoot).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))) {
            for (Path javaFile : javaFiles.collect(java.util.stream.Collectors.toList())) { // Collect to avoid ConcurrentModificationException if parsing modifies anything globally
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
                            TestCaseAnalyzer.AnalysisResult analysisResult = testCaseAnalyzer.analyzeTestCase(cu, md, originalSource);
                            
                            String jsonFileName = analysisResult.getJsonFileName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".json"; // Sanitize filename
                            Path outputPath = outputDir.resolve(jsonFileName);

                            try (FileWriter writer = new FileWriter(outputPath.toFile())) {
                                gson.toJson(analysisResult, writer);
                                processedFiles.add(outputPath.toString());
                                System.out.println("    Successfully wrote: " + outputPath);
                            } catch (IOException e) {
                                System.err.println("    Error writing JSON for " + analysisResult.getJsonFileName() + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (IOException | ProjectDetectionException e) {
                    System.err.println("Error processing file " + javaFile + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error walking through test source files: " + e.getMessage());
            return new TaskResult(projectCtx.getProjectPath().toString(), TASK_NAME + " [Error reading source files]");
        }

        // TaskResult might need to be enhanced to store these details
        // For now, just returning a basic success/failure message based on file processing.
        String summaryMessage = String.format("%s: Found %d test cases, Generated %d JSON files. Output dir: %s", 
                                        TASK_NAME, testCasesFound, processedFiles.size(), outputDir.toString());
        boolean success = !processedFiles.isEmpty() || testCasesFound == 0; // Consider success if files were made or no tests to process
        
        TaskResult result = new TaskResult(projectCtx.getProjectPath().toString(), summaryMessage);
        // result.addDetail("Test cases found", String.valueOf(testCasesFound));
        // result.addDetail("JSON files generated", String.valueOf(processedFiles.size()));
        // result.addDetail("Output directory", outputDir.toString());
        // if (processedFiles.isEmpty() && testCasesFound > 0) {
        //      result.addDetail("Status", "Completed with errors (no files generated despite finding tests).");
        // } else if (processedFiles.isEmpty() && testCasesFound == 0) {
        //      result.addDetail("Status", "Completed (no test cases found).");
        // } else {
        //     result.addDetail("Status", "Completed successfully.");
        // }
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
} 