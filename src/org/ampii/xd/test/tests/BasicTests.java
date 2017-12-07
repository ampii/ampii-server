// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Collection of small basic tests.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class BasicTests {

    public static Test[] tests = {
            new Test("Simple Read and Write value using Plain Text") {
                public void execute() throws TestException {
                    serverData("<String name='foo' value='change me' displayName='not important' writable='true'/>");
                    alt("plain");
                    step("GET initial $value");
                    clientData("<String/>");
                    get();
                    expectClientData("<String value='change me'/>");
                    step("PUT value");
                    clientData("<String value='changed'/>");
                    put();
                    expectSuccessCode();
                    step("Confirm PUT");
                    clientData("<String/>");
                    get();
                    expectClientData("<String value='changed'/>");
                }
            },
            new Test("Indirect Read and Write $displayName using JSON/XML") {
                public void execute() throws TestException {
                    serverData("<Real name='foo' displayName='change me' writable='true'/>");
                    step("GET initial value of $displayName");
                    clientData("<Real/>");
                    query("metadata=cat-ui");
                    get();
                    expectClientData("<Real displayName='change me'/>");
                    step("PUT new value for $displayName and check that server data changed");
                    clientData("<Real displayName='changed'/>");
                    clientDataPars("metadata=cat-ui");
                    put();
                    expectServerData("<Real displayName='changed' writable='true'/>");
                    step("confirm that change is also available with a GET");
                    clientData("<Real/>");
                    query("metadata=cat-ui");
                    get();
                    expectClientData("<Real displayName='changed'/>");
                }
            },
            new Test("Metadata filter tests") {
                public void execute() throws TestException {
                    serverData("<Real value='75.5' displayName='find me' maximum='100.0'/>");
                    step("GET with default metadata");
                    clientData("<Real/>");
                    get();
                    expectClientData("<Real value='75.5'/>");
                    step("GET with 'all'");
                    query("metadata=all");
                    clientData("<Real/>");
                    get();
                    expectClientData("<Real value='75.5' displayName='find me' maximum='100.0' writable='true'/>");
                    step("GET with 'cat-data'");
                    query("metadata=cat-data");
                    clientData("<Real/>");
                    get();
                    expectClientData("<Real maximum='100.0' writable='true'/>");
                    step("GET with 'cat-data,value'");
                    query("metadata=cat-data,value");
                    clientData("<Real/>");
                    get();
                    expectClientData("<Real maximum='100.0' writable='true' value='75.5'/>");
                }
            },
            new Test("Fabricated type test") {
                public void execute() throws TestException {
                    step("get top level type name direct");
                    path("/.auth/$type");
                    clientData("<String/>");
                    get();
                    expectClientData("<String value='0-BACnetWsAuth'/>");
                    step("get top level type name indirect");
                    path("/.auth");
                    query("metadata=cat-types");
                    clientData("<Composition/>");
                    get();
                    expectResponseDataItemValue("$type", "0-BACnetWsAuth");
                    step("get fabricated lower level type name direct");
                    query("");
                    path("/.auth/int/enable/$type");
                    clientData("<String/>");
                    get();
                    expectClientData("<String value='0-BACnetWsAuth/int/enable'/>");
                }
            },
            new Test("$truncated test") {
                public void execute() throws TestException {
                    step("ask for a top level truncated structure");
                    path("/.auth");
                    query("depth=0");
                    get();
                    expectResponseDataItemValue("$truncated", "true");
                    step("ask for a second level truncated structure");
                    path("/.auth");
                    query("depth=1");
                    get();
                    expectResponseDataItemValue("int/$truncated", "true");
                }
            },
            new Test("Collection POST and DELETE test") {
                public void execute() throws TestException {
                    serverData("<Collection memberType='String' writable='true'/>");
                    step("post three new members");
                    clientData("<String name='post-me' displayName='Post Me!'/>");      // post a string
                    clientDataPars("metadata=cat-ui,cat-types");
                    post();
                    clientData("<String name='post-me' displayName='Post Another!'/>"); // deliberate duplicate name
                    post();
                    clientData("<String name='different' displayName='And Another!'/>");
                    post();
                    step("read the resulting three members");
                    clientData("<Collection memberType='String'/>");
                    query("metadata=displayName");
                    get();
                    expectClientData(
                            "<Collection memberType='String'>" +
                            "<String name='post-me'   displayName='Post Me!'     />" +
                            "<String name='post-me_1' displayName='Post Another!'/>" +
                            "<String name='different' displayName='And Another!'/>" +
                            "</Collection>");

                    step("delete a member in the middle");
                    pathAdd("/post-me_1");
                    delete();
                    expectSuccessCode();
                    pathRemove("/post-me_1");
                    step("check that the middle member has been deleted");
                    clientData("<Collection memberType='String'/>");
                    get();
                    expectClientData(
                            "<Collection memberType='String'>" +
                                    "<String name='post-me'   displayName='Post Me!'     />" +
                                    "<String name='different' displayName='And Another!' />" +
                                    "</Collection>");
                }
            },
            new Test("List POST and DELETE test") {
                public void execute() throws TestException {
                    serverData("<List memberType='String' writable='true'/>");
                    step("post three new members");
                    clientData("<String name='post-me' displayName='Post Me!'/>");      // post a string
                    clientDataPars("metadata=cat-ui,cat-types");
                    post();
                    clientData("<String name='post-me' displayName='Post Another!'/>"); // deliberate duplicate name
                    post();
                    clientData("<String name='different' displayName='And Another!'/>");
                    post();
                    step("read the resulting three members");   // all of the above names should be ignored!!!
                    clientData("<List memberType='String'/>");
                    query("metadata=displayName");
                    get();
                    expectClientData(
                            "<List memberType='String'>" +
                                    "<String displayName='Post Me!'     />" +
                                    "<String displayName='Post Another!'/>" +
                                    "<String displayName='And Another!' />" +
                                    "</List>");
                    expectResponseDataItemPresent("1");
                    expectResponseDataItemPresent("2");
                    expectResponseDataItemPresent("3");
                    step("delete a member in the middle");
                    pathAdd("/2");
                    delete();
                    expectSuccessCode();
                    pathRemove("/2");
                    step("check that the middle member has been deleted");
                    clientData("<List memberType='String'/>");
                    get();
                    expectClientData(
                            "<List memberType='String'>" +
                                    "<String displayName='Post Me!'     />" +
                                    "<String displayName='And Another!' />" +
                                    "</List>");
                    expectResponseDataItemPresent("1");
                    expectResponseDataItemPresent("2");
                    expectResponseDataItemAbsent("3");
                }
            }
    };
}

