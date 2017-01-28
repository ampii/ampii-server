// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.data.Meta;
import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests the effects of the 'select' query parameter.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class SkipTests {

    public static Test[] tests = {
            new Test("Collection Skip Tests") {
                public void execute() throws TestException {

                    step("Collection skip with no max");
                    serverData("" +
                            "<Collection name='blah'>" +
                            "    <String name='a' value='a'/>" +
                            "    <String name='b' value='b'/>" +
                            "    <String name='c' value='c'/>" +
                            "    <String name='d' value='d'/>" +
                            "    <String name='e' value='e'/>" +
                            "</Collection>");
                    clientData("<Collection/>");
                    query("skip=2");
                    get();
                    expectClientData("" +
                            "<Collection name='blah' partial='true'>" +
                            "    <String name='c' value='c'/>" +
                            "    <String name='d' value='d'/>" +
                            "    <String name='e' value='e'/>" +
                            "</Collection>");
                    step("Collection skip with max, expecting $next");
                    clientData("<Collection/>");
                    query("skip=2&max-results=2");
                    get();
                    expectClientData("" +
                            "{ '$base':'Collection', " +
                            "  '$partial':true," +
                            "  '$next':{'$..matchAny':true}," +
                            "  'c':{'$base':'String','$value':'c'}," +
                            "  'd':{'$base':'String','$value':'d'}" +
                            "}");
                    step("Collection skip with max, using $next");
                    uri(getClientData().stringValueOf(Meta.NEXT, "<none>"));
                    clientData("<Collection/>");
                    get();
                    expectClientData("" +
                            "{ '$base':'Collection', " +
                            "  '$partial':true," +
                            "  'e':{'$base':'String','$value':'e'}" +
                            "}");
                }
            },
            new Test("String Skip Tests") {
                public void execute() throws TestException {

                    step("String skip with no max");
                    serverData("<String value='0123456789'/>");
                    query("skip=2");
                    alt("plain");
                    get();
                    expectResponseText("23456789");

                    step("String skip with max");
                    serverData("<String value='0123456789'/>");
                    query("skip=2&max-results=4");
                    get();
                    expectResponseText("2345");

                    step("String append");
                    serverData("<String value='beginning'/>");
                    requestText("-end");
                    query("skip=-1");
                    put();
                    expectSuccessCode();
                    query("");
                    get();
                    expectResponseText("beginning-end");

                    step("String overlay within original length");
                    serverData("<String value='beginning'/>");
                    requestText("MIDDLE");
                    query("skip=2");
                    put();
                    expectSuccessCode();
                    query("");
                    get();
                    expectResponseText("beMIDDLEg");

                    step("String overlay beyond original length");
                    serverData("<String value='beginning'/>");
                    requestText("MIDDLEandEND");
                    query("skip=2");
                    put();
                    expectSuccessCode();
                    query("");
                    get();
                    expectResponseText("beMIDDLEandEND");

                }
            },
            new Test("Localized String Skip Tests") {
                public void execute() throws TestException {

                    step("Get localized String skip/max, default locale");
                    serverData("{'$base':'String', '$value':'hello','$value$$fr':'bonjour'}");
                    query("skip=2");
                    alt("plain");
                    get();
                    expectResponseText("llo");

                    step("Get localized String skip/max, alternate locale");
                    query("skip=2&locale=fr");
                    get();
                    expectResponseText("njour");

                    step("Append localized String, with skip=-1, alternate locale");
                    query("skip=-1&locale=fr");
                    requestText(", ami");
                    put();
                    expectSuccessCode();
                    alt("default");
                    clientData("<String/>");
                    get();
                    expectClientData("{'$base':'String', '$value':'hello','$value$$fr':'bonjour, ami'}");

                    step("Append localized String, with big skip, alternate locale");
                    query("skip=1000&locale=fr");
                    requestText("!");
                    alt("plain");
                    put();
                    expectSuccessCode();
                    alt("default");
                    clientData("<String/>");
                    get();
                    expectClientData("{'$base':'String', '$value':'hello','$value$$fr':'bonjour, ami!'}");

                    step("Merge localized String, alternate locale");
                    query("skip=-1&alt=plain&locale=fr");
                    requestText("!");
                    query("skip=7&locale=fr");
                    alt("plain");
                    put();
                    expectSuccessCode();
                    query("");
                    alt("default");
                    clientData("<String/>");
                    get();
                    expectClientData("{'$base':'String', '$value':'hello','$value$$fr':'bonjour! ami!'}");

                    step("Merge localized String, default locale");
                    query("skip=1");
                    requestText("i there!");
                    alt("plain");
                    put();
                    expectSuccessCode();
                    query("");
                    alt("default");
                    clientData("<String/>");
                    get();
                    expectClientData("{'$base':'String', '$value':'hi there!'}");  // check that other locales removed!

                }
            },
            new Test("OctetString Skip Tests") {
                public void execute() throws TestException {

                    step("OctetString skip with no max");
                    serverData("<OctetString value='00010203040506070809'/>");
                    clientData("<OctetString/>");
                    query("skip=2");
                    alt("plain");
                    get();
                    expectResponseText("0203040506070809");

                    step("OctetString skip with max");
                    serverData("<OctetString value='00010203040506070809'/>");
                    clientData("<OctetString/>");
                    query("skip=2&max-results=4");
                    get();
                    expectResponseText("02030405");

                    step("OctetString append");
                    serverData("<OctetString value='00010203040506070809'/>");
                    requestText("CAFEBABE");
                    query("skip=-1");
                    put();
                    expectSuccessCode();
                    query("");
                    get();
                    expectResponseText("00010203040506070809CAFEBABE");

                    step("OctetString overlay within original length");
                    serverData("<OctetString value='00010203040506070809'/>");
                    requestText("CAFEBABE");
                    query("skip=4&alt=plain");
                    put();
                    expectSuccessCode();
                    query("");
                    get();
                    expectResponseText("00010203CAFEBABE0809");

                    step("OctetString overlay beyond original length");
                    serverData("<OctetString value='00010203040506070809'/>");
                    requestText("CAFEBABEDEADBEEF");
                    query("skip=4&alt=plain");
                    put();
                    expectSuccessCode();
                    query("");
                    get();
                    expectResponseText("00010203CAFEBABEDEADBEEF");

                }
            }
    };
}


