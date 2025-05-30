package edu.stevens.swe.research.java.cli.analyzer.spi;

// TODO: Define ProjectCtx and a generic CollectorResult or use Map<String, Object>
// import edu.stevens.swe.research.java.cli.analyzer.ProjectCtx;

/**
 * Service Provider Interface for data collectors.
 * Collectors are responsible for gathering specific pieces of information,
 * often without performing full AST traversal (though they might use parsers or other tools).
 */
public interface Collector {

    /**
     * Executes the data collection logic.
     *
     * @param projectCtx The context of the project.
     * @param params Optional parameters to configure the collector's behavior.
     *               For example, the 'target' mentioned in the DeclarativeTask example.
     * @return An object containing the collected data. The exact type will depend on the collector.
     *         It could be a custom result object, a Map, or a simple value.
     * @throws Exception if an error occurs during collection.
     */
    // Object collect(ProjectCtx projectCtx, Map<String, Object> params) throws Exception;

    /**
     * Returns the name of the collector. This name is used to identify the collector in configurations.
     * @return The name of the collector.
     */
    String getName();
} 