package edu.stevens.swe.research.java.cli.analyzer.visitors;

import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class GenericMethodVisitor extends ASTVisitor {
    private MethodDeclaration method = null;
    private final IMethodBinding bindingToFind;
    private final ITypeBinding[] targetParameterTypes;

    public GenericMethodVisitor(IMethodBinding bindingToFind) {
        this.bindingToFind = bindingToFind;
        this.targetParameterTypes = bindingToFind.getParameterTypes();
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        if (match(node)) {
            method = node;
            return false; // Found the method, stop visiting further MethodDeclarations
        }
        return super.visit(node); // Continue visiting other nodes or children
    }

    public MethodDeclaration getMethod() {
        return method;
    }

    private boolean match(MethodDeclaration md) {
        if (!md.getName().getIdentifier().equals(bindingToFind.getName())) {
            return false;
        }

        List<SingleVariableDeclaration> declaredParameters = md.parameters();
        if (targetParameterTypes.length != declaredParameters.size()) {
            return false;
        }

        ITypeBinding[] declaredParameterTypes = new ITypeBinding[declaredParameters.size()];
        for (int i = 0; i < declaredParameters.size(); i++) {
            Type type = declaredParameters.get(i).getType();
            ITypeBinding typeBinding = type.resolveBinding();
            if (typeBinding == null) {
                // Cannot resolve type binding for declared parameter, consider it a non-match
                // or handle with more sophisticated logic if partial matches are allowed.
                return false; 
            }
            declaredParameterTypes[i] = typeBinding;
        }
        
        return allParametersMatch(targetParameterTypes, declaredParameterTypes);
    }

    private boolean allParametersMatch(ITypeBinding[] types1, ITypeBinding[] types2) {
        if (types1.length != types2.length) {
            return false;
        }
        for (int i = 0; i < types1.length; i++) {
            if (!singleParameterMatch(types1[i], types2[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean singleParameterMatch(ITypeBinding type1, ITypeBinding type2) {
        if (type1 == null || type2 == null) return false; // Should not happen with resolved bindings

        // If type2 is a type variable (e.g., T from a generic declaration) and type1 is its erasure or a specific substitution,
        // direct isEqualTo might fail. The JDT ITypeBinding.isEqualTo() is generally robust.
        // For more complex generic scenarios, ITypeBinding.isSubTypeCompatible() or custom logic might be needed.
        // The original code had a simple `itb2.getModifiers() == 0` check which is unreliable for generic matching.
        // Using isEqualTo is a good starting point. Add more complex logic if needed.
        if (type1.isEqualTo(type2)) {
            return true;
        }

        // Fallback: check binary names if isEqualTo fails (e.g. due to different binding contexts)
        // This is a simplified check and might not cover all generic cases perfectly.
        String binaryName1 = type1.getQualifiedName(); // Using qualified name for better comparison
        String binaryName2 = type2.getQualifiedName(); 

        if (!binaryName1.equals(binaryName2)) {
            return false;
        }

        // If they are parameterized types, also check their type arguments.
        ITypeBinding[] typeArgs1 = type1.getTypeArguments();
        ITypeBinding[] typeArgs2 = type2.getTypeArguments();

        return allParametersMatch(typeArgs1, typeArgs2);
    }
} 