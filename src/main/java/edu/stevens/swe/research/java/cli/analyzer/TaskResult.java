package edu.stevens.swe.research.java.cli.analyzer;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents the result of an analysis task.
 * This structure is designed to be easily serializable to JSON as per the design specification.
 */
public class TaskResult {
    private String project;
    private String task;
    private List<TestCaseResult> cases;

    public TaskResult(String project, String task) {
        this.project = project;
        this.task = task;
        this.cases = new ArrayList<>();
    }

    // Getters and Setters
    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public List<TestCaseResult> getCases() {
        return cases;
    }

    public void setCases(List<TestCaseResult> cases) {
        this.cases = cases;
    }

    public void addCase(TestCaseResult testCase) {
        this.cases.add(testCase);
    }

    /**
     * Represents a single test case analysis result, which may contain multiple issues.
     */
    public static class TestCaseResult {
        private String clazz;
        private String method;
        private String file;
        private int startLine;
        private int endLine;
        private List<Issue> issues;

        public TestCaseResult(String clazz, String method, String file, int startLine, int endLine) {
            this.clazz = clazz;
            this.method = method;
            this.file = file;
            this.startLine = startLine;
            this.endLine = endLine;
            this.issues = new ArrayList<>();
        }

        // Getters and Setters for TestCaseResult fields
        public String getClazz() {
            return clazz;
        }

        public void setClazz(String clazz) {
            this.clazz = clazz;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public int getStartLine() {
            return startLine;
        }

        public void setStartLine(int startLine) {
            this.startLine = startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public void setEndLine(int endLine) {
            this.endLine = endLine;
        }

        public List<Issue> getIssues() {
            return issues;
        }

        public void setIssues(List<Issue> issues) {
            this.issues = issues;
        }
        
        public void addIssue(Issue issue) {
            this.issues.add(issue);
        }
    }

    /**
     * Represents a specific issue found within a test case.
     */
    public static class Issue {
        private String rule;
        private int startLine;
        private int endLine;
        // Potentially add a message field
        // private String message;

        public Issue(String rule, int startLine, int endLine) {
            this.rule = rule;
            this.startLine = startLine;
            this.endLine = endLine;
        }

        // Getters and Setters for Issue fields
        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

        public int getStartLine() {
            return startLine;
        }

        public void setStartLine(int startLine) {
            this.startLine = startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public void setEndLine(int endLine) {
            this.endLine = endLine;
        }
    }
} 