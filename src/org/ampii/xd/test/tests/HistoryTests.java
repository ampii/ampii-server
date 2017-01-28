// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.common.Errors;
import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests for $hasHistory, $history and historyPeriodic().
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class HistoryTests {

    public static Test[] tests = {
            new Test("History Tests") {
                public void execute() throws TestException {
                    serverData("{" +
                            "      '$base':'Composition'," +
                            "      'has-history':{ '$base':'Real', '$..historyLocation':'../the-history' }," +
                            "      'no-history' :{ '$base':'Real' }," +
                            "      'the-history':{ " +
                            "          '$base':'List', '$memberType':'0-BACnetLogRecord', " +
                            "          '1':{'timestamp':'2014-04-02T13:01:00-04:00','log-datum':{'real-value':75.1},'status-flags':{}}," +
                            "          '2':{'timestamp':'2014-04-02T13:02:00-04:00','log-datum':{'real-value':75.2},'status-flags':{}}," +
                            "          '3':{'timestamp':'2014-04-02T13:03:00-04:00','log-datum':{'real-value':75.3},'status-flags':{}}," +
                            "          '4':{'timestamp':'2014-04-02T13:04:00-04:00','log-datum':{'real-value':75.4},'status-flags':{}}," +
                            "          '5':{'timestamp':'2014-04-02T13:05:00-04:00','log-datum':{'real-value':75.5},'status-flags':{}}" +
                            "      }" +
                            "   }");

                    step("Check $hasHistory true");
                    clientData("<Boolean/>");
                    pathResetAndAdd("/has-history/$hasHistory");
                    get();
                    expectClientData("<Boolean value='true'/>");

                    step("Check $hasHistory false");
                    clientData("<Boolean/>");
                    pathResetAndAdd("/no-history/$hasHistory");
                    get();
                    expectClientData("<Boolean value='false'/>");

                    step("Check $history present");
                    clientData("<List memberType='0-BACnetLogRecord'/>");
                    pathResetAndAdd("/has-history/$history");
                    get();
                    expectClientData("{ " +
                            "   '$base':'List', '$memberType':'0-BACnetLogRecord', " +
                            "   '1':{'timestamp':'2014-04-02T13:01:00-04:00','log-datum':{'real-value':75.1},'status-flags':{}}," +
                            "   '2':{'timestamp':'2014-04-02T13:02:00-04:00','log-datum':{'real-value':75.2},'status-flags':{}}," +
                            "   '3':{'timestamp':'2014-04-02T13:03:00-04:00','log-datum':{'real-value':75.3},'status-flags':{}}," +
                            "   '4':{'timestamp':'2014-04-02T13:04:00-04:00','log-datum':{'real-value':75.4},'status-flags':{}}," +
                            "   '5':{'timestamp':'2014-04-02T13:05:00-04:00','log-datum':{'real-value':75.5},'status-flags':{}}" +
                            "}");

                    step("Check $history missing");
                    clientData("<List memberType='0-BACnetLogRecord'/>");
                    pathResetAndAdd("/no-history/$history");
                    get();
                    expectStatusCode(404);
                    expectErrorNumber(Errors.METADATA_NOT_FOUND);

                    step("Check historyPeriodic() function present");
                    pathResetAndAdd("/has-history/historyPeriodic(start=2014-04-02T13:01:00-04:00,method=org.ampii.fake)");
                    get();
                    expectSuccessCode();

                    step("Check historyPeriodic() function absent");
                    pathResetAndAdd("/no-history/historyPeriodic(start=2014-04-02T13:01:00-04:00)");
                    get();
                    expectStatusCode(403);
                    expectErrorNumber(Errors.NO_HISTORY);

                    step("Check historyPeriodic() function present in plain text");
                    pathResetAndAdd("/has-history/historyPeriodic(start=2014-04-02T13:01:00-04:00)");
                    alt("plain");
                    get();
                    expectSuccessCode();
                    expectResponseText();

                    // TODO actually check history periodic results


                }
            }
    };
}
