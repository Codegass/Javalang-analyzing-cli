package edu.stevens.swe.research.java.cli.analyzer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data structure for logging analysis session information
 */
public class LogData {
    private String projectName;
    private String taskName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;
    private int totalTestCases;
    private int processedTestCases;
    private int unresolvedInvocationCount;
    private List<UnresolvedCase> unresolvedCases;
    private String status; // "COMPLETED", "INTERRUPTED", "FAILED"
    private String errorMessage;

    public LogData(String projectName, String taskName) {
        this.projectName = projectName;
        this.taskName = taskName;
        this.startTime = LocalDateTime.now();
        this.unresolvedCases = new ArrayList<>();
        this.status = "RUNNING";
    }

    public void finish(String status) {
        this.endTime = LocalDateTime.now();
        this.status = status;
        this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
    }

    public void finish(String status, String errorMessage) {
        finish(status);
        this.errorMessage = errorMessage;
    }

    public void addUnresolvedCase(String className, String methodName, String fileName, int startLine, int endLine, List<String> unresolvedInvocations) {
        UnresolvedCase unresolvedCase = new UnresolvedCase();
        unresolvedCase.className = className;
        unresolvedCase.methodName = methodName;
        unresolvedCase.fileName = fileName;
        unresolvedCase.startLine = startLine;
        unresolvedCase.endLine = endLine;
        unresolvedCase.unresolvedInvocations = new ArrayList<>(unresolvedInvocations);
        unresolvedCase.unresolvedCount = unresolvedInvocations.size();
        
        this.unresolvedCases.add(unresolvedCase);
        this.unresolvedInvocationCount += unresolvedInvocations.size();
    }

    public void writeToFile(Path outputDir, String projectName) throws IOException {
        // Sanitize project name for cross-platform filename compatibility
        String sanitizedProjectName = projectName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String logFileName = sanitizedProjectName + "-log.json";
        Path logFilePath = outputDir.resolve(logFileName);
        
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        
        try (FileWriter writer = new FileWriter(logFilePath.toFile())) {
            gson.toJson(this, writer);
        }
        
        System.out.println("Analysis log written to: " + logFilePath);
    }

    // Getters and setters
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    
    public int getTotalTestCases() { return totalTestCases; }
    public void setTotalTestCases(int totalTestCases) { this.totalTestCases = totalTestCases; }
    
    public int getProcessedTestCases() { return processedTestCases; }
    public void setProcessedTestCases(int processedTestCases) { this.processedTestCases = processedTestCases; }
    
    public int getUnresolvedInvocationCount() { return unresolvedInvocationCount; }
    public void setUnresolvedInvocationCount(int unresolvedInvocationCount) { this.unresolvedInvocationCount = unresolvedInvocationCount; }
    
    public List<UnresolvedCase> getUnresolvedCases() { return unresolvedCases; }
    public void setUnresolvedCases(List<UnresolvedCase> unresolvedCases) { this.unresolvedCases = unresolvedCases; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /**
     * Inner class for unresolved test cases
     */
    public static class UnresolvedCase {
        public String className;
        public String methodName;
        public String fileName;
        public int startLine;
        public int endLine;
        public int unresolvedCount;
        public List<String> unresolvedInvocations;
    }

    /**
     * Custom adapter for LocalDateTime serialization
     */
    private static class LocalDateTimeAdapter implements com.google.gson.JsonSerializer<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        @Override
        public com.google.gson.JsonElement serialize(LocalDateTime localDateTime, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(formatter.format(localDateTime));
        }
    }
} 