// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.basetypes.ListData;
import org.ampii.xd.data.basetypes.RealData;
import org.ampii.xd.data.basetypes.UnsignedData;
import org.ampii.xd.database.Session;
import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests the capabilities of /.subs.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class SubscriptionTests {

    public static Test[] tests = {
            new Test("Basic subscription cov callback test") {
                public void execute() throws TestException,XDException {
                    step("creation of a subscription record");
                    serverData(
                            "<Composition name='sub-test'>" +
                             "    <List name='callback' writable='true' memberType='0-BACnetWsSubscriptionCallback'/>\n" +
                             "    <Real name='real-target' value='1.0'/>" +
                             "    <Unsigned name='unsigned-target' value='10'/>" +
                             "</Composition>");
                    clientData(
                            "<Composition>" +
                            "    <String name='label' value='subtest1'/>" +
                            "    <String name='callback' value='" + getServerBaseHttpURI() + getServerTestDataPath() + "/sub-test/callback?alt="+env.defaultFormat+"'/>" +
                            "    <Unsigned name='lifetime' value='60'/>" +
                            "    <List name='covs'>" +
                            "        <Composition name='1'>" +
                            "            <String name='path' value='" + getServerTestDataPath() + "/sub-test/real-target'/>" +
                            "            <Real name='increment' value='1.0'/>" +
                            "        </Composition>" +
                            "        <Composition name='2'>" +
                            "            <String name='path' value='" + getServerTestDataPath() + "/sub-test/unsigned-target'/>" +
                            "            <Real name='increment' value='10.0'/>" +
                            "        </Composition>" +
                            "        <Composition name='3'>" +
                            "            <String name='path' value='" + getServerTestDataPath() + "/sub-test/string-target'/>" +
                            "        </Composition>" +
                            "    </List>" +
                            "</Composition>");
                    path("/.subs");
                    post();
                    expectStatusCode(201);
                    expectResponseHeaderPresent("Location");
                    String location = getResponseHeader("Location", "<none>"); // remember the created location, we'll use it later

                    step("check that the initial callback succeeded (NOTE: this test assumes non-batched and in order!);");
                    //waitFor(env.covCallbackFailTime, (test) -> getServerData().get("callback").size() == 2); // wait for callback children to appear
                    delay(env.covCallbackFailTime);//TODO fix waitFor()!
                    expectServerDataItemValue("callback/1/1", "1.0");
                    expectServerDataItemValue("callback/2/1", "10");
                    Session.atomicPut("SubscriptionTests", serverDataPath + "/callback", new ListData("")); // clear the callback records

                    step("change floating point value by the cov increment and check callback");
                    Session.atomicPut("SubscriptionTests", serverDataPath+"/real-target", new RealData("", 2.1));
                    ; //  change by the cov increment amount
                    //waitFor(env.covCallbackFailTime, (test) -> getServerData().get("callback").size() == 1); // wait for callback child to appear
                    delay(env.covCallbackFailTime);//TODO fix waitFor()!

                    expectServerDataItemValue("callback/1/1", "2.1");
                    Session.atomicPut("SubscriptionTests", serverDataPath + "/callback", new ListData("")); // clear the callback records

                    step("change floating point value by less than cov increment and check callback");
                    Session.atomicPut("SubscriptionTests", serverDataPath+"/real-target", new RealData("", 2.5));
                    ; //  change by less than cov increment amount
                    delay(env.covCallbackFailTime); // wait for callback children to *not* appear
                    expectServerDataItemAbsent("callback/1");

                    step("change unsigned point value by the cov increment and check callback");
                    Session.atomicPut("SubscriptionTests", serverDataPath+"/unsigned-target", new UnsignedData("", 21));
                    ; //  change by the cov increment amount
                    //waitFor(env.covCallbackFailTime, (test) -> getServerData().get("callback").size() == 1); // wait for callback child to appear
                    delay(env.covCallbackFailTime);//TODO fix waitFor()!
                    expectServerDataItemValue("callback/1/1", "21");
                    Session.atomicPut("SubscriptionTests", serverDataPath + "/callback", new ListData("")); // clear the callback records

                    step("change unsigned point value by less than cov increment and check callback");
                    Session.atomicPut("SubscriptionTests", serverDataPath + "/unsigned-target", new UnsignedData("", 25));
                    ; //  change by less than cov increment amount
                    delay(env.covCallbackFailTime); // wait for callback children to *not* appear
                    expectServerDataItemAbsent("callback/1/1");

                    step("read and validate the subscription record");
                    uri(location); // set URI of new record based on remembered Location Header
                    clientData("<Composition type='0-BACnetWsSubscriptionRecord' partial='true'/>");
                    get();
                    expectClientData();
                    expectClientDataItemPresent("lifetime");
                    //we could do more validation here, but it seems to be working and the above tests that the record exists

                    step("cancel the subscription by zeroing the lifetime");
                    pathAdd("/lifetime");
                    clientData("<Unsigned name='lifetime' value='0'/>");
                    put();
                    expectSuccessCode();

                    step("check that the subscription has been deleted");
                    delay(env.subsRecordRemovalFailTime);
                    pathRemove("/lifetime");
                    clientData("<Composition type='0-BACnetWsSubscriptionRecord' partial='true'/>");
                    get();
                    expectStatusCode(404);


                }
            }
    };
}
