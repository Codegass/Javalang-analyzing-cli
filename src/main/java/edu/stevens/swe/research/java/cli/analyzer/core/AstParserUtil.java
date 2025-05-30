package edu.stevens.swe.research.java.cli.analyzer.core;

import edu.stevens.swe.research.java.cli.analyzer.ProjectCtx;
import edu.stevens.swe.research.java.parser.core.ResearchParser;
import edu.stevens.swe.research.java.parser.core.utils.exceptions.ProjectDetectionException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AstParserUtil {

    // Inner class to hold both CompilationUnit and original source string
    public static class ParseResult {
        public final CompilationUnit compilationUnit;
        public final String originalSource;

        public ParseResult(CompilationUnit compilationUnit, String originalSource) {
            this.compilationUnit = compilationUnit;
            this.originalSource = originalSource;
        }
    }

    private final ProjectCtx projectCtx;
    private final ResearchParser researchParser;

    public AstParserUtil(ProjectCtx projectCtx) {
        this.projectCtx = projectCtx;
        this.researchParser = new ResearchParser();
        // Manual configuration of sourcepath, classpath, encodings is removed
        // as parser-core (ResearchParser) is expected to handle this.
    }

    public ParseResult parse(String filePath) throws IOException, ProjectDetectionException {
        Path path = Paths.get(filePath);
        String sourceCodeString = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        // Get the pre-configured parser from parser-core
        ASTParser parser = researchParser.getParser(projectCtx.getProjectPath());

        // DEBUG: Print detected parser configuration
        try {
            System.out.println("DEBUG: Parser-core detected configuration for project: " + projectCtx.getProjectPath());
            String configDetails = researchParser.getDetectedParserConfigAsString(projectCtx.getProjectPath());
            System.out.println(configDetails);
        } catch (Exception e) {
            System.err.println("DEBUG: Failed to get/print parser-core config details: " + e.getMessage());
        }
        // END DEBUG

        if (parser == null) {
            throw new ProjectDetectionException("Failed to get ASTParser from ResearchParser for project: " + projectCtx.getProjectPath());
        }

        // Set the source code for the specific file to be parsed
        parser.setSource(sourceCodeString.toCharArray());
        
        // Set a unique name for the compilation unit, using absolute path for robustness with bindings
        // This aligns with the example provided for parser.setUnitName in ResearchParser.getTestCases
        parser.setUnitName(path.toAbsolutePath().toString());
        
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        return new ParseResult(cu, sourceCodeString);
    }
} 