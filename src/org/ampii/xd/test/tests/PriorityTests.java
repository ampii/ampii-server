// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests the 'priority' query parameter and the actions of priority arrays.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author drobin
 */
public class PriorityTests {

    public static Test[] tests = {
            new Test("Priority Query Parameter Tests") {
                public void execute() throws TestException {

                    definition("" +
                            "<Composition name='..Test-A'>" +
                            "    <Unsigned name='with' relinquishDefault='0' commandable='true'/>" +
                            "    <Unsigned name='without' />" +
                            "</Composition>");
                    serverData("" +
                            "<Composition type='..Test-A'>" +
                            "   <Unsigned name='with'    value='160'/>" +
                            "   <Unsigned name='without' value='88'/>" +
                            "</Composition>");

                    step("check initial level 16");
                    pathAdd("/with");
                    clientData("<Unsigned type='..Test-A/with'/>");
                    get();
                    expectClientData("" +
                            "{ '$type':'..Test-A/with'," +
                            "  '$value':160," +
                            "  '$priorityArray':{" +
                            "     '1':{'null':{}}," +
                            "     '2':{'null':{}}," +
                            "     '3':{'null':{}}," +
                            "     '4':{'null':{}}," +
                            "     '5':{'null':{}}," +
                            "     '6':{'null':{}}," +
                            "     '7':{'null':{}}," +
                            "     '8':{'null':{}}," +
                            "     '9':{'null':{}}," +
                            "     '10':{'null':{}}," +
                            "     '11':{'null':{}}," +
                            "     '12':{'null':{}}," +
                            "     '13':{'null':{}}," +
                            "     '14':{'null':{}}," +
                            "     '15':{'null':{}}," +
                            "     '16':{'unsigned':160}" +
                            "  }" +
                            "}");

                    step("write at priority 12");
                    clientData("<Unsigned value='120'/>");
                    query("priority=12");
                    put();
                    clientData("<Unsigned type='..Test-A/with'/>");
                    get();
                    expectClientData("" +
                            "{ '$type':'..Test-A/with'," +
                            "  '$value':120," +
                            "  '$priorityArray':{" +
                            "     '1':{'null':{}}," +
                            "     '2':{'null':{}}," +
                            "     '3':{'null':{}}," +
                            "     '4':{'null':{}}," +
                            "     '5':{'null':{}}," +
                            "     '6':{'null':{}}," +
                            "     '7':{'null':{}}," +
                            "     '8':{'null':{}}," +
                            "     '9':{'null':{}}," +
                            "     '10':{'null':{}}," +
                            "     '11':{'null':{}}," +
                            "     '12':{'unsigned':120}," +
                            "     '13':{'null':{}}," +
                            "     '14':{'null':{}}," +
                            "     '15':{'null':{}}," +
                            "     '16':{'unsigned':160}" +
                            "  }" +
                            "}");

                    step("write at priority 4");
                    clientData("<Unsigned value='40'/>");
                    query("priority=4");
                    put();
                    clientData("<Unsigned type='..Test-A/with'/>");
                    get();
                    expectClientData("" +
                            "{ '$type':'..Test-A/with'," +
                            "  '$value':40," +
                            "  '$priorityArray':{" +
                            "     '1':{'null':{}}," +
                            "     '2':{'null':{}}," +
                            "     '3':{'null':{}}," +
                            "     '4':{'unsigned':40}," +
                            "     '5':{'null':{}}," +
                            "     '6':{'null':{}}," +
                            "     '7':{'null':{}}," +
                            "     '8':{'null':{}}," +
                            "     '9':{'null':{}}," +
                            "     '10':{'null':{}}," +
                            "     '11':{'null':{}}," +
                            "     '12':{'unsigned':120}," +
                            "     '13':{'null':{}}," +
                            "     '14':{'null':{}}," +
                            "     '15':{'null':{}}," +
                            "     '16':{'unsigned':160}" +
                            "  }" +
                            "}");

                    step("relinquish at priority 4");
                    clientData("<Null/>");
                    query("priority=4");
                    put();
                    clientData("<Unsigned type='..Test-A/with'/>");
                    get();
                    expectClientData("" +
                            "{ '$type':'..Test-A/with'," +
                            "  '$value':120," +
                            "  '$priorityArray':{" +
                            "     '1':{'null':{}}," +
                            "     '2':{'null':{}}," +
                            "     '3':{'null':{}}," +
                            "     '4':{'null':{}}," +
                            "     '5':{'null':{}}," +
                            "     '6':{'null':{}}," +
                            "     '7':{'null':{}}," +
                            "     '8':{'null':{}}," +
                            "     '9':{'null':{}}," +
                            "     '10':{'null':{}}," +
                            "     '11':{'null':{}}," +
                            "     '12':{'unsigned':120}," +
                            "     '13':{'null':{}}," +
                            "     '14':{'null':{}}," +
                            "     '15':{'null':{}}," +
                            "     '16':{'unsigned':160}" +
                            "  }" +
                            "}");

                    step("relinquish at priority 12");
                    clientData("<Null/>");
                    query("priority=12");
                    put();
                    clientData("<Unsigned type='..Test-A/with'/>");
                    get();
                    expectClientData("" +
                            "{ '$type':'..Test-A/with'," +
                            "  '$value':160," +
                            "  '$priorityArray':{" +
                            "     '1':{'null':{}}," +
                            "     '2':{'null':{}}," +
                            "     '3':{'null':{}}," +
                            "     '4':{'null':{}}," +
                            "     '5':{'null':{}}," +
                            "     '6':{'null':{}}," +
                            "     '7':{'null':{}}," +
                            "     '8':{'null':{}}," +
                            "     '9':{'null':{}}," +
                            "     '10':{'null':{}}," +
                            "     '11':{'null':{}}," +
                            "     '12':{'null':{}}," +
                            "     '13':{'null':{}}," +
                            "     '14':{'null':{}}," +
                            "     '15':{'null':{}}," +
                            "     '16':{'unsigned':160}" +
                            "  }" +
                            "}");

                    step("relinquish at priority 16");
                    clientData("<Null/>");
                    query("priority=16");
                    put();
                    clientData("<Unsigned type='..Test-A/with'/>");
                    get();
                    expectClientData("" +
                            "{ '$type':'..Test-A/with'," +
                            "  '$value':0," +
                            "  '$priorityArray':{" +
                            "     '1':{'null':{}}," +
                            "     '2':{'null':{}}," +
                            "     '3':{'null':{}}," +
                            "     '4':{'null':{}}," +
                            "     '5':{'null':{}}," +
                            "     '6':{'null':{}}," +
                            "     '7':{'null':{}}," +
                            "     '8':{'null':{}}," +
                            "     '9':{'null':{}}," +
                            "     '10':{'null':{}}," +
                            "     '11':{'null':{}}," +
                            "     '12':{'null':{}}," +
                            "     '13':{'null':{}}," +
                            "     '14':{'null':{}}," +
                            "     '15':{'null':{}}," +
                            "     '16':{'null':{}}" +
                            "  }" +
                            "}");


                }
            }
    };
}


