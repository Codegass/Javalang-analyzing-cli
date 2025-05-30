package edu.stevens.swe.research.java.cli.analyzer;

import edu.stevens.swe.research.java.cli.analyzer.spi.AnalyzerTask;
import edu.stevens.swe.research.java.cli.analyzer.spi.AstVisitor;
import edu.stevens.swe.research.java.cli.analyzer.spi.Collector;
import edu.stevens.swe.research.java.parser.core.ResearchParser; // For parser-core facade

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the lifecycle of analysis tasks, including loading plugins (via SPI),
 * scheduling tasks, and managing thread pools.
 */
public class TaskManager {
    private final Map<String, AnalyzerTask> taskRegistry = new HashMap<>();
    private final Map<String, AstVisitor> visitorRegistry = new HashMap<>();
    private final Map<String, Collector> collectorRegistry = new HashMap<>();
    private final ExecutorService executorService;
    private final ProjectCtx projectCtx; // Project context for tasks
    private final ResearchParser parserFacade; // Parser facade for visitors

    public TaskManager(ProjectCtx projectCtx, int numThreads) {
        this.projectCtx = projectCtx;
        if (numThreads <= 0) {
            this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        } else {
            this.executorService = Executors.newFixedThreadPool(numThreads);
        }
        this.parserFacade = new ResearchParser(); // Initialize parser facade
        loadPlugins();
    }

    private void loadPlugins() {
        // Load AnalyzerTask plugins
        ServiceLoader<AnalyzerTask> taskLoader = ServiceLoader.load(AnalyzerTask.class);
        for (AnalyzerTask task : taskLoader) {
            taskRegistry.put(task.getName(), task);
            System.out.println("Registered AnalyzerTask: " + task.getName());
        }

        // Load AstVisitor plugins
        ServiceLoader<AstVisitor> visitorLoader = ServiceLoader.load(AstVisitor.class);
        for (AstVisitor visitor : visitorLoader) {
            // AstVisitor SPI doesn't have a getName() yet, we might need to add it or use class name
            // For now, let's use class simple name as a placeholder key if getName() is not available.
            // Ideally, visitors should also have a unique name provided by a getName() method.
            visitor.setParserFacade(this.parserFacade); // Inject parser facade
            // String visitorName = visitor.getClass().getSimpleName(); // Placeholder for a proper name
            // TODO: Add getName() to AstVisitor SPI and use it here.
            // visitorRegistry.put(visitorName, visitor);
            // System.out.println("Registered AstVisitor: " + visitorName);
            String visitorName = visitor.getName();
            if (visitorName == null || visitorName.trim().isEmpty()) {
                System.err.println("Warning: AstVisitor " + visitor.getClass().getName() + " has null or empty name. Skipping registration.");
                continue;
            }
            if (visitorRegistry.containsKey(visitorName)) {
                System.err.println("Warning: Duplicate AstVisitor name '" + visitorName + "' found. Class: " + visitor.getClass().getName() + ". Previous one was: " + visitorRegistry.get(visitorName).getClass().getName() + ". Skipping this one.");
                continue;
            }
            visitorRegistry.put(visitorName, visitor);
            System.out.println("Registered AstVisitor: " + visitorName + " (Class: " + visitor.getClass().getName() + ")");
        }

        // Load Collector plugins
        ServiceLoader<Collector> collectorLoader = ServiceLoader.load(Collector.class);
        for (Collector collector : collectorLoader) {
            collectorRegistry.put(collector.getName(), collector);
            System.out.println("Registered Collector: " + collector.getName());
        }
    }

    public AnalyzerTask getTask(String name) {
        AnalyzerTask task = taskRegistry.get(name);
        if (task == null) {
            System.err.println("Error: Task '" + name + "' not found.");
            // Consider throwing a specific exception like TaskNotFoundException
        }
        return task;
    }

    public AstVisitor getVisitor(String name) {
        AstVisitor visitor = visitorRegistry.get(name);
        if (visitor == null) {
            System.err.println("Error: AST Visitor '" + name + "' not found.");
        }
        return visitor;
    }

    public Collector getCollector(String name) {
        Collector collector = collectorRegistry.get(name);
        if (collector == null) {
            System.err.println("Error: Collector '" + name + "' not found.");
        }
        return collector;
    }

    public TaskResult executeTask(String taskName /*, other params like config */) {
        AnalyzerTask task = getTask(taskName);
        if (task != null) {
            System.out.println("Executing task: " + taskName);
            try {
                // TODO: Initialize task with specific configurations if needed
                // ((ConfigurableTask) task).configure(taskSpecificConfigs);
                return task.execute(this.projectCtx);
            } catch (Exception e) {
                System.err.println("Error executing task " + taskName + ": " + e.getMessage());
                e.printStackTrace();
                // Return an error TaskResult or null, or rethrow a custom exception
                TaskResult errorResult = new TaskResult(projectCtx.getProjectPath().toString(), taskName);
                // Populate with error information if your TaskResult schema supports it
                // errorResult.setErrorDescription(e.getMessage()); 
                return errorResult; // Or null
            }
        }
        System.err.println("Task execution failed for: " + taskName + " (task not found or other issue)");
        return new TaskResult(projectCtx.getProjectPath().toString(), taskName); // Return empty result
    }

    public void shutdown() {
        System.out.println("Shutting down TaskManager executor service...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    System.err.println("Executor service did not terminate.");
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("TaskManager shut down.");
    }

    // TODO: Add methods for result processing and formatting
    // private void processResult(TaskResult result) { ... }
} 