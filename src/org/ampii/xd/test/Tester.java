// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Log;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.database.DataStore;
import org.ampii.xd.marshallers.DataParser;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The master Test runner - selects and runs {@link Test} instances.
 * <p>
 * Runs all the tests in the List "test-list" in the xml or json file specified by Application.testDefinitionFile, using
 * the "test-pars" found in that same file.
 *
 * @author daverobin
 */
public class Tester {


    public static void doTests(String format) {
        try {
            Log.logConsole("Checking database consistency.");
            Log.logConsole(DataStore.checkConsistency() + " items checked.");
            Log.logConsole("Initializing tests (default format=" + format + ").");
            Data testStuff = DataParser.parse(new File(Application.testDefinitionFile));
            Data testPars  = testStuff.get("test-pars");
            TestEnvironment env = new TestEnvironment(format, testPars, (test)->step(test));
            //
            // run all the tests in the classes specified in the List "test-list" in the xml or json file specified by Application.testDefinitionFile
            //
            for (Data testClass : testStuff.get("test-list").getChildren()) {
                boolean failed = runOneClass(testClass.stringValue(),env);
                if (failed && Application.testStopOnFailure) break;
            }
            if (failures.size() == 0) {
                Log.logConsole("Checking database consistency.");
                Log.logConsole(DataStore.checkConsistency() + " items checked.");
                Log.logConsole("\nALL TESTS PASSED\n");
            }
            else {
                Log.logConsole("\nTESTS FAILED\n");
                for (String failure : failures) { Log.logConsole(failure); }
                Log.logConsole("\n----------------------------------");
            }
        } catch (XDException e) { Log.logConsole("Tester failed: " + e.getLocalizedMessage()); }
    }

    private static boolean runOneClass(String className, TestEnvironment env) throws XDException{
        try {
            Class testClass = Application.class.getClassLoader().loadClass(className);
            Field testsField = testClass.getField("tests");   // get the 'Tests[] tests' field
            Test[] tests = (Test[])testsField.get(null);
            boolean failed = false;
            for (Test test : tests) {
                failed = runOneTest(test,env);
                if (failed && Application.testStopOnFailure) break;
            }
            return failed;
        }
        catch (ClassNotFoundException e) { logTesterFailure("Could not find test class " + className); }
        catch (NoSuchFieldException e)   { logTesterFailure("Test class " + className + " does not have 'Test[] tests={}' field");   }
        catch (IllegalAccessException e) { logTesterFailure("Test class " + className + " does not allow access to 'Test[] tests={}' field");   }
        return true;
    }

    private static boolean runOneTest(Test test, TestEnvironment env) throws XDException {
        boolean failed = false;
        start(test, env);
        try { test.execute(); } // execute() succeeds quietly or throws TestException on failure
        catch (TestException|XDException e) { failed = true;  logTestFailure(test, e.getLocalizedMessage()); }
        test.close();  // this will remove any temporary definitions created by the test
        end(test);
        return failed;
    }

    private static void start(Test test, TestEnvironment env) {
        test.initialize(env);
        logTestStart(test);
    }

    private static void step(Test test) {   // called back (via environment's step()) from running Tests to log progress
        logTestStep(test);
    }

    private static void end(Test test) {
        logTestEnd(test);
    };

    private static void logTestStart(Test test) {
        Log.logHttpInfo("--------------- TEST "+test.testNumber + ": " + test.testDescription);
        Log.logConsole(test.testNumber + ": " + test.testDescription);  // if not skipping, print the test description
    }

    private static void logTestStep(Test test) {
        Log.logHttpInfo("--------------- TEST " + test.testNumber + "." + test.stepNumber + ": " + test.stepDescription);
        Log.logConsole("  " + test.testNumber + "." + test.stepNumber + ": " + test.stepDescription);  // if not skipping, print the test step description
    }

    private static void logTestEnd(Test test) {
    }

    private static List<String> failures = new ArrayList<>();

    private static void logTestFailure(Test test, String reason) {
        String failureText =
                "---------- Test Failure ----------\n" +
                "Test "+test.testNumber+": " + test.testDescription + "\n" +
                "Step "+test.stepNumber+": " + test.stepDescription + "\n" +
                "Reason: " + reason;
        Log.logInfo(failureText);
        Log.logHttpInfo(failureText);
        failures.add(failureText);  // no output to console yet
    }


    private static void logTesterFailure(String reason) {
        // this is bad... this indicates that the tests are badly constructed, not that they "fail"
        String failureText =
                "---------- Tester Failure ----------\n" +
                "Reason: " + reason;
        Log.logInfo(failureText);
        Log.logConsole(failureText); // complain loudly
    }


}
