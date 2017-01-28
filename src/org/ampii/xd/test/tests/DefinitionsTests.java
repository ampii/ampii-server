// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestEnvironment;
import org.ampii.xd.test.TestException;

/**
 * Tests for making and using Definitions.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class DefinitionsTests  {

    public static Test[] tests = {
            new Test("Simple definition creation and usage") {
                public void execute() throws TestException {
                    definition(""+
                            "<Composition name='org.ampii.tests.X'>\n" +
                            "   <Integer     name='x1' minimum='0' maximum='100'/>\n" +
                            "   <Composition name='x2' type='org.ampii.tests.Y' />\n" +
                            "</Composition>\n");
                    definition(""+
                            "<Composition name='org.ampii.tests.Y'>\n" +
                            "   <String  name='y1' maximumLength='4' />\n" +
                            "</Composition>\n");
                    step("initial put of a correctly formed X containing Y");
                    serverData("<Collection name='foo' memberType='org.ampii.tests.X' />");
                    clientData(""+
                            "<Composition name='a-valid-X'>" +
                            "   <Integer     name='x1' value='50'/>" +
                            "   <Composition name='x2' >" +
                            "       <String  name='y1' maximumLength='4' />" +
                            "   </Composition>" +
                            "</Composition>");
                    post();
                    expectSuccessCode();
                }
            },
            new Test("Slightly more complicated definition and instance") {
                public void execute() throws TestException {
                    // much more is needed here...
                    // this is just a beginning, and it's not really an "inheritance test" yet since all the value and children are overwritten by InstanceA
                    definition("" +
                            "<Composition name = 'org.ampii.tests.A' >\n" +
                            "    <String name = 'a1' value = 'this is a1' />\n" +
                            "    <Real   name = 'a2' value = '2' />\n" +
                            "    <List   name = 'a3' memberType = 'org.ampii.tests.B' />\n" +
                            "    <List   name = 'a4' >\n" +
                            "        <MemberTypeDefinition >\n" +
                            "            <Composition >\n" +
                            "                <String name = 'a4m1' value = 'default-a' />\n" +
                            "                <String name = 'a4m2' value = 'default-b' />\n" +
                            "            </Composition >\n" +
                            "        </MemberTypeDefinition >\n" +
                            "    </List>\n" +
                            "    <Real   name = 'a5' value = '33' optional = 'true' />\n" +
                            "</Composition>");
                    definition("" +
                            "<Composition name = 'org.ampii.tests.B' extends='org.ampii.tests.A' >\n" +
                            "    <String      name = 'a1' value = 'new value for a1 from TypeB' />\n" +
                            "    <String      name = 'b1' value = 'this is b1' />\n" +
                            "    <Composition name = 'b2' type = 'org.ampii.tests.A' description = 'this is a type A inside type B' />\n" +
                            "    <Real        name = 'a5' value = '44' description = 'this makes a5 no longer optional' />\n" +
                            "</Composition>");
                    serverData("" +
                            "<Composition name='InstanceA' type='org.ampii.tests.A'>\n" +
                            "     <String name='a1' value='instance value for a1'/>\n" +
                            "     <Real name='a2' value='22'/>\n" +
                            "     <List name='a3'>\n" +
                            "         <Composition name='1' comment='InstanceA-a3-1 (has an explicit type=\"TypeB\")' >\n" +
                            "             <String name='a1' value='InstanceA-a3-1'/>\n" +
                            "             <Real name='a2' />\n" +
                            "            <List name='a3' />\n" +
                            "            <List name='a4' />\n" +
                            "            <String name='b1' value='InstanceA-b1-1'/>\n" +
                            "             <Composition name='b2' comment='InstanceA-a3-1 ( no \"type\")'>\n" +
                            "                 <String name='a1' value='InstanceA-a3-1-b2-a1'/>\n" +
                            "                 <Real name='a2' value='222'/>\n" +
                            "                 <List name='a3' />\n" +
                            "                 <List name='a4' >\n" +
                            "                     <Composition>\n" +
                            "                         <String name='a4m1' value='InstanceA-a3-1-b2-a4-1-a4m1'/>\n" +
                            "                         <String name='a4m2' value='InstanceA-a3-1-b2-a4-1-a4m2'/>\n" +
                            "                     </Composition>\n" +
                            "                     <Composition>\n" +
                            "                         <String name='a4m1' value='InstanceA-a3-1-b2-a4-2-a4m1'/>\n" +
                            "                         <String name='a4m2' value='InstanceA-a3-1-b2-a4-2-a4m2'/>\n" +
                            "                     </Composition>\n" +
                            "                 </List>\n" +
                            "                 <Real name='a5' value='3125' description='InstanceA-a3-1-b2-a5'/>\n" +
                            "             </Composition>\n" +
                            "             <Real name='a5' value='315' description='InstanceA-a3-1-a5'/>\n" +
                            "         </Composition>\n" +
                            "         <Composition name='2' comment='InstanceA-a3-2 (has no \"type\", implicitly TypeB)' partial='true'>\n" +
                            "             <String name='a1' value='InstanceA-a3-2-a1'/>\n" +
                            "             <String name='b1' value='InstanceA-b1-2-b1'/>\n" +
                            "             <Composition name='b2' comment='InstanceA-a3-2-b2 (has no \"type\", implicitly TypeA)'>\n" +
                            "                 <String name='a1' value='InstanceA-a3-2-b2-a1'/>\n" +
                            "                 <Real name='a2' value='2222'/>\n" +
                            "                 <List name='a3'/>\n" +
                            "                 <List name='a4'/>\n" +
                            "                 <Real name='a5' value='3225' description='InstanceA-a3-2-b2-a5'/>\n" +
                            "             </Composition>\n" +
                            "             <Real name='a5' value='325' description='InstanceA-a3-2-a5'/>\n" +
                            "         </Composition>\n" +
                            "     </List>\n" +
                            "     <List name='a4'/>\n" +
                            "     <Real name='a5' value='5' description='InstanceA-a5'/>\n" +
                            "</Composition>");
                    clientData("<Composition/>");
                    query("metadata=all");
                    get();
                    expectClientData(""+
                            "<Composition name='client-result' type='org.ampii.tests.A' writable='true'>\n" +
                            "    <String name='a1' value='instance value for a1'/>\n" +
                            "    <Real name='a2' value='22'/>\n" +
                            "    <List name='a3'>\n" +
                            "        <Composition name='1' comment='InstanceA-a3-1 (has an explicit type=\"TypeB\")'>\n" +
                            "            <String name='a1' value='InstanceA-a3-1'/>\n" +
                            "            <Real name='a2' value='2'/>\n" +
                            "            <List name='a3'/>\n" +
                            "            <List name='a4'/>\n" +
                            "            <String name='b1' value='InstanceA-b1-1'/>\n" +
                            "            <Composition name='b2' comment='InstanceA-a3-1 ( no \"type\")'>\n" +
                            "                <String name='a1' value='InstanceA-a3-1-b2-a1'/>\n" +
                            "                <Real name='a2' value='222'/>\n" +
                            "                <List name='a3' />\n" +
                            "                <List name='a4' >\n" +
                            "                    <Composition>\n" +
                            "                        <String name='a4m1' value='InstanceA-a3-1-b2-a4-1-a4m1'/>\n" +
                            "                        <String name='a4m2' value='InstanceA-a3-1-b2-a4-1-a4m2'/>\n" +
                            "                    </Composition>\n" +
                            "                    <Composition>\n" +
                            "                        <String name='a4m1' value='InstanceA-a3-1-b2-a4-2-a4m1'/>\n" +
                            "                        <String name='a4m2' value='InstanceA-a3-1-b2-a4-2-a4m2'/>\n" +
                            "                    </Composition>\n" +
                            "                </List>\n" +
                            "                <Real name='a5' value='3125' description='InstanceA-a3-1-b2-a5'/>\n" +
                            "            </Composition>\n" +
                            "            <Real name='a5' value='315' description='InstanceA-a3-1-a5'/>\n" +
                            "        </Composition>\n" +
                            "        <Composition name='2' comment='InstanceA-a3-2 (has no \"type\", implicitly TypeB)' >\n" +
                            "            <String name='a1' value='InstanceA-a3-2-a1'/>\n" +
                            "            <Real name='a2' value='2.0'/>\n" +
                            "            <List name='a3'/>\n" +
                            "            <List name='a4'/>\n" +
                            "            <String name='b1' value='InstanceA-b1-2-b1'/>\n" +
                            "            <Composition name='b2' comment='InstanceA-a3-2-b2 (has no \"type\", implicitly TypeA)'>\n" +
                            "                <String name='a1' value='InstanceA-a3-2-b2-a1'/>\n" +
                            "                <Real name='a2' value='2222'/>\n" +
                            "                <List name='a3'/>\n" +
                            "                <List name='a4'/>\n" +
                            "                <Real name='a5' value='3225' description='InstanceA-a3-2-b2-a5'/>\n" +
                            "            </Composition>\n" +
                            "            <Real name='a5' value='325' description='InstanceA-a3-2-a5'/>\n" +
                            "        </Composition>\n" +
                            "    </List>\n" +
                            "    <List name='a4'/>\n" +
                            "    <Real name='a5' value='5' description='InstanceA-a5'/>\n" +
                            "</Composition>");
                }
            }
    };

}
