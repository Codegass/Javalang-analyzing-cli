package edu.stevens.swe.research.java.cli.analyzer;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Represents the context of the project being analyzed.
 * This includes information like JDK path, classpath, source roots, language, and temporary directories.
 */
public class ProjectCtx {
    private final Path projectPath;
    private final String language;
    private List<Path> sourceRoots;
    private List<Path> classpath;
    private Path jdkPath; // Optional, might be inferred or configured
    private Path tempDir;
    private Path outputDirectory; // Directory for task output files

    public ProjectCtx(Path projectPath, String language) {
        this.projectPath = projectPath;
        this.language = language;
        // Sensible defaults or further configuration can set these
        // this.sourceRoots = ...;
        // this.classpath = ...;
    }

    // Getters
    public Path getProjectPath() {
        return projectPath;
    }

    public String getLanguage() {
        return language;
    }

    public List<Path> getSourceRoots() {
        return sourceRoots;
    }

    public void setSourceRoots(List<Path> sourceRoots) {
        this.sourceRoots = sourceRoots;
    }

    public List<Path> getClasspath() {
        return classpath;
    }

    public void setClasspath(List<Path> classpath) {
        this.classpath = classpath;
    }

    public Path getJdkPath() {
        return jdkPath;
    }

    public void setJdkPath(Path jdkPath) {
        this.jdkPath = jdkPath;
    }

    public Path getTempDir() {
        if (this.tempDir == null) {
            // Create a default temporary directory if not set
            // For example, under the project path or system temp
            this.tempDir = projectPath.resolve(".analyzer-temp"); 
        }
        // Ensure the temp directory exists
        File tempDirFile = this.tempDir.toFile();
        if (!tempDirFile.exists()) {
            tempDirFile.mkdirs();
        }
        return tempDir;
    }

    public void setTempDir(Path tempDir) {
        this.tempDir = tempDir;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    // TODO: Add methods to help resolve files or paths within the project context
} 