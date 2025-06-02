package edu.stevens.swe.research.java.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import edu.stevens.swe.research.java.cli.analyzer.ProjectCtx;
import edu.stevens.swe.research.java.cli.analyzer.TaskManager;
import edu.stevens.swe.research.java.cli.analyzer.TaskResult;
import edu.stevens.swe.research.java.cli.analyzer.ResultFormatter;
import edu.stevens.swe.research.java.cli.analyzer.LogData;

@Command(name = "analyzer", mixinStandardHelpOptions = true, version = "Analyzer CLI 1.3.1",
        description = "Analyzes Java source code based on specified tasks.")
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "The task to execute (e.g., DetectAAA).")
    private String taskName;

    @Option(names = {"--project"}, required = true, description = "Absolute path to the project directory.")
    private File projectDir;

    @Option(names = {"--threads"}, defaultValue = "0", description = "Number of threads to use. 0 means use CPU cores.")
    private int threads;

    @Option(names = {"--config"}, description = "Path to the YAML/JSON configuration file.")
    private File configFile;

    @Option(names = {"--format"}, defaultValue = "json", description = "Output format (json, csv, md, console).")
    private String outputFormat;

    @Option(names = {"--lang"}, defaultValue = "java", description = "Programming language (java, go, python).")
    private String language;
    
    @Option(names = {"--output-file"}, description = "Path to the output file.")
    private File outputFile;

    @Option(names = {"--output-dir"}, description = "Directory for task output files. Default: <project>/AAA")
    private File outputDir;

    @Option(names = {"--plugin-path"}, description = "Path to the directory containing plugin JARs.")
    private File pluginPath;

    private TaskManager taskManager;
    private LogData logData;
    private volatile boolean normalExit = false; // Flag to track normal exit

    public Main() {
        // TaskManager will be initialized in call() after ProjectCtx is created
    }

    @Override
    public Integer call() throws Exception {
        // Extract project name from directory
        String projectName = projectDir.getName();
        
        // Initialize logging
        logData = new LogData(projectName, taskName);
        
        System.out.println("Analyzer CLI starting...");
        System.out.println("Task: " + taskName);
        System.out.println("Project Directory: " + projectDir.getAbsolutePath());
        System.out.println("Project Name: " + projectName);
        System.out.println("Language: " + language);
        System.out.println("Threads: " + (threads == 0 ? "Default (CPU Cores)" : threads));
        if (configFile != null) {
            System.out.println("Config File: " + configFile.getAbsolutePath());
        }
        System.out.println("Output Format: " + outputFormat);
        if (outputFile != null) {
            System.out.println("Output File: " + outputFile.getAbsolutePath());
        }
        if (pluginPath != null) {
            System.out.println("Plugin Path: " + pluginPath.getAbsolutePath());
            // TODO: Add logic to load plugins from pluginPath if ServiceLoader doesn't suffice
            // (e.g., if plugins are not on the main classpath)
        }

        // Set default output directory if not specified
        if (outputDir == null) {
            outputDir = new File(projectDir, "AAA");
        }
        System.out.println("Output Directory: " + outputDir.getAbsolutePath());

        // Add shutdown hook to handle interruption
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (logData != null && !normalExit) {
                try {
                    logData.finish("INTERRUPTED", "Process was interrupted by user");
                    logData.writeToFile(outputDir.toPath(), projectName);
                } catch (Exception e) {
                    System.err.println("Error writing log during shutdown: " + e.getMessage());
                }
            }
        }));

        try {
            // 1. Create ProjectCtx
            ProjectCtx projectCtx = new ProjectCtx(projectDir.toPath(), language);
            // Set the output directory in ProjectCtx
            projectCtx.setOutputDirectory(outputDir.toPath());
            // Add log data to project context for tasks to access
            projectCtx.setLogData(logData);
            // TODO: Populate ProjectCtx further from configFile if provided
            // For example, load source roots, classpath, specific task configs etc.

            // 2. Initialize TaskManager
            this.taskManager = new TaskManager(projectCtx, threads);

            // 3. Get the specified task
            // AnalyzerTask task = taskManager.getTask(taskName); // This is done in TaskManager.executeTask

            // 4. Execute the task
            System.out.println("Executing task via TaskManager: " + taskName);
            TaskResult result = taskManager.executeTask(taskName);

            if (result == null) {
                System.err.println("Task execution failed or returned null result for task: " + taskName);
                logData.finish("FAILED", "Task execution returned null result");
                // Create a minimal result to avoid NullPointerException with formatter
                result = new TaskResult(projectCtx.getProjectPath().toString(), taskName + " [Execution Failed]");
            } else {
                logData.finish("COMPLETED");
            }

            // 5. Format and output results
            ResultFormatter formatter = new ResultFormatter(outputFormat);
            try (OutputStream os = (outputFile != null) ? new FileOutputStream(outputFile) : System.out) {
                formatter.format(result, os); // Use the actual result from taskManager
            } catch (Exception e) {
                System.err.println("Error formatting or writing results: " + e.getMessage());
                e.printStackTrace();
                logData.finish("FAILED", "Error formatting results: " + e.getMessage());
                return 1;
            }

            // Write log file
            try {
                logData.writeToFile(outputDir.toPath(), projectName);
            } catch (Exception e) {
                System.err.println("Error writing analysis log: " + e.getMessage());
                e.printStackTrace();
                // Don't fail the entire process for log writing issues
            }

            System.out.println("Analysis finished.");
            normalExit = true; // Mark as normal exit
            return 0;

        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            e.printStackTrace();
            if (logData != null) {
                try {
                    logData.finish("FAILED", "Unexpected error: " + e.getMessage());
                    logData.writeToFile(outputDir.toPath(), projectName);
                } catch (Exception logException) {
                    System.err.println("Error writing error log: " + logException.getMessage());
                }
            }
            return 1;
        } finally {
            if (taskManager != null) {
                taskManager.shutdown(); // Shutdown the executor service
            }
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}