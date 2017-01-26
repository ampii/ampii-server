// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests the various capabilities of Links.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author drobin
 */
public class LinkTests {

    public static Test[] tests = {
            /*
            new Test("Post links and check error codes") {
                public void execute() throws TestException {
                    // Links have preread() that validates the link target, so we'll test that
                    serverData("<Collection name='foo' writable='true' memberType='Link'/>");
                    clientData("<Link name='valid' value='/bws/.auth' targetType='0-BACnetWsAuth'/>");
                    post();
                    expectSuccessCode();
                    clientData("<Link name='invalid-type' value='/bws/.auth' targetType='0-BACnetBOGUS'/>");
                    post();
                    expectSuccessCode();
                    clientData("<Link name='invalid-target' value='/bws/.auth/BOGUS' targetType='0-BACnetWsAuth'/>");
                    post();
                    expectSuccessCode();
                    clientData("<Collection/>");
                    query("metadata=base,value");
                    get();
                    expectClientData(
                            "<Collection name='foo'>" +
                            "    <Link name='valid' value='/bws/.auth' />" +
                            "    <Link name='invalid-type' value='/bws/.auth' error='1034'>" +
                            //" <Error>Can't find definition for $targetType of '0-BOGUS'</Error>" + // shouldn't test for exact error strings!
                            "    </Link>" +
                            "    <Link name='invalid-target' value='/bws/.auth/BOGUS' error='1034'>" +
                            //" <Error>Can't find target data</Error>" +
                            "    </Link>" +
                            "</Collection>");
                    //TODO some day we might want to implement something like the following for non-critical mismatches that are flagged as warnings but not considered "failures"
                    //desireClientDataItemValue("invalid-type/errorText", "Can't find definition for $targetType of '0-BOGUS'");
                    //desireClientDataItemValue("invalid-target/errorText","Can't find target data");
                }
            },*/
            new Test("Test relative links and $target") {
                public void execute() throws TestException {
                    serverData(
                            "<Composition name='testme'>" +
                            "    <Collection name='some-links'>" +
                            "        <Link name='link1' value='target1'/>" +  // reference fom link from one level down
                            "        <List name='list-of-links'>" +
                            "            <Link name='link2' value='target2'/>" + // reference fom link from two level down
                            "        </List>" +
                            "        <Link name='link3' value='some-links/list-of-links'/>" +
                            "        <Link name='link4' value='some-links/link3/$target/1/$target'/>" + // torture test (points to target2)
                            "    </Collection>" +
                            "    <String name='target1' value='value1'/>" +
                            "    <String name='target2' value='value2'/>" +
                            "</Composition>");
                    // could do more (and simpler) tests here, but this just jumps straight to the torture test.
                    pathAdd("/some-links/link4/$target");
                    clientData("<String/>");
                    get();
                    expectClientData("<String value='value2'/>");
                }
            },
            new Test("Test $descendants with $target for filter") {
                public void execute() throws TestException {
                    // this assumes the built-in BACnet device is present with the properties in the default order
                    step("try all filtered descendants first");
                    path("/.bacnet/.local/.this/.device/$descendants");
                    query("filter=$target/$name/contains(object)");
                    clientData("<List memberType='Link'/>");
                    get();
                    expectClientData(  // NOTE! might actually come in in different order on the wire
                            "<List memberType='Link' partial='true'>" +
                            "    <Link name='1' value='object-identifier'/>" +
                            "    <Link name='2' value='object-name'/>" +
                            "    <Link name='3' value='object-type'/>" +
                            "    <Link name='4' value='protocol-object-types-supported'/>" +
                            "    <Link name='5' value='object-list'/>" +
                            "</List>");
                }
            }
    };

}
