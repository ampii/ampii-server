// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests for the Binding mechanism.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class BindingTests {

    public static Test[] tests = {
            new Test("Binding Skip Tests") {
                public void execute() throws TestException {

                    step("Auth with skip/max");
                    clientData("<Composition/>");
                    query("skip=2&max-results=1");
                    path("/.auth");
                    clientData("<Composition name='.auth' partial='true' type='0-BACnetWsAuth'/>");
                    get();
                    expectClientData("{ " +
                            "  '$base':'Composition', " +
                            "  '$type':'0-BACnetWsAuth'," +
                            "  '$partial':true," +
                            "  '$next':{'$..matchAny':true}," +
                            "  'ca-certs-pend':{'$base':'List'}" +
                            "}");

                }
            }
    };
}


