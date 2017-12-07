// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestEnvironment;
import org.ampii.xd.test.TestException;

/**
 * Tests presence of $type, $extends, and replacement of Anys.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class TypeTests  {

    public static Test[] tests = {
            new Test("Test when $type is/isn't returned") {
                public void execute() throws TestException {
                    definition("" +
                            "<Composition name='org.ampii.tests.X'>\n" +
                            "   <String      name='x1' value='this is x1'/>\n" + // no type
                            "   <Composition name='x2' type='org.ampii.tests.Y' />\n" + // explicit type
                            "   <List        name='x3' memberType='org.ampii.tests.Y' />\n" + // explicit memberType
                            "   <List        name='x4' />\n" +   // no memberType
                            "</Composition>\n");
                    definition("" +
                            "<Composition name='org.ampii.tests.Y'>\n" +
                            "   <String  name='y1' value='this is y1'/>\n" +
                            "</Composition>\n");
                    serverData("" +
                            "<Composition name='instance-of-X' type='org.ampii.tests.X'>\n" +
                            "   <String      name='x1' />\n" +
                            "   <Composition name='x2' >\n" +
                            "      <String  name='y1' value='noway'/>\n" +
                            "   </Composition>\n" +
                            "   <List        name='x3' >\n" +
                            "      <Composition name='1'>\n" +
                            "         <String  name='y1' />\n" +
                            "      </Composition>\n" +
                            "   </List>\n" +
                            "   <List        name='x4' >\n" +
                            "      <Composition name='1' type='org.ampii.tests.Y'>\n" +
                            "         <String  name='y1' />\n" +
                            "      </Composition>\n" +
                            "   </List>\n" +
                            "</Composition>\n");
                    step("metadata=all should only return $type where not implied");
                    query("metadata=all");
                    get();
                    expectResponseData();
                    expectResponseDataItemValue("$type", "org.ampii.tests.X");
                    expectResponseDataItemPresent("x1");
                    expectResponseDataItemAbsent("x1/$type");
                    expectResponseDataItemPresent("x2");
                    expectResponseDataItemAbsent("x2/$type");
                    expectResponseDataItemPresent("x2/y1");
                    expectResponseDataItemAbsent("x2/y1/$type");
                    expectResponseDataItemPresent("x3");
                    expectResponseDataItemAbsent("x3/$type");
                    expectResponseDataItemPresent("x3/1");
                    expectResponseDataItemAbsent("x3/1/$type");
                    expectResponseDataItemPresent("x4");
                    expectResponseDataItemAbsent("x4/$type");
                    expectResponseDataItemPresent("x4/1");
                    expectResponseDataItemValue("x4/1/$type","org.ampii.tests.Y");
                }
            },
            new Test("Test of presence of $type on Any children of Sequence/Object/Composition") {
                public void execute() throws TestException {
                    definition("" +
                            "<Composition name='org.ampii.tests.X'>\n" +
                            "   <String      name='x1' description='this is x1' type='org.ampii.tests.Y'/>\n" +
                            "   <Any         name='x2' description='this is x2' />\n" +
                            "</Composition>\n");
                    definition("" +
                            "<String name='org.ampii.tests.Y'/>\n");
                    serverData("" +
                            "<Composition name='instance-of-X' type='org.ampii.tests.X'>\n" +
                            "   <String  name='x1' />\n" +
                            "   <String  name='x2' type='org.ampii.tests.Y'/>\n" +  // this $type will turn into $extends when read at top level
                            "</Composition>\n");

                    step("metadata=all should return $type on the lower level Any");
                    query("metadata=all");
                    get();
                    expectResponseData();
                    expectResponseDataItemValue("$type", "org.ampii.tests.X");
                    expectResponseDataItemPresent("x1");
                    expectResponseDataItemAbsent("x1/$type");
                    expectResponseDataItemPresent("x2");
                    expectResponseDataItemValue("x2/$type", "org.ampii.tests.Y");

                    step("metadata=all should return effective type in $effectiveType and declared type in $type");
                    query("metadata=all");
                    pathAdd("/x2");
                    get();
                    expectResponseData();
                    expectResponseDataItemValue("$effectiveType", "org.ampii.tests.X/x2");   // this is the effective type
                    expectResponseDataItemValue("$type", "org.ampii.tests.Y");           // this is the declared type
                }
            },
            new Test("Test replacement of Any") {
                public void execute() throws TestException {
                    definition("" +
                            "<Composition name='org.ampii.tests.X'>\n" +
                            "   <String      name='x1' description='this is x1' type='org.ampii.tests.Y'/>\n" +
                            "   <Any         name='x2' description='this is x2' />\n" +
                            "</Composition>\n");
                    definition("" +
                            "<String name='org.ampii.tests.Y'/>\n");
                    serverData("" +
                            "<Composition name='instance-of-X' type='org.ampii.tests.X'>\n" +
                            "   <String  name='x1' value='x1'/>\n" +
                            "   <String  name='x2'value='x2'/>\n" +  // replace with string to start with
                            "</Composition>\n");
                    step("read the Any as a String");
                    clientData("<Composition name='instance-of-X' type='org.ampii.tests.X' partial='true'/>");
                    get();
                    expectClientData("" +
                            "<Composition name='instance-of-X' type='org.ampii.tests.X'>\n" +
                            "   <String  name='x1' value='x1'/>\n" +
                            "   <String  name='x2'value='x2'/>\n" +
                            "</Composition>\n");
                    step("replace the String with Unsigned");
                    clientData("" +
                            "<Composition name='instance-of-X'>\n" +
                            "   <String  name='x1' value='x1'/>\n" +
                            "   <Unsigned  name='x2' value='1234'/>\n" +
                            "</Composition>\n");
                    put();
                    expectSuccessCode();
                    step("and check replacement");
                    clientData("<Composition name='instance-of-X' type='org.ampii.tests.X' partial='true'/>");
                    get();
                    expectClientData("" +
                            "<Composition name='instance-of-X' type='org.ampii.tests.X'>\n" +
                            "   <String  name='x1' value='x1'/>\n" +
                            "   <Unsigned  name='x2' value='1234'/>\n" +
                            "</Composition>\n");

                }
            }


    };

}
