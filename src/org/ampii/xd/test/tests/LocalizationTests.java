// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests localizable strings.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class LocalizationTests {

    public static Test[] tests = {
            new Test("Localized Value Tests") {
                public void execute() throws TestException {
                    serverData(
                            "<String name='foo' value='replace me' writable='true'>" +
                                    "    <Value locale='de-DE'>ersetzen mich</Value>" +
                                    "    <Value locale='fr-FR'>remplacez moi</Value>" +
                                    "</String>");

                    step("GET initial $value in default locale");
                    alt("plain");
                    clientData("<String/>");
                    get();
                    expectClientData("<String value='replace me'/>");

                    step("GET initial $value in alternate locale");
                    clientData("<String/>");
                    query("locale=de-DE");
                    get();
                    expectClientData("<String value='ersetzen mich'/>");

                    step("GET all values in all locales - should ignore locale query par");
                    alt("default"); // switch back to default xml or json
                    query("locale=de-DE"); // this should be ignored for json or xml, so we'll test that we still get all locales
                    clientData("<String/>");
                    get();
                    expectClientData(
                            "<String name='foo' value='replace me'>" +
                                    "    <Value locale='de-DE'>ersetzen mich</Value>" +
                                    "    <Value locale='fr-FR'>remplacez moi</Value>" +
                                    "</String>");

                    step("PUT value in alternate locale and confirm");
                    alt("plain");
                    query("locale=de-DE");
                    clientData("<String value='ersetzt'/>");
                    put();
                    clientData("<String/>");
                    get();
                    expectClientData("<String value='ersetzt'/>");


                    step("GET all values in all locales - check that only one has changed changed");
                    alt("default"); // switch back to default xml or json
                    query("locale=fr_FR"); // again, be evil and add a bogus locale that needs to be ignored
                    clientData("<String/>");
                    get();
                    expectClientData(
                            "<String name='foo' value='replace me'>" +
                                    "    <Value locale='de-DE'>ersetzt</Value>" +
                                    "    <Value locale='fr-FR'>remplacez moi</Value>" +
                                    "</String>");

                    step("PUT value without local and confirm others are removed");
                    alt("plain");
                    query("");
                    clientData("<String value='replaced'/>");
                    put();

                    step("GET all values in all locales - check that only one still exists");
                    alt("default"); // switch back to default xml or json
                    clientData("<String/>");
                    get();
                    expectClientData("<String name='foo' value='replaced'></String>");


                }
            },
            new Test("Localized Metadata Tests") {
                public void execute() throws TestException {
                    serverData(
                            "<Real name='foo' displayName='replace me' writable='true'>" +
                                    "    <DisplayName locale='de-DE'>ersetzen mich</DisplayName>" +
                                    "    <DisplayName locale='fr-FR'>remplacez moi</DisplayName>" +
                                    "</Real>");

                    step("GET initial $displayName in default locale");
                    alt("plain");
                    clientData("<String/>");
                    pathAdd("/$displayName");
                    get();
                    expectClientData("<String value='replace me'/>");

                    step("GET initial $displayName in alternate locale");
                    clientData("<String/>");
                    query("locale=de-DE");
                    get();
                    expectClientData("<String value='ersetzen mich'/>");
                    pathRemove("/$displayName");

                    step("GET all $displayName values in all locales - should ignore locale query par");
                    alt("default"); // switch back to default xml or json
                    query("metadata=cat-ui&locale=de-DE"); // this local should be ignored for json or xml, so we'll test that we still get all locales
                    clientData("<Real/>");
                    get();
                    expectClientData(
                            "<Real name='foo' displayName='replace me'>" +
                                    "    <DisplayName locale='de-DE'>ersetzen mich</DisplayName>" +
                                    "    <DisplayName locale='fr-FR'>remplacez moi</DisplayName>" +
                                    "</Real>");

                    step("PUT displayName in alternate locale and confirm");
                    query("locale=de-DE");
                    pathAdd("/$displayName");
                    clientData("<String value='ersetzt'/>");
                    put();
                    clientData("<String/>");
                    get();
                    expectClientData("<String value='ersetzt'/>");

                    step("PUT displayName without default locale and confirm that other locales are removed");
                    alt("plain");
                    query("");
                    clientData("<String value='replaced'/>");
                    put();
                    clientData("<String/>");
                    get();
                    expectClientData("<String value='replaced'/>");

                    step("GET all values in all locales - check that only one remains");
                    alt("default"); // switch back to default xml or json
                    pathRemove("/$displayName");
                    query("metadata=cat-ui&locale=fr_FR"); // again, add a bogus local that needs to be ignored
                    clientData("<Real/>");
                    get();
                    expectClientData(
                            "<Real name='foo' displayName='replaced'/>");
                }
            }
    };

}
