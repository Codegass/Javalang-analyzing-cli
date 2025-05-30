package edu.stevens.swe.research.java.cli.analyzer.visitors;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class InvocationVisitor extends ASTVisitor {
    private final List<ASTNode> methods = new ArrayList<>();
    private final List<ASTNode> order = new ArrayList<>();

    @Override
    public boolean visit(MethodInvocation node) {
        methods.add(node);
        if (!order.contains(node)) {
            order.addAll(getPotential(node));
        }
        return true;
    }

    private List<ASTNode> getPotential(ASTNode node) {
        List<ASTNode> invocations = new ArrayList<>();
        Expression caller = null;
        List<Expression> arguments = null; // Changed to List<Expression> for JDT

        if (node instanceof MethodInvocation) {
            MethodInvocation mi = (MethodInvocation) node;
            caller = mi.getExpression();
            arguments = mi.arguments(); // JDT MethodInvocation.arguments() returns List<Expression>
        } else if (node instanceof ClassInstanceCreation) {
            ClassInstanceCreation cic = (ClassInstanceCreation) node;
            caller = cic.getExpression();
            arguments = cic.arguments(); // JDT ClassInstanceCreation.arguments() returns List<Expression>
        } else if (node instanceof ExpressionMethodReference) {
            ExpressionMethodReference emr = (ExpressionMethodReference) node;
            caller = emr.getExpression();
        } else if (node instanceof LambdaExpression) {
            LambdaExpression le = (LambdaExpression) node;
            ASTNode body = le.getBody();
            if (body instanceof Block) {
                Block block = (Block) body; // Corrected cast for Block
                for (Statement stmt : (List<Statement>) block.statements()) {
                    if (stmt instanceof ExpressionStatement) {
                        Expression expr = ((ExpressionStatement) stmt).getExpression();
                        invocations.addAll(getPotential(expr));
                    }
                }
            } else if (body instanceof Expression) {
                Expression exprBody = (Expression) body;
                invocations.addAll(getPotential(exprBody));
            }
        } else if (node instanceof InfixExpression) {
            InfixExpression ie = (InfixExpression) node;
            Expression leftOperand = ie.getLeftOperand();
            Expression rightOperand = ie.getRightOperand();
            invocations.addAll(getPotential(leftOperand));
            invocations.addAll(getPotential(rightOperand));
            // Add extended operands for InfixExpression if any
            if (ie.hasExtendedOperands()) {
                for (Object extOp : ie.extendedOperands()) {
                    if (extOp instanceof Expression) {
                        invocations.addAll(getPotential((Expression) extOp));
                    }
                }
            }
        }

        if (caller != null && (
                caller.getNodeType() == ASTNode.METHOD_INVOCATION ||
                caller.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION ||
                caller.getNodeType() == ASTNode.EXPRESSION_METHOD_REFERENCE ||
                caller.getNodeType() == ASTNode.LAMBDA_EXPRESSION ||
                caller.getNodeType() == ASTNode.INFIX_EXPRESSION
        )) {
            invocations.addAll(getPotential(caller));
        }

        if (arguments != null) {
            for (Expression arg : arguments) { // Changed to Expression
                if (arg.getNodeType() == ASTNode.METHOD_INVOCATION ||
                    arg.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION ||
                    arg.getNodeType() == ASTNode.EXPRESSION_METHOD_REFERENCE ||
                    arg.getNodeType() == ASTNode.LAMBDA_EXPRESSION || // Added Lambda and Infix to argument check
                    arg.getNodeType() == ASTNode.INFIX_EXPRESSION
                ) {
                    invocations.addAll(getPotential(arg));
                }
            }
        }

        // Add the current node itself only if it's a primary callable type
        if (node instanceof MethodInvocation || 
            node instanceof ClassInstanceCreation || 
            node instanceof ExpressionMethodReference || 
            node instanceof NormalAnnotation) { // Added NormalAnnotation as per original logic
            invocations.add(node);
        }

        return invocations;
    }

    private void sortMethods() {
        // Ensure all elements in 'methods' are also in 'order' to avoid indexOf returning -1
        // This can happen if 'getPotential' doesn't add the node itself for certain types
        // or if 'methods.add(node)' happens for nodes not processed by 'getPotential' in the same way.
        order.removeIf(item -> !methods.contains(item)); // Clean up order list
        methods.removeIf(item -> !order.contains(item)); // Ensure consistency

        // Custom comparator to handle items in 'methods' that might not be in 'order'
        // (though the above cleanup should prevent this)
        // Nodes not found in 'order' will be placed at the end.
        Collections.sort(methods, Comparator.comparingInt(n -> {
            int index = order.indexOf(n);
            return index == -1 ? Integer.MAX_VALUE : index;
        }));
    }

    @Override
    public boolean visit(ExpressionMethodReference node) {
        methods.add(node);
        if (!order.contains(node)) {
            order.addAll(getPotential(node));
        }
        return true;
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        methods.add(node);
        if (!order.contains(node)) {
            order.addAll(getPotential(node));
        }
        return true;
    }

    @Override
    public boolean visit(NormalAnnotation node) {
        if ("Test".equals(node.getTypeName().toString())) { // Use equals for string comparison
            List<MemberValuePair> values = node.values();
            for (MemberValuePair v : values) {
                if ("expected".equals(v.getName().toString())) {
                    methods.add(node); // Only add if it's the specific expected annotation
                    // Not running getPotential for annotations as they are not typically nested calls
                    if (!order.contains(node)) {
                         order.add(node); // Add to order for sorting if needed
                    }
                }
            }
        }
        return true;
    }

    public List<ASTNode> getMethods() {
        sortMethods();
        return new ArrayList<>(methods); // Return a copy
    }
} 