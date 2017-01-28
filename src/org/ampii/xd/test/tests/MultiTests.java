// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Meta;
import org.ampii.xd.database.Session;
import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests the capabilities of /.multi.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class MultiTests  {

    public static Test[] tests = {
            new Test("Persistent .multi creation, and usage, and removal") {
                public void execute() throws TestException {
                    step("creation of a persistent multi read record");
                    clientData(""+
                            "<Composition>" +
                            "<Unsigned name='lifetime' value='20'/>" +
                            "<List name='values'>" +
                            "<Any name='1' via='" + getServerDataPrefix() + "/.info/vendor-name'/>" +
                            "</List>" +
                            "</Composition>");
                    clientDataPars("metadata=base,via,value");
                    path("/.multi");
                    post();
                    expectStatusCode(201);
                    // check that we got a 'Location' header
                    String location = getResponseHeader("Location", null);
                    if (location == null) fail("'Location' header was not returned");

                    step("read and validate the newly created record");
                    uri(location);

                    clientData("<Composition/>");
                    query("metadata=base,value");
                    get();
                    expectClientData();
                    expectClientDataItemValue("values/1/$base", "String"); // test that the Any has been replaced with String
                    expectClientDataItemValue("values/1", Session.atomicGetString("MultiTest",".../.info/vendor-name", "")); // and that it actually got the right value!

                    step("cancel the multi by zeroing the lifetime");
                    pathAdd("/lifetime");
                    clientData("<Unsigned name='lifetime' value='0'/>");
                    put();
                    expectSuccessCode();

                    step("check that the multi record has been deleted");
                    delay(env.multiRecordRemovalFailTime);
                    pathRemove("/lifetime");
                    clientData("<Composition/>");
                    get();
                    expectStatusCode(404);

                }
            },

            new Test("non-persistent .multi query") {
                public void execute() throws TestException {
                    // there's only one step here: post a .multi query and and parse the ephemeral result record
                    clientData(""+
                            "<Composition>" +  // no lifetime
                            "<List name='values'>" +
                            "<Any name='1' via='" + getServerDataPrefix() + "/.info/vendor-name'/>" +
                            "</List>" +
                            "</Composition>");
                    clientDataPars("metadata=base,via,value");
                    path("/.multi");
                    post();
                    expectResponseHeaderAbsent("Location"); // we should *not* get a location header for non-persistent query
                    clientData("<Composition type='0-BACnetWsMultiRecord' partial='true'/>"); // set the target for parsing the results
                    expectClientDataItemValue("values/1/$base", "String"); // test that the Any has been replaced with String
                    expectClientDataItemValue("values/1/$via", getServerDataPrefix() + "/.info/vendor-name"); // check the via is still present
                    expectClientDataItemValue("values/1", Session.atomicGetString("MultiTest",".../.info/vendor-name", "?testerror?")); // and check that it actually got the right value!
                }
            },
            new Test("non-persistent .multi query with embedded errors") {
                public void execute() throws TestException {
                    clientData(""+
                            "<Composition>" +  // no lifetime
                            "    <List name='values'>" +
                            "        <Any name='1' via='" + getServerDataPrefix() + "/.info/vendor-name'/>" +
                            "        <Any name='2' via='" + getServerDataPrefix() + "/.info/BOGUS'/>" +  // error: not found
                            "        <Any name='3' via='" + getServerDataPrefix() + "/.info/vendor-identifier'/>" +
                            "        <Any name='4' via='' />" +  // error: empty via
                            "        <Any name='5' via='" + getServerDataPrefix() + "/.info/model-name'/>" +
                            "    </List>" +
                            "</Composition>");
                    clientDataPars("metadata=base,via,value");
                    path("/.multi");
                    post();
                    expectResponseHeaderAbsent("Location"); // we should *not* get a location header for non-persistent query
                    clientData("<Composition type='0-BACnetWsMultiRecord' partial='true'/>"); // set the target for parsing the results

                    expectClientDataItemValue("values/1/$base", "String"); // the Any has been replaced with String
                    expectClientDataItemValue("values/1/$via", getServerDataPrefix() + "/.info/vendor-name"); // the via is still present
                    expectClientDataItemValue("values/1", Session.atomicGetString("MultiTest",".../.info/vendor-name", "?testerror?")); // and it actually got the right value!

                    expectClientDataItemValue("values/2/$base", "Any"); // the Any has *not* been replaced for error situation
                    expectClientDataItemValue("values/2/$via", getServerDataPrefix() + "/.info/BOGUS"); // the via is still present
                    expectClientDataItemValue("values/2/$error", String.valueOf(Errors.DATA_NOT_FOUND)); // the error should be present and equal to Errors.DATA_NOT_FOUND

                    expectClientDataItemValue("values/3/$base", "Unsigned"); // the Any has been replaced with Unsigned
                    expectClientDataItemValue("values/3/$via", getServerDataPrefix() + "/.info/vendor-identifier"); // the via is still present
                    expectClientDataItemValue("values/3", Session.atomicGetString("MultiTest",".../.info/vendor-identifier", "?testerror?")); // and it actually got the right value!

                    expectClientDataItemValue("values/4/$base", "Any"); // the Any has *not* been replaced for error situation
                    expectClientDataItemValue("values/4/$via", ""); // the via is still present... and still empty!
                    expectClientDataItemPresent("values/4/$error"); // not sure what the required error number is for this case, so just test "presence"

                    expectClientDataItemValue("values/5/$base", "String"); // the Any has been replaced with String
                    expectClientDataItemValue("values/5/$via", getServerDataPrefix() + "/.info/model-name"); // the via is still present
                    expectClientDataItemValue("values/5", Session.atomicGetString("MultiTest",".../.info/model-name", "?testerror?")); // and it actually got the right value!

                }
            },
            new Test(".multi write with embedded errors") {
                public void execute() throws TestException {
                    serverData("" +
                            "<Composition name='targ' writable='true'>" +
                            "    <String name='str'/>" +
                            "    <Boolean name='bool'/>" +
                            "</Composition>");
                    clientData(""+
                            "<Composition>" +  // no lifetime
                            "    <List name='values'>" +
                            "        <String name='1'  via='" + getServerTestDataPath() + "/targ/str'  value='hi'/>" +
                            "        <String name='2'  via='" + getServerTestDataPath() + "/targ/BOGUS' value='ignored'/>" +
                            "        <Boolean name='3' via='" + getServerTestDataPath() + "/targ/bool'  value='true'/>" +
                            "    </List>" +
                            "</Composition>");
                    clientDataPars("metadata=base,via,value");
                    path("/.multi");
                    post();
                    expectResponseHeaderAbsent("Location"); // we should *not* get a location header for non-persistent query

                    clientData("<Composition type='0-BACnetWsMultiRecord' partial='true'/>"); // set the target for parsing the results

                    expectClientDataItemValue("$failures/1", "values/2"); // check that the failure was properly recorded at the top

                    expectClientDataItemValue("values/1/$base", "String");
                    expectClientDataItemValue("values/1/$via", getServerTestDataPath() + "/targ/str"); // the via is still present

                    expectClientDataItemValue("values/2/$base", "String");
                    expectClientDataItemValue("values/2/$via", getServerTestDataPath() + "/targ/BOGUS"); // the via is still present
                    expectClientDataItemValue("values/2/$error", String.valueOf(Errors.DATA_NOT_FOUND))
                    ; // the error should be present and equal to Errors.DATA_NOT_FOUND

                    expectClientDataItemValue("values/3/$base", "Boolean");
                    expectClientDataItemValue("values/3/$via", getServerTestDataPath() + "/targ/bool"); // the via is still present

                    expectServerDataItemValue("str", "hi");     // and finally, most importantly, check that it actually changed the server data too
                    expectServerDataItemValue("bool", "true");
                }
            },

            new Test(".multi write with no errors") {
                public void execute() throws TestException,XDException {
                    serverData(""+
                            "<Composition name='targ' writable='true'>" +
                            "    <String name='str'/>" +
                            "    <Boolean name='bool'/>" +
                            "</Composition>");
                    clientData(""+
                            "<Composition>" +  // no lifetime
                            "    <List name='values'>" +
                            "        <String name='1'  via='" + getServerTestDataPath() + "/targ/str'  value='hi'/>" +
                            "        <Boolean name='2' via='" + getServerTestDataPath() + "/targ/bool'  value='true'/>" +
                            "    </List>" +
                            "</Composition>");
                    clientDataPars("metadata=base,via,value");
                    path("/.multi");
                    post();
                    expectResponseHeaderAbsent("Location"); // we should *not* get a location header for non-persistent query
                    clientData("<Composition type='0-BACnetWsMultiRecord' partial='true'/>"); // set the target for parsing the results
                    expectClientDataItemAbsent("$failures/1"); // check that there were no failures  ($failures might be present, but it should be empty);
                    // if it's not marked as $truncated, then the 'values' must be present, otherwise, 'values' must be absent
                    if (!getClientData().booleanValueOf(Meta.TRUNCATED, false)) {
                        if (getClientData().find("values") == null)
                            fail("results not marked as $truncated but 'values' is missing");
                    } else {
                        if (getClientData().find("values") != null)
                            fail("results is marked as $truncated but 'values' is present");
                    }
                }
            }
    };

}
