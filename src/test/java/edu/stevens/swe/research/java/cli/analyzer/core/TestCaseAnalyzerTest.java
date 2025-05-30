package edu.stevens.swe.research.java.cli.analyzer.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class TestCaseAnalyzerTest {

    @BeforeAll
    static void setUpBeforeClass() {
        System.out.println("Setting up before all tests");
    }
    
    @BeforeEach
    void setUp() {
        System.out.println("Setting up before each test");
    }
    
    @AfterEach
    void tearDown() {
        System.out.println("Cleaning up after each test");
    }
    
    @AfterAll
    static void tearDownAfterClass() {
        System.out.println("Cleaning up after all tests");
    }
    
    @Test
    void testLifecycleMethodDetection() {
        // This test method contains lifecycle annotations in the class
        System.out.println("Running test method");
    }
    
    @Test
    void testAnotherMethod() {
        // Another test method to verify multiple tests work
        System.out.println("Running another test method");
    }
} 