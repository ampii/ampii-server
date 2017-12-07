// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.common.Errors;
import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests for operations in plain text.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class PlainTests {

    public static Test[] tests = {
            new Test("Post Plain Text") {
                public void execute() throws TestException {

                    step("POST to primitive (error)");
                    serverData("<String value='change me' writable='true'/>");
                    alt("plain");
                    requestText("reject me");
                    post();
                    expectFailureCode();
                    expectErrorNumber(Errors.CANNOT_CREATE);

                    step("POST to collection of ANY (error - don't know type)");
                    serverData("<Collection writable='true'/>");
                    alt("plain");
                    requestText("42");  // deliberately ambiguous type
                    post();
                    expectFailureCode();
                    expectErrorNumbers(Errors.CANNOT_CREATE,Errors.VALUE_FORMAT);

                    step("POST to List of Unsigned");
                    serverData("<List memberType='Unsigned' writable='true'/>");
                    alt("plain");
                    requestText("42");
                    post();
                    expectSuccessCode();
                    step("... and read it back in XML to confirm");
                    alt("xml");
                    pathAdd("/1");
                    get();
                    expectClientData("<Unsigned name='1' value='42'/>");

                    step("POST to List of Unsigned with bad value (error)");
                    serverData("<List memberType='Unsigned' writable='true' />");
                    alt("plain");
                    requestText("reject me");
                    post();
                    expectFailureCode();
                    expectErrorNumber(Errors.VALUE_FORMAT);

                }
            },
            new Test("PUT/GET Plain Text") {
                public void execute() throws TestException {

                    step("PUT plain text to primitive");
                    serverData("<String value='change me' writable='true'/>");
                    alt("plain");
                    requestText("hello, server 1");
                    put();
                    expectSuccessCode();

                    step("GET plain text");
                    alt("plain");
                    get();
                    expectSuccessCode();
                    expectResponseText("hello, server 1");

                }
            }

    };
}

