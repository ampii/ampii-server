// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.basetypes.*;
import org.ampii.xd.database.Session;
import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests the default bindings in IndexManager class (which will likely be replaced by any large application, but hey, we'll test the default anyway).
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author drobin
 */
public class IndexerTests {

    public static Test[] tests = {
            new Test("Indexer Tests") {
                public void execute() throws TestException {

                    step("Setup /.data/events and /.data/objects");
                    try {  // indexer looks for objects under "/.bacnet"
                        Session.atomicPost("Indexer Tests", ".../.bacnet",
                                parseServerInstance("" +
                                        "<Collection name='..scope-1'>" +
                                        "    <Collection name='device-1-1'>" +
                                        "        <Object name='device-1-1-1' type='0-DeviceObject'   partial='true'/>" +
                                        "        <Object name='event-1-1-1'  type='0-EventLogObject' partial='true'/>" +
                                        "        <Object name='event-1-1-2'  type='0-EventLogObject' partial='true'/>" +
                                        "    </Collection>" +
                                        "    <Collection name='device-1-2'>" +
                                        "        <Object name='device-1-2-1' type='0-DeviceObject'   partial='true'/>" +
                                        "        <Object name='event-1-2-1'  type='0-EventLogObject' partial='true'/>" +
                                        "    </Collection>" +
                                        "</Collection>"));
                        Session.atomicPost("Indexer Tests", ".../.bacnet",
                                parseServerInstance("" +
                                        "<Collection name='..scope-2'>" +
                                        "    <Collection name='device-2-1'>" +
                                        "        <Object name='device-2-1-1' type='0-DeviceObject'   partial='true'/>" +
                                        "        <Object name='event-2-1-1'  type='0-EventLogObject' partial='true'/>" +
                                        "    </Collection>" +
                                        "</Collection>"));
                    } catch (XDException e) { fail(e.getLocalizedMessage()); }
                    step("Check /.data/events");
                    clientData("<List type='0-BACnetWsData/events'/>");
                    path("/.data/events");
                    get();
                    expectClientData("" +
                                    "<List name='events' type='0-BACnetWsData/events'>" +
                                    "   <Link name='1' value='/bws/.bacnet/..scope-1/device-1-1/event-1-1-1/log-buffer'/>" +
                                    "   <Link name='2' value='/bws/.bacnet/..scope-1/device-1-1/event-1-1-2/log-buffer'/>" +
                                    "   <Link name='3' value='/bws/.bacnet/..scope-1/device-1-2/event-1-2-1/log-buffer'/>" +
                                    "   <Link name='4' value='/bws/.bacnet/..scope-2/device-2-1/event-2-1-1/log-buffer'/>" +
                                    "</List>"
                    );
                    step("Check /.data/objects");
                    clientData("<List type='0-BACnetWsData/objects'/>");
                    path("/.data/objects");
                    get();
                    expectClientData("" +
                                    "<List name='events' type='0-BACnetWsData/objects'>" +
                                    "   <Link name='1' value='/bws/.bacnet/.local/657780/device,657780'/>" +
                                    "   <Link name='2' value='/bws/.bacnet/..scope-1/device-1-1/device-1-1-1'/>" +
                                    "   <Link name='3' value='/bws/.bacnet/..scope-1/device-1-1/event-1-1-1'/>" +
                                    "   <Link name='4' value='/bws/.bacnet/..scope-1/device-1-1/event-1-1-2'/>" +
                                    "   <Link name='5' value='/bws/.bacnet/..scope-1/device-1-2/device-1-2-1'/>" +
                                    "   <Link name='6' value='/bws/.bacnet/..scope-1/device-1-2/event-1-2-1'/>" +
                                    "   <Link name='7' value='/bws/.bacnet/..scope-2/device-2-1/device-2-1-1'/>" +
                                    "   <Link name='8' value='/bws/.bacnet/..scope-2/device-2-1/event-2-1-1'/>" +
                                    "</List>"
                    );
                    try {  // clean up our mess since we wrote outside /test-data
                        Session.atomicDelete("Indexer Tests", ".../.bacnet/..scope-1");
                        Session.atomicDelete("Indexer Tests", ".../.bacnet/..scope-2");
                    } catch (XDException e) { fail(e.getLocalizedMessage()); }


                    step("Test /.data/histories");
                    try {  // indexer looks for histories in "/.trees", "/my-data", and "/test-data" by default;
                        Session.atomicPost("Indexer Tests", ".../.trees", new UnsignedData("..foo-1", new StringData(Meta.AMPII_HISTORY_LOCATION, "foo-1-history")));
                        Session.atomicPost("Indexer Tests", ".../my-data", new UnsignedData("..foo-2", new StringData(Meta.AMPII_HISTORY_LOCATION, "foo-2-history")));
                        Session.atomicPost("Indexer Tests", ".../test-data", new UnsignedData("..foo-3", new StringData(Meta.AMPII_HISTORY_LOCATION, "foo-3-history")));
                    } catch (XDException e) { fail(e.getLocalizedMessage()); }
                    clientData("<List type='0-BACnetWsData/histories'/>");
                    path("/.data/histories");
                    get();
                    expectClientData("" +
                                    "<List name='histories' type='0-BACnetWsData/histories'>" +
                                    "   <Link name='1' value='/bws/.trees/..foo-1'/>" +
                                    "   <Link name='2' value='/bws/my-data/..foo-2'/>" +
                                    "   <Link name='3' value='/bws/test-data/..foo-3'/>" +
                                    "</List>"
                    );
                    try {  // clean up our mess since we wrote outside /test-data
                        Session.atomicDelete("Indexer Tests", ".../.trees/..foo-1");
                        Session.atomicDelete("Indexer Tests", ".../my-data/..foo-2");
                        Session.atomicDelete("Indexer Tests", ".../test-data/..foo-3");
                    } catch (XDException e) { fail(e.getLocalizedMessage()); }


                    step("Setup /.data/nodes");
                    try {  // indexer looks for nodes in "/.trees", "/my-data", "/test-data", and /.bacnet by default;
                        Session.atomicPost("Indexer Tests", ".../.trees", new UnsignedData("..foo-1", new EnumeratedData(Meta.NODETYPE, "floor")));
                        Session.atomicPost("Indexer Tests", ".../my-data", new UnsignedData("..foo-2", new EnumeratedData(Meta.NODETYPE, "organizational")));
                        Session.atomicPost("Indexer Tests", ".../test-data", new UnsignedData("..foo-3", new EnumeratedData(Meta.NODETYPE, "functional")));
                    } catch (XDException e) { fail(e.getLocalizedMessage()); }
                    step("Check /.data/nodes");
                    clientData("<Collection type='0-BACnetWsData/nodes'/>");
                    path("/.data/nodes");
                    get();
                    expectClientData("" +
                                    "<Collection name='nodes' type='0-BACnetWsData/nodes'>" +
                                    "   <List name='protocol'>" +
                                    "      <Link name='1' value='/bws/.bacnet'/>" +
                                    "   </List>" +
                                    "      <List name='collection'>" +
                                    "      <Link name='1' value='/bws/.bacnet/.local'/>" +
                                    "      <Link name='2' value='/bws/.trees'/>" +
                                    "   </List>" +
                                    "      <List name='device'>" +
                                    "      <Link name='1' value='/bws/.bacnet/.local/657780'/>" +
                                    "   </List>" +
                                    "   <List name='tree'>" +
                                    "      <Link name='1' value='/bws/.trees/.geo'/>" +
                                    "   </List>" +
                                    "   <List name='floor'>" +
                                    "      <Link name='1' value='/bws/.trees/..foo-1_1'/>" +
                                    "   </List>" +
                                    "   <List name='organizational'>" +
                                    "      <Link name='1' value='/bws/my-data/..foo-2_1'/>" +
                                    "   </List>" +
                                    "   <List name='functional'>" +
                                    "      <Link name='1' value='/bws/test-data/..foo-3_1'/>" +
                                    "   </List>" +
                                    "</Collection>"
                    );
                    step("Check /.data/nodes/floor");
                    clientData("<List memberType='Link'/>");
                    path("/.data/nodes/floor");
                    get();
                    expectClientData(""+
                                    "<List name='floor' memberType='Link'>" +
                                    "   <Link name='1' value='/bws/.trees/..foo-1_1'/>" +
                                    "</List>"
                    );
                    try {  // clean up our mess since we wrote outside /test-data
                        Session.atomicDelete("Indexer Tests", ".../.trees/..foo-1");
                        Session.atomicDelete("Indexer Tests", ".../my-data/..foo-2");
                        Session.atomicDelete("Indexer Tests", ".../test-data/..foo-3");
                    } catch (XDException e) { fail(e.getLocalizedMessage()); }

                }
            }
    };
}


