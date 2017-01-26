// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests the effects of the 'select' query parameter.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author drobin
 */
public class SelectTests {

    public static Test[] tests = {
            new Test("Select Query Parameter Tests") {
                public void execute() throws TestException {

                    definition("" +
                            "<Composition name='..Test-A'>" +
                            "    <String name='a' optional='true'/>" +
                            "    <String name='b' value='b'/>" +
                            "    <String name='c' value='c'/>" +
                            "</Composition>");
                    serverData(""+
                            "<Composition type='..Test-A' partial='true'>" +
                            "    <String name='a' value='a'/>" +
                            "</Composition>");

                    step("Select .required");
                    query("select=.required");
                    get();
                    expectResponseDataItemAbsent("a");
                    expectResponseDataItemPresent("b");
                    expectResponseDataItemPresent("c");

                    step("Select names");
                    query("select=a;b");
                    get();
                    expectResponseDataItemPresent("a");
                    expectResponseDataItemPresent("b");
                    expectResponseDataItemAbsent("c");

                    step("Select .optional");
                    query("select=.optional");
                    get();
                    expectResponseDataItemPresent("a");
                    expectResponseDataItemAbsent("b");
                    expectResponseDataItemAbsent("c");

                    step("Select mixture");
                    query("select=.optional;c");
                    get();
                    expectResponseDataItemPresent("a");
                    expectResponseDataItemAbsent ("b");
                    expectResponseDataItemPresent("c");


                }
            }
    };
}


