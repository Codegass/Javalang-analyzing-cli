package edu.stevens.swe.research.java.cli.analyzer.spi;

import edu.stevens.swe.research.java.cli.analyzer.ProjectCtx;
import edu.stevens.swe.research.java.cli.analyzer.TaskResult;

// TODO: Define ProjectCtx and TaskResult classes later
// import edu.stevens.swe.research.java.cli.analyzer.ProjectCtx;
// import edu.stevens.swe.research.java.cli.analyzer.TaskResult;

/**
 * Service Provider Interface for analyzer tasks.
 * Implementations of this interface define a specific analysis to be performed on a project.
 */
public interface AnalyzerTask {

    /**
     * Executes the analysis task.
     *
     * @param projectCtx The context of the project being analyzed.
     * @return The result of the task execution.
     * @throws Exception if an error occurs during task execution.
     */
    TaskResult execute(ProjectCtx projectCtx) throws Exception;

    /**
     * Returns the name of the task. This name is used to identify the task in configurations and CLI commands.
     * @return The name of the task.
     */
    String getName();

     /**
     * Initializes the task with necessary configurations.
     * This method could be called by the TaskManager before execute().
     * Parameters would depend on how configuration is passed (e.g., a Map, a dedicated config object).
     */
    // void init(Map<String, Object> config);
} 