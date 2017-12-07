// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDException;
import org.ampii.xd.security.AuthManager;
import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests for various error conditions mandated by the standard
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class ErrorTests {
    public static Test[] tests = {
            new Test("PUT illegal metadata") {
                public void execute() throws TestException {
                    boolean xml = alt.equals("xml");
                    serverData("<String name='foo' value='put to me' writable='true'/>");

                    // the standard says: ...none of the server-computed metadata, 'count', 'children', 'descendants',
                    // 'truncated', 'history', 'etag', 'next', 'self', 'edit', 'failures', or 'id', are present

                    step("reject count");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" count=\"123\"/>" : "{ \"$count\":123 }");
                    put();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't put computed metadata");

                    step("reject children");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" children=\"foo\"/>" : "{ \"$children\":\"foo\" }");
                    put();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't put computed metadata");

                    step("reject descendants");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" descendants=\"foo\"/>" : "{ \"$descendants\":\"foo\" }");
                    put();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't put computed metadata");

                    step("reject truncated");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" truncated=\"true\"/>" : "{ \"$truncated\":false }");
                    put();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't put computed metadata");

                    step("reject history");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\"><Extensions><List name=\"history\"/></Extensions></String>" : "{ \"$history\":{} }");
                    put();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't put computed metadata");

                    step("reject etag");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" etag=\"123\"/>" : "{ \"$etag\":\"foo\" }");
                    put();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't put computed metadata");

                    step("reject next");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" next=\"123\"/>" : "{ \"$next\":\"foo\" }");
                    put();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't put computed metadata");

                    step("reject self");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" self=\"123\"/>" : "{ \"$self\":\"foo\" }");
                    put();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't put computed metadata");

                    step("reject edit");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" edit=\"123\"/>" : "{ \"$edit\":\"foo\" }");
                    put();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't put computed metadata");

                    step("reject failures");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\"><Extensions><List name=\"failures\"/></Extensions></String>" : "{ \"$failures\":{} }");
                    put();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't put computed metadata");

                    step("reject id");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" id=\"123\"/>" : "{ \"$id\":\"foo\" }");
                    put();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't put computed metadata");
                }
            },
            new Test("POST illegal metadata") {
                public void execute() throws TestException {
                    boolean xml = alt.equals("xml");
                    serverData("<List name='foo' value='post to me' writable='true' memberType='String'/>");

                    // the standard says: ...none of the server-computed metadata, 'count', 'children', 'descendants',
                    // 'truncated', 'history', 'etag', 'next', 'self', 'edit', 'failures', or 'id', are present

                    step("reject count");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" count=\"123\"/>" : "{ \"$count\":123 }");
                    post();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't post computed metadata");

                    step("reject children");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" children=\"foo\"/>" : "{ \"$children\":\"foo\" }");
                    post();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't post computed metadata");

                    step("reject descendants");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" descendants=\"foo\"/>" : "{ \"$descendants\":\"foo\" }");
                    post();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't post computed metadata");

                    step("reject truncated");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" truncated=\"true\"/>" : "{ \"$truncated\":false }");
                    post();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't post computed metadata");
                    
                    step("reject failures");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\"><Extensions><List name=\"failures\"/></Extensions></String>" : "{ \"$failures\":{} }");
                    post();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't post computed metadata");

                    step("reject history");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\"><Extensions><List name=\"history\"/></Extensions></String>" : "{ \"$history\":{} }");
                    post();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't post computed metadata");

                    step("reject etag");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" etag=\"123\"/>" : "{ \"$etag\":\"foo\" }");
                    post();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't post computed metadata");

                    step("reject next");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" next=\"123\"/>" : "{ \"$next\":\"foo\" }");
                    post();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't post computed metadata");

                    step("reject self");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" self=\"123\"/>" : "{ \"$self\":\"foo\" }");
                    post();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't post computed metadata");

                    step("reject edit");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" edit=\"123\"/>" : "{ \"$edit\":\"foo\" }");
                    post();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't post computed metadata");

                    step("reject id");
                    requestText(xml ? "<?xml version=\"1.0\"?><String xmlns=\"http://bacnet.org/CSML/1.3\" id=\"123\"/>" : "{ \"$id\":\"foo\" }");
                    post();
                    expectErrorNumber(Errors.VALUE_FORMAT);
                    expectAmpiiErrorTextContains("Can't post computed metadata");

                }
            },
            new Test("PUT name matching") {
                public void execute() throws TestException {

                    step("matching (OK)");
                    serverData("<String name='foo' writable='true'/>");
                    clientData("<String name='foo' value='something'/>");
                    put();
                    expectSuccessCode();

                    step("non-matching (error)");
                    serverData("<String name='foo' writable='true'/>");
                    clientData("<String name='bar' value='something' />");
                    put();
                    expectErrorNumber(Errors.INCONSISTENT_VALUES);

                    step(".anonymous (OK)");
                    serverData("<String name='foo' writable='true'/>");
                    clientData("<String name='.anonymous' value='something'/>");
                    put();
                    expectSuccessCode();

                }
            }
    };
}

