package edu.stevens.swe.research.java.cli.analyzer.spi;

import org.eclipse.jdt.core.dom.ASTNode;
import edu.stevens.swe.research.java.parser.core.ResearchParser; // For parser-core facade

/**
 * Service Provider Interface for AST visitors.
 * AST visitors perform fine-grained AST traversal and analysis.
 * Implementing classes will typically provide specific visitNode methods for different ASTNode types
 * (e.g., visitNode(MethodDeclaration md)).
 */
public interface AstVisitor {

    /**
     * Returns the unique name of this visitor.
     * This name can be used to identify the visitor in configurations or task definitions.
     * @return The unique name of the visitor.
     */
    String getName();

    /**
     * A generic visit method that can be called for any AST node.
     * Implementations should check the type of the node and cast it to the specific type they are interested in,
     * or provide overloaded methods for specific ASTNode subtypes.
     *
     * @param node The ASTNode being visited.
     */
    void visitNode(ASTNode node);

    /**
     * Sets the ResearchParser facade instance for this visitor.
     * This allows visitors to access parser functionalities if needed, though typically the AST is already provided.
     * The framework (e.g., TaskManager or a specific task) will call this method to inject the parser facade.
     *
     * @param parser The ResearchParser instance.
     */
    void setParserFacade(ResearchParser parser);

    // Other potential lifecycle methods or configuration methods can be added here.
    // For example, a method to receive ProjectCtx or an IssueReporter as shown in design.md example.
    // void init(ProjectCtx context, IssueReporter reporter);
} 