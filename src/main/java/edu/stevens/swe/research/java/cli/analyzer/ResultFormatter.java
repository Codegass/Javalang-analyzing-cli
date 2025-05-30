package edu.stevens.swe.research.java.cli.analyzer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
// TODO: Add a JSON library like Jackson or Gson for JSON formatting if not already a dependency.
// import com.fasterxml.jackson.databind.ObjectMapper; 
// import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Formats TaskResult objects into various output formats (JSON, CSV, Markdown, Console).
 */
public class ResultFormatter {

    private final String formatType; // "json", "csv", "md", "console"

    public ResultFormatter(String formatType) {
        this.formatType = formatType != null ? formatType.toLowerCase() : "console";
    }

    public void format(TaskResult result, OutputStream outputStream) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            switch (formatType) {
                case "json":
                    formatJson(result, writer);
                    break;
                case "csv":
                    formatCsv(result, writer);
                    break;
                case "md":
                case "markdown":
                    formatMarkdown(result, writer);
                    break;
                case "console":
                default:
                    formatConsole(result, writer);
                    break;
            }
            writer.flush();
        }
    }

    private void formatJson(TaskResult result, PrintWriter writer) throws IOException {
        // TODO: Implement JSON formatting using a library like Jackson or Gson
        // ObjectMapper objectMapper = new ObjectMapper();
        // objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        // writer.write(objectMapper.writeValueAsString(result));
        writer.println("{\n  \"project\": \"" + result.getProject() + "\",");
        writer.println("  \"task\": \"" + result.getTask() + "\",");
        writer.println("  \"cases\": [" );
        List<TaskResult.TestCaseResult> cases = result.getCases();
        for (int i = 0; i < cases.size(); i++) {
            TaskResult.TestCaseResult tc = cases.get(i);
            writer.println("    {");
            writer.println("      \"class\": \"" + tc.getClazz() + "\",");
            writer.println("      \"method\": \"" + tc.getMethod() + "\",");
            writer.println("      \"file\": \"" + tc.getFile() + "\",");
            writer.println("      \"startLine\": " + tc.getStartLine() + ",");
            writer.println("      \"endLine\": " + tc.getEndLine() + ",");
            writer.println("      \"issues\": [" );
            List<TaskResult.Issue> issues = tc.getIssues();
            for (int j = 0; j < issues.size(); j++) {
                TaskResult.Issue issue = issues.get(j);
                writer.println("        {");
                writer.println("          \"rule\": \"" + issue.getRule() + "\",");
                writer.println("          \"startLine\": " + issue.getStartLine() + ",");
                writer.println("          \"endLine\": " + issue.getEndLine());
                writer.print("        }");
                if (j < issues.size() - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
            }
            writer.println("      ]");
            writer.print("    }");
            if (i < cases.size() - 1) {
                writer.println(",");
            } else {
                writer.println();
            }
        }
        writer.println("  ]");
        writer.println("}");
        System.out.println("JSON output (manual basic) written. Consider using a library for robust JSON.");
    }

    private void formatCsv(TaskResult result, PrintWriter writer) {
        // Header
        writer.println("Project,Task,CaseClass,CaseMethod,CaseFile,CaseStartLine,CaseEndLine,IssueRule,IssueStartLine,IssueEndLine");
        if (result.getCases() != null) {
            for (TaskResult.TestCaseResult tc : result.getCases()) {
                if (tc.getIssues() != null && !tc.getIssues().isEmpty()) {
                    for (TaskResult.Issue issue : tc.getIssues()) {
                        writer.print(escapeCsv(result.getProject()) + ",");
                        writer.print(escapeCsv(result.getTask()) + ",");
                        writer.print(escapeCsv(tc.getClazz()) + ",");
                        writer.print(escapeCsv(tc.getMethod()) + ",");
                        writer.print(escapeCsv(tc.getFile()) + ",");
                        writer.print(tc.getStartLine() + ",");
                        writer.print(tc.getEndLine() + ",");
                        writer.print(escapeCsv(issue.getRule()) + ",");
                        writer.print(issue.getStartLine() + ",");
                        writer.println(issue.getEndLine());
                    }
                } else {
                    // Output case even if no issues, if desired
                    writer.print(escapeCsv(result.getProject()) + ",");
                    writer.print(escapeCsv(result.getTask()) + ",");
                    writer.print(escapeCsv(tc.getClazz()) + ",");
                    writer.print(escapeCsv(tc.getMethod()) + ",");
                    writer.print(escapeCsv(tc.getFile()) + ",");
                    writer.print(tc.getStartLine() + ",");
                    writer.print(tc.getEndLine() + ",");
                    writer.println(",,"); // No issue data
                }
            }
        }
        System.out.println("CSV output generated.");
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\\R", " "); // Replace newlines with space
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            escapedData = "\"" + escapedData.replace("\"", "\"\"") + "\"";
        }
        return escapedData;
    }

    private void formatMarkdown(TaskResult result, PrintWriter writer) {
        writer.println("# Analysis Report");
        writer.println("## Task: " + result.getTask());
        writer.println("### Project: " + result.getProject());
        writer.println();

        if (result.getCases() != null && !result.getCases().isEmpty()) {
            writer.println("## Test Cases Analyzed:");
            for (TaskResult.TestCaseResult tc : result.getCases()) {
                writer.println("### Case: `" + tc.getClazz() + "::" + tc.getMethod() + "`");
                writer.println("- **File**: `" + tc.getFile() + "`");
                writer.println("- **Lines**: " + tc.getStartLine() + " - " + tc.getEndLine());
                if (tc.getIssues() != null && !tc.getIssues().isEmpty()) {
                    writer.println("- **Issues Found**:");
                    writer.println("  | Rule ID       | Start Line | End Line   |");
                    writer.println("  |---------------|------------|------------|");
                    for (TaskResult.Issue issue : tc.getIssues()) {
                        writer.printf("  | %-13s | %-10d | %-10d |\n", 
                            issue.getRule(), issue.getStartLine(), issue.getEndLine());
                    }
                } else {
                    writer.println("- No issues found in this case.");
                }
                writer.println();
            }
        } else {
            writer.println("No test cases were processed or no issues found overall.");
        }
        System.out.println("Markdown output generated.");
    }

    private void formatConsole(TaskResult result, PrintWriter writer) {
        writer.println("===========================================");
        writer.println("          ANALYSIS TASK REPORT             ");
        writer.println("===========================================");
        writer.println("Project: " + result.getProject());
        writer.println("Task: " + result.getTask());
        writer.println("-------------------------------------------");

        if (result.getCases() == null || result.getCases().isEmpty()) {
            writer.println("No test cases processed or no findings.");
        } else {
            for (TaskResult.TestCaseResult tc : result.getCases()) {
                writer.println("Case: " + tc.getClazz() + "::" + tc.getMethod());
                writer.println("  File: " + tc.getFile() + " (Lines: " + tc.getStartLine() + "-" + tc.getEndLine() + ")");
                if (tc.getIssues() != null && !tc.getIssues().isEmpty()) {
                    writer.println("  Issues:");
                    for (TaskResult.Issue issue : tc.getIssues()) {
                        writer.println("    - Rule: " + issue.getRule() +
                                   " (Lines: " + issue.getStartLine() + "-" + issue.getEndLine() + ")");
                    }
                } else {
                    writer.println("  No issues found in this case.");
                }
                writer.println("---");
            }
        }
        writer.println("===========================================");
        System.out.println("Console output generated.");
    }
} 