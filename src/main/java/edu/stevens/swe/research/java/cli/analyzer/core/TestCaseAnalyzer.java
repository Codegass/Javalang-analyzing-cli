package edu.stevens.swe.research.java.cli.analyzer.core;

import edu.stevens.swe.research.java.cli.analyzer.ProjectCtx;
import edu.stevens.swe.research.java.cli.analyzer.visitors.GenericMethodVisitor;
import edu.stevens.swe.research.java.cli.analyzer.visitors.InvocationVisitor;
import edu.stevens.swe.research.java.parser.core.utils.exceptions.ProjectDetectionException;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestCaseAnalyzer {

    private final AstParserUtil astParserUtil;
    private final ProjectCtx projectCtx;
    private final String projectRootPath;

    public TestCaseAnalyzer(AstParserUtil astParserUtil, ProjectCtx projectCtx) {
        this.astParserUtil = astParserUtil;
        this.projectCtx = projectCtx;
        this.projectRootPath = projectCtx.getProjectPath().toString();
    }

    public static class AnalysisResult {
        public List<String> parsedStatementsSequence = new ArrayList<>();
        public List<String> productionFunctionImplementations = new ArrayList<>();
        public String testCaseSourceCode = "";
        public List<String> importedPackages = new ArrayList<>();
        public String testClassName = "";
        public String testCaseName = "";
        public String projectName = "";
        public List<String> beforeMethods = new ArrayList<>();
        public List<String> beforeAllMethods = new ArrayList<>();
        public List<String> afterMethods = new ArrayList<>();
        public List<String> afterAllMethods = new ArrayList<>();

        // Helper to create the JSON filename
        public String getJsonFileName() {
            return projectName + ":" + testClassName + ":" + testCaseName;
        }
    }

    public AnalysisResult analyzeTestCase(CompilationUnit testCu, MethodDeclaration testMethodDeclaration, String originalFileSource) throws IOException, ProjectDetectionException {
        AnalysisResult result = new AnalysisResult();
        result.projectName = Paths.get(projectRootPath).getFileName().toString();
        
        if (testMethodDeclaration.resolveBinding() != null && testMethodDeclaration.resolveBinding().getDeclaringClass() != null) {
            result.testClassName = testMethodDeclaration.resolveBinding().getDeclaringClass().getQualifiedName();
        } else if (testCu.getPackage() != null && !testCu.types().isEmpty() && testCu.types().get(0) instanceof AbstractTypeDeclaration) {
            String packageNameStr = testCu.getPackage().getName().getFullyQualifiedName();
            result.testClassName = packageNameStr + "." + ((AbstractTypeDeclaration)testCu.types().get(0)).getName().getIdentifier();
        } else {
            result.testClassName = "UnknownClass";
        }
        result.testCaseName = testMethodDeclaration.getName().getIdentifier();

        // Get test case source code
        int startPos = testMethodDeclaration.getStartPosition();
        int length = testMethodDeclaration.getLength();

        // DEBUGGING OUTPUT
        System.out.println("DEBUG: Analyzing method: " + result.testClassName + "." + result.testCaseName);
        System.out.println("DEBUG: originalFileSource.length(): " + originalFileSource.length());
        System.out.println("DEBUG: Method startPos (from AST): " + startPos);
        System.out.println("DEBUG: Method length (from AST): " + length);
        System.out.println("DEBUG: Calculated end position (startPos + length): " + (startPos + length));
        // END DEBUGGING OUTPUT

        // Use originalFileSource for substring extraction
        if (startPos >= 0 && length > 0 && (startPos + length) <= originalFileSource.length()) {
            result.testCaseSourceCode = originalFileSource.substring(startPos, startPos + length);
        } else {
            System.err.println("Warning: Invalid start/length for test case source code extraction. Method: " + testMethodDeclaration.getName().getIdentifier() + " in class " + result.testClassName);
            System.err.println("  Start: " + startPos + ", Length: " + length + ", File Length: " + originalFileSource.length());
            result.testCaseSourceCode = "// Error extracting source code";
        }

        // Get imported packages
        if (testCu.imports() != null) {
            for (Object imp : testCu.imports()) {
                if (imp instanceof ImportDeclaration) {
                    result.importedPackages.add(((ImportDeclaration) imp).getName().getFullyQualifiedName());
                }
            }
        }

        // Extract lifecycle methods (@Before, @BeforeAll, @After, @AfterAll) from the test class
        extractLifecycleMethods(testCu, result, originalFileSource);

        // Perform DFS-like analysis for parsed_statements_sequence and production_function_implementations
        dfsAnalyze(testCu, testMethodDeclaration, 0, result, result.testClassName + "." + result.testCaseName + getParameters(testMethodDeclaration.resolveBinding()), originalFileSource);
        return result;
    }

    private void dfsAnalyze(CompilationUnit currentCu, MethodDeclaration currentMethod, int level, AnalysisResult result, String originalEntryMethodSignature, String currentOriginalSource) throws IOException, ProjectDetectionException {
        InvocationVisitor visitor = new InvocationVisitor();
        currentMethod.accept(visitor);

        String currentMethodPackageName = "";
        if (currentCu.getPackage() != null) {
            currentMethodPackageName = currentCu.getPackage().getName().getFullyQualifiedName();
        }
        
        IMethodBinding currentMethodBinding = currentMethod.resolveBinding();
        if (currentMethodBinding == null) { 
            // Cannot resolve binding for currentMethod, skip further analysis of this method
            System.err.println("Warning: Could not resolve binding for method: " + currentMethod.getName().getIdentifier());
            return; 
        }
        String currentMethodQualifiedNameAndParam = currentMethodBinding.getDeclaringClass().getQualifiedName() + "." + currentMethod.getName().toString() + getParameters(currentMethodBinding);

        for (ASTNode node : visitor.getMethods()) {
            String statementPrefix = getSpace(level);
            String statementDetails = "";
            String lineNumberRange = getLineNumberRange(currentCu, node);

            if (node instanceof MethodInvocation) {
                MethodInvocation mi = (MethodInvocation) node;
                IMethodBinding binding = mi.resolveMethodBinding();

                if (binding == null) {
                    statementDetails = "UNRESOLVED_INVOCATION: " + mi.toString();
                    result.parsedStatementsSequence.add(statementPrefix + statementDetails + "#" + lineNumberRange);
                    continue;
                }
                
                String calledMethodSignature = binding.getDeclaringClass().getQualifiedName() + "." + mi.getName().toString() + getParameters(binding);
                 // Recursive call detection (comparing against the very first method in the call chain)
                if (calledMethodSignature.equals(originalEntryMethodSignature)) {
                    System.out.println("Recursive call to entry test method detected, adding to sequence and stopping this path: " + calledMethodSignature);
                    result.parsedStatementsSequence.add(statementPrefix + "RECURSIVE_TO_ENTRY " + calledMethodSignature + "#" + lineNumberRange);
                    continue; 
                }
                // Direct recursion for the current method being processed
                if (calledMethodSignature.equals(currentMethodQualifiedNameAndParam)) {
                     System.out.println("Direct recursive call detected, adding to sequence and stopping this path: " + calledMethodSignature);
                    result.parsedStatementsSequence.add(statementPrefix + "DIRECT_RECURSIVE " + calledMethodSignature + "#" + lineNumberRange);
                    continue;
                }

                if (isAssert(mi)) {
                    statementDetails = "ASSERT " + calledMethodSignature;
                } else if (isJunitExpectedExceptionRule(mi, binding)) {
                    statementDetails = "EXPECTEDEXCEPTION_RULE " + calledMethodSignature;
                } else if (isMockingFramework(mi, binding)) {
                    statementDetails = "MOCK " + calledMethodSignature;
                } else if (isThirdParty(binding, currentMethodPackageName)) {
                    if (isGetter(mi, binding)) {
                        statementDetails = "THIRD GET " + calledMethodSignature;
                    } else if (isSetter(mi, binding)) {
                        statementDetails = "THIRD SET " + calledMethodSignature;
                    } else {
                        statementDetails = "THIRD " + calledMethodSignature;
                    }
                } else {
                    // Potentially project code (production or test utility)
                    ITypeBinding declaringClass = binding.getDeclaringClass();
                    if (declaringClass == null) {
                         statementDetails = "UNRESOLVED_DECLARING_CLASS: " + mi.toString();
                         result.parsedStatementsSequence.add(statementPrefix + statementDetails + "#" + lineNumberRange);
                         continue;
                    }
                    String className = declaringClass.getQualifiedName();
                    String sourceFilePath = getSourceFilePathForClass(className, projectRootPath);

                    if (sourceFilePath != null) {
                        AstParserUtil.ParseResult expandedParseResultFromUtil = astParserUtil.parse(sourceFilePath);
                        CompilationUnit expandedCu = expandedParseResultFromUtil.compilationUnit;
                        String expandedOriginalSource = expandedParseResultFromUtil.originalSource; // Original source for the expandedCu

                        MethodDeclaration md = findMethodDeclaration(binding, expandedCu);

                        if (md != null) {
                            if (isProductionCode(sourceFilePath)) {
                                String qualifiedName = md.resolveBinding().getDeclaringClass().getQualifiedName() + "." + md.getName().toString();
                                if (isGetter(mi, binding)) {
                                    statementDetails = "GET " + qualifiedName + getParameters(binding);
                                } else if (isSetter(mi, binding)) {
                                    statementDetails = "SET " + qualifiedName + getParameters(binding);
                                } else {
                                    statementDetails = qualifiedName + getParameters(binding);
                                }

                                // Add production function implementation using expandedOriginalSource
                                int prodStartPos = md.getStartPosition();
                                int prodLength = md.getLength();

                                // DEBUGGING OUTPUT FOR PRODUCTION CODE
                                System.out.println("DEBUG: Production method: " + qualifiedName);
                                System.out.println("DEBUG: expandedOriginalSource.length(): " + expandedOriginalSource.length());
                                System.out.println("DEBUG: Prod method startPos (from AST): " + prodStartPos);
                                System.out.println("DEBUG: Prod method length (from AST): " + prodLength);
                                System.out.println("DEBUG: Prod calculated end position (startPos + length): " + (prodStartPos + prodLength));
                                // END DEBUGGING OUTPUT

                                if (prodStartPos >=0 && prodLength > 0 && (prodStartPos + prodLength) <= expandedOriginalSource.length()) {
                                    result.productionFunctionImplementations.add(expandedOriginalSource.substring(prodStartPos, prodStartPos + prodLength));
                                } else {
                                    System.err.println("Warning: Invalid start/length for production code extraction. Method: " + md.getName().getIdentifier());
                                    result.productionFunctionImplementations.add("// Error extracting source for " + qualifiedName);
                                }
                            } else if (isTestCode(sourceFilePath)) { // Test utility method
                                String qualifiedName = md.resolveBinding().getDeclaringClass().getQualifiedName() + "." + md.getName().toString();
                                if (isGetter(mi, binding)) {
                                    statementDetails = "TEST GET " + qualifiedName + getParameters(binding);
                                } else if (isSetter(mi, binding)) {
                                    statementDetails = "TEST SET " + qualifiedName + getParameters(binding);
                                } else {
                                    statementDetails = "TEST " + qualifiedName + getParameters(binding);
                                }
                                // When recursing into a test utility, pass its own CU and its original source
                                dfsAnalyze(expandedCu, md, level + 1, result, originalEntryMethodSignature, expandedOriginalSource);
                            } else {
                                statementDetails = "UNKNOWN_PROJECT_CODE " + calledMethodSignature; 
                            }
                        } else {
                            statementDetails = "METHOD_NOT_FOUND_IN_SOURCE " + calledMethodSignature;
                        }
                    } else {
                        statementDetails = "SOURCE_FILE_NOT_FOUND " + calledMethodSignature;
                    }
                }
                result.parsedStatementsSequence.add(statementPrefix + statementDetails + "#" + lineNumberRange);

            } else if (node instanceof ClassInstanceCreation) {
                ClassInstanceCreation cic = (ClassInstanceCreation) node;
                IMethodBinding constructorBinding = cic.resolveConstructorBinding();
                if (constructorBinding != null && constructorBinding.getDeclaringClass()!=null) {
                    String declaringClassName = constructorBinding.getDeclaringClass().getQualifiedName();
                    if (declaringClassName.startsWith("java.")) {
                        statementDetails = "NEW_JDK_CLASS " + declaringClassName + getParameters(constructorBinding);
                    } else if (constructorBinding.getDeclaringClass().isAnonymous()){
                        statementDetails = "NEW ANONYMOUS_CLASS" + getParameters(constructorBinding); // Might need more info for anonymous
                    }else {
                        statementDetails = "NEW " + declaringClassName + getParameters(constructorBinding);
                    }
                } else {
                     statementDetails = "UNRESOLVED_CONSTRUCTOR: " + cic.toString();
                }
                result.parsedStatementsSequence.add(statementPrefix + statementDetails + "#" + lineNumberRange);

            } else if (node instanceof ExpressionMethodReference) {
                ExpressionMethodReference emr = (ExpressionMethodReference) node;
                IMethodBinding emrBinding = emr.resolveMethodBinding();
                if (emrBinding != null) {
                    statementDetails = "METHOD_REF " + emrBinding.getDeclaringClass().getQualifiedName() + "." + emrBinding.getName() + getParameters(emrBinding);
                } else {
                    statementDetails = "UNRESOLVED_METHOD_REF: " + emr.toString();
                }
                result.parsedStatementsSequence.add(statementPrefix + statementDetails + "#" + lineNumberRange);

            } else if (node instanceof NormalAnnotation) {
                // This logic is from the old InvocationVisitor, check if it's needed for @Test(expected=...)
                NormalAnnotation na = (NormalAnnotation) node;
                if ("Test".equals(na.getTypeName().toString())) {
                    for (MemberValuePair mvp : (List<MemberValuePair>)na.values()) {
                        if ("expected".equals(mvp.getName().getIdentifier())) {
                            ITypeBinding exceptionType = mvp.getValue().resolveTypeBinding();
                            if (exceptionType != null) {
                                statementDetails = "EXPECTEDEXCEPTION_ANNOTATION " + exceptionType.getQualifiedName();
                                result.parsedStatementsSequence.add(statementPrefix + statementDetails + "#" + lineNumberRange);
                            }
                        }
                    }
                }
            }
        }
    }

    private MethodDeclaration findMethodDeclaration(IMethodBinding binding, CompilationUnit cu) {
        MethodDeclaration md = (MethodDeclaration) cu.findDeclaringNode(binding.getKey());
        if (md == null) {
            GenericMethodVisitor visitor = new GenericMethodVisitor(binding);
            cu.accept(visitor);
            md = visitor.getMethod();
        }
        return md;
    }

    private boolean isMockingFramework(MethodInvocation mi, IMethodBinding binding) {
        String qualifiedName = binding.getDeclaringClass().getQualifiedName();
        return qualifiedName.startsWith("org.mockito.") || 
               qualifiedName.startsWith("org.easymock.") ||
               qualifiedName.startsWith("org.powermock.");
    }

    private boolean isThirdParty(IMethodBinding binding, String projectPackagePrefix) {
        if (binding == null || binding.getDeclaringClass() == null || binding.getDeclaringClass().getPackage() == null) {
            return true; // Default to third-party if info is missing
        }
        String packageName = binding.getDeclaringClass().getPackage().getName();

        // Exclude known testing frameworks that aren't mocks, if they weren't caught by isAssert or isJunitExpectedExceptionRule
        if (packageName.startsWith("org.junit") || packageName.startsWith("junit.framework") || packageName.startsWith("org.testng")) {
            return false; 
        }
        // Exclude mocking frameworks explicitly handled by isMockingFramework
        if (packageName.startsWith("org.mockito") || packageName.startsWith("org.easymock") || packageName.startsWith("org.powermock")) {
            return false;
        }
        // Check if it's a JDK class
        if (packageName.startsWith("java.") || packageName.startsWith("javax.") || packageName.startsWith("com.sun.")) {
             return true; // Consider JDK as third party for this purpose
        }
        // Check against the project's own package prefix
        // This requires a good way to determine the project's base package(s).
        // For now, we use a simple check. This might need to be more robust.
        // if (projectCtx.getProjectBasePackages() != null && !projectCtx.getProjectBasePackages().isEmpty()) { //Temporarily comment out
        //     for (String basePkg : projectCtx.getProjectBasePackages()) {
        //         if (packageName.startsWith(basePkg)) {
        //             return false; // Part of the project
        //         }
        //     }
        // } else 
        if (!projectPackagePrefix.isEmpty() && packageName.startsWith(projectPackagePrefix)) {
             // Fallback if specific base packages aren't defined in ProjectCtx
            return false; // Part of the project (based on current file's package as a heuristic)
        }
        
        return true; // Otherwise, assume third-party
    }

    private boolean isGetter(MethodInvocation mi, IMethodBinding binding) {
        String methodName = mi.getName().getIdentifier();
        return (methodName.startsWith("get") || methodName.startsWith("is")) && 
               binding.getParameterTypes().length == 0 && 
               !"void".equals(binding.getReturnType().getName());
    }

    private boolean isSetter(MethodInvocation mi, IMethodBinding binding) {
        String methodName = mi.getName().getIdentifier();
        return methodName.startsWith("set") && 
               binding.getParameterTypes().length > 0 && // Typically 1, but could be more for fluent setters
               "void".equals(binding.getReturnType().getName());
    }
 
    private static boolean isAssert(MethodInvocation mc) {
        if (mc.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains(".Assert")) {
            return true;
        } else if (mc.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("MatcherAssert")) {
            return true;
        } else if (mc.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("org.hamcrest")) {
            return true;
        } else if (mc.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("org.junit")) {
            return true;
        } else if (mc.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("org.assertj.core.api.Assertions")) {
            return true;
        }

        return false;
    }

    private boolean isJunitExpectedExceptionRule(MethodInvocation mi, IMethodBinding binding) {
        return binding.getDeclaringClass().getQualifiedName().equals("org.junit.rules.ExpectedException");
    }

    private String getLineNumberRange(CompilationUnit cu, ASTNode node) {
        int startLine = cu.getLineNumber(node.getStartPosition());
        int endLine = cu.getLineNumber(node.getStartPosition() + node.getLength() - 1);
        return "[" + startLine + "-" + endLine + "]";
    }

    private String getParameters(IMethodBinding binding) {
        if (binding == null) return "(...)";
        StringBuilder parameters = new StringBuilder("(");
        ITypeBinding[] paramTypes = binding.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            parameters.append(paramTypes[i].getName());
            if (i < paramTypes.length - 1) {
                parameters.append(", ");
            }
        }
        parameters.append(")");
        return parameters.toString();
    }

    private String getSpace(int count) {
        return "    ".repeat(Math.max(0, count));
    }

    // Heuristics to determine if a path is production or test code.
    // These should ideally be configurable or more robustly determined from ProjectCtx.
    private boolean isProductionCode(String filePath) {
        return filePath.contains(File.separator + "src" + File.separator + "main" + File.separator) || 
               !filePath.contains(File.separator + "src" + File.separator + "test" + File.separator); // Assume prod if not explicitly test
    }

    private boolean isTestCode(String filePath) {
        return filePath.contains(File.separator + "src" + File.separator + "test" + File.separator);
    }
    
    // Utility from old MethodAnalyzer to find source file for a class.
    // This might need to be made more robust or use ProjectCtx information.
    public String getSourceFilePathForClass(String className, String sourceRootPath) {
        String classPath = className.replace('.', File.separatorChar) + ".java";
        Path effectiveSourceRoot = Paths.get(sourceRootPath); 

        // Attempt to find in standard main/java or test/java if sourceRootPath is project root
        // This is a heuristic. ProjectCtx should ideally provide specific source roots.
        List<Path> potentialSourceRoots = new ArrayList<>();
        if (Files.isDirectory(effectiveSourceRoot.resolve("src/main/java"))) {
            potentialSourceRoots.add(effectiveSourceRoot.resolve("src/main/java"));
        }
        if (Files.isDirectory(effectiveSourceRoot.resolve("src/test/java"))) {
            potentialSourceRoots.add(effectiveSourceRoot.resolve("src/test/java"));
        }
        // If no sub-source roots found, use the provided sourceRootPath directly
        if (potentialSourceRoots.isEmpty()) {
            potentialSourceRoots.add(effectiveSourceRoot);
        }
        // Also consider the project root itself if it's not already covered
        if (!potentialSourceRoots.contains(Paths.get(this.projectRootPath))){
             potentialSourceRoots.add(Paths.get(this.projectRootPath));
        }

        for (Path root : potentialSourceRoots) {
            try (Stream<Path> pathStream = Files.walk(root)) {
                List<Path> foundPaths = pathStream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(classPath))
                        .collect(Collectors.toList());
                if (!foundPaths.isEmpty()) {
                    // Prefer paths that match more closely or handle multiple matches if necessary
                    return foundPaths.get(0).toString(); 
                }
            } catch (IOException e) {
                System.err.println("Error walking path: " + root + " : " + e.getMessage());
            }
        }
        System.err.println("Warning: Source file not found for class " + className + " under roots: " + potentialSourceRoots);
        return null;
    }

    private void extractLifecycleMethods(CompilationUnit testCu, AnalysisResult result, String originalFileSource) {
        // Get all type declarations in the compilation unit
        for (Object type : testCu.types()) {
            if (type instanceof TypeDeclaration) {
                TypeDeclaration typeDecl = (TypeDeclaration) type;
                
                // Get all methods in the type
                for (MethodDeclaration method : typeDecl.getMethods()) {
                    // Check for lifecycle annotations
                    for (Object modifier : method.modifiers()) {
                        if (modifier instanceof Annotation) {
                            Annotation annotation = (Annotation) modifier;
                            String annotationName = annotation.getTypeName().getFullyQualifiedName();
                            
                            String methodSourceCode = extractMethodSourceCode(method, originalFileSource);
                            
                            // Check for @Before annotations (JUnit 4 and 5)
                            if ("Before".equals(annotationName) || 
                                "org.junit.Before".equals(annotationName) || 
                                "org.junit.jupiter.api.BeforeEach".equals(annotationName) ||
                                "BeforeEach".equals(annotationName)) {
                                result.beforeMethods.add(methodSourceCode);
                                System.out.println("  Found @Before method: " + method.getName().getIdentifier());
                            }
                            
                            // Check for @BeforeAll annotations (JUnit 5)
                            else if ("BeforeAll".equals(annotationName) || 
                                     "org.junit.jupiter.api.BeforeAll".equals(annotationName) ||
                                     "BeforeClass".equals(annotationName) ||
                                     "org.junit.BeforeClass".equals(annotationName)) {
                                result.beforeAllMethods.add(methodSourceCode);
                                System.out.println("  Found @BeforeAll/@BeforeClass method: " + method.getName().getIdentifier());
                            }
                            
                            // Check for @After annotations (JUnit 4 and 5)
                            else if ("After".equals(annotationName) || 
                                     "org.junit.After".equals(annotationName) || 
                                     "org.junit.jupiter.api.AfterEach".equals(annotationName) ||
                                     "AfterEach".equals(annotationName)) {
                                result.afterMethods.add(methodSourceCode);
                                System.out.println("  Found @After method: " + method.getName().getIdentifier());
                            }
                            
                            // Check for @AfterAll annotations (JUnit 5)
                            else if ("AfterAll".equals(annotationName) || 
                                     "org.junit.jupiter.api.AfterAll".equals(annotationName) ||
                                     "AfterClass".equals(annotationName) ||
                                     "org.junit.AfterClass".equals(annotationName)) {
                                result.afterAllMethods.add(methodSourceCode);
                                System.out.println("  Found @AfterAll/@AfterClass method: " + method.getName().getIdentifier());
                            }
                        }
                    }
                }
            }
        }
    }
    
    private String extractMethodSourceCode(MethodDeclaration method, String originalFileSource) {
        int startPos = method.getStartPosition();
        int length = method.getLength();
        
        if (startPos >= 0 && length > 0 && (startPos + length) <= originalFileSource.length()) {
            return originalFileSource.substring(startPos, startPos + length);
        } else {
            System.err.println("Warning: Invalid start/length for lifecycle method source code extraction. Method: " + method.getName().getIdentifier());
            return "// Error extracting source code for " + method.getName().getIdentifier();
        }
    }
} 