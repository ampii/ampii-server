// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests for the {@code<Includes>/$$includes} mechanism.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author drobin
 */
public class IncludeTests { // add this class to the config file indicated by Application.testDefinitionFile if you want these to run

    public static Test[] tests = {
       new Test("Test of remote file inclusion") {
           public void execute() throws TestException {
               serverFile("include-test-outer.xml",
                       "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                       "<CSML xmlns=\"http://bacnet.org/csml/1.2\" defaultLocale=\"en-US\">\n" +
                       "   <Includes>\n" +
                       "       <Link value=\""+ getServerBaseHttpURI()+ getServerTestFilePath()+"/include-test-inner.json\"/>\n" +
                       "   </Includes>\n" +
                       "</CSML>");
               serverFile("include-test-inner.xml",
                       "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                       "<CSML xmlns=\"http://bacnet.org/csml/1.2\" defaultLocale=\"en-US\">\n" +
                       "   <String name=\"include-test-inner-xml\" value=\"from xml\"/>\n" +
                       "</CSML>");
               serverFile("include-test-outer.json",
                       "{\n" +
                       "  \"$name\":\".csml\",\n" +
                       "  \"$base\":\"Collection\",\n" +
                       "  \"$includes\":{\n" +
                       "    \"1\":\""+ getServerBaseHttpURI()+ getServerTestFilePath()+"/include-test-inner.xml\"\n" +
                       "  }\n" +
                       "}");
               serverFile("include-test-inner.json",
                       "{\n" +
                       "  \"$name\":\".csml\",\n" +
                       "  \"$base\":\"Collection\",\n" +
                       "  \"include-test-inner-json\":{\"$base\":\"String\",\"$value\":\"from json\"}\n" +
                       "}");
               serverData(
                   "<Collection>\n" +
                   "   <Includes>\n" +
                   "      <Link value='"+ getServerBaseHttpURI()+ getServerTestFilePath()+"/include-test-inner.xml'/>\n" +
                   "      <Link value='"+ getServerBaseHttpURI()+ getServerTestFilePath()+"/include-test-inner.json'/>\n" +
                   "   </Includes>\n" +
                   "</Collection>\n"
               );
               expectServerDataItemValue("include-test-inner-xml", "from xml");
               expectServerDataItemValue("include-test-inner-json","from json");
           }
       }
    };
}

