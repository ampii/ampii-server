// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test.tests;

import org.ampii.xd.data.Meta;
import org.ampii.xd.test.Test;
import org.ampii.xd.test.TestException;

/**
 * Tests filtering and limiting responses based on time, sequence, etc.
 * <p>
 * Add this class to the config file indicated by Application.testDefinitionFile if you want these tests to run.
 *
 * @author daverobin
 */
public class RangeTests {

        public static Test[] tests = {
            new Test("Range Tests") {
                public void execute() throws TestException {
                    serverData("" +
                            "<List name='log-buffer-1'>" +
                            "    <Sequence name='1'>" +
                            "        <DateTime name='timestamp' value='2014-04-02T13:01:00-04:00'/>" +
                            "        <Choice name='log-datum'>" +
                            "            <Real name='real-value' value='75.1'/>" +
                            "        </Choice>" +
                            "        <BitString name='status-flags'/>" +
                            "    </Sequence>" +
                            "    <Sequence name='2'>" +
                            "        <DateTime name='timestamp' value='2014-04-02T13:02:00-04:00'/>" +
                            "        <Choice name='log-datum'>" +
                            "            <Real name='real-value' value='75.2'/>" +
                            "        </Choice>" +
                            "        <BitString name='status-flags'/>" +
                            "    </Sequence>" +
                            "    <Sequence name='3'>" +
                            "        <DateTime name='timestamp' value='2014-04-02T13:03:00-04:00'/>" +
                            "        <Choice name='log-datum'>" +
                            "            <Real name='real-value' value='75.3'/>" +
                            "        </Choice>" +
                            "        <BitString name='status-flags'/>" +
                            "    </Sequence>" +
                            "    <Sequence name='4'>" +
                            "        <DateTime name='timestamp' value='2014-04-02T13:04:00-04:00'/>" +
                            "        <Choice name='log-datum'>" +
                            "            <Real name='real-value' value='75.4'/>" +
                            "        </Choice>" +
                            "        <BitString name='status-flags'/>" +
                            "    </Sequence>" +
                            "    <Sequence name='5'>" +
                            "        <DateTime name='timestamp' value='2014-04-02T13:05:00-04:00'/>" +
                            "        <Choice name='log-datum'>" +
                            "            <Real name='real-value' value='75.5'/>" +
                            "        </Choice>" +
                            "        <BitString name='status-flags'/>" +
                            "    </Sequence>" +
                            "</List>\n");
                    step("Time Range");
                    clientData("<List memberType='0-BACnetLogRecord'/>");
                    query("published-gt=2014-04-02T13:01:00-04:00&published-lt=2014-04-02T13:05:00-04:00");
                    get();
                    expectClientData("" +
                            "{" +
                            "   '$base':'List'," +
                            "   '$memberType':'0-BACnetLogRecord'," +
                            "   '$partial':true," +
                            "   '2':{" +
                            "      'timestamp':'2014-04-02T13:02:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.2" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }," +
                            "   '3':{" +
                            "      'timestamp':'2014-04-02T13:03:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.3" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }," +
                            "   '4':{" +
                            "      'timestamp':'2014-04-02T13:04:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.4" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }" +
                            "}");
                    step("Time Range, Limited");
                    clientData("<List memberType='0-BACnetLogRecord'/>");
                    query("max-results=2&published-gt=2014-04-02T13:01:00-04:00&published-lt=2014-04-02T13:05:00-04:00");
                    get();
                    expectClientData("" +
                            "{" +
                            "   '$base':'List'," +
                            "   '$memberType':'0-BACnetLogRecord'," +
                            "   '$partial':true," +
                            "   '$next':{'$value':'', '$..matchAny':true}," +
                            "   '2':{" +
                            "      'timestamp':'2014-04-02T13:02:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.2" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }," +
                            "   '3':{" +
                            "      'timestamp':'2014-04-02T13:03:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.3" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }" +
                            "}");
                    step("Time Range, Reversed, Limited");
                    clientData("<List memberType='0-BACnetLogRecord'/>");
                    query("max-results=2&reverse=true&published-gt=2014-04-02T13:01:00-04:00&published-lt=2014-04-02T13:05:00-04:00");
                    get();
                    expectClientData("" +
                            "{" +
                            "   '$base':'List'," +
                            "   '$memberType':'0-BACnetLogRecord'," +
                            "   '$partial':true," +
                            "   '$next':{'$value':'', '$..matchAny':true}," +
                            "   '3':{" + // THIS IS DELIBERATELY OUT OF ORDER from that which is received, we don't want the test fooled by send ordering
                            "      'timestamp':'2014-04-02T13:03:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.3" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }," +
                            "   '4':{" + // SEE ABOVE
                            "      'timestamp':'2014-04-02T13:04:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.4" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }" +
                            "}");
                    step("Sequence Range");
                    clientData("<List memberType='0-BACnetLogRecord'/>");
                    query("sequence-gt=1&sequence-lt=5");
                    get();
                    expectClientData("" +
                            "{" +
                            "   '$base':'List'," +
                            "   '$memberType':'0-BACnetLogRecord'," +
                            "   '$partial':true," +
                            "   '3':{" + // DELIBERATELY OUT OF ORDER from on the wire
                            "      'timestamp':'2014-04-02T13:03:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.3" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }," +
                            "   '2':{" +
                            "      'timestamp':'2014-04-02T13:02:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.2" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }," +
                            "   '4':{" +
                            "      'timestamp':'2014-04-02T13:04:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.4" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }" +
                            "}");
                    step("Sequence Range, Limited");
                    clientData("<List memberType='0-BACnetLogRecord'/>");
                    query("max-results=2&sequence-gt=1&sequence-lt=5");
                    get();
                    expectClientData("" +
                            "{" +
                            "   '$base':'List'," +
                            "   '$memberType':'0-BACnetLogRecord'," +
                            "   '$partial':true," +
                            "   '$next':{'$value':'', '$..matchAny':true}," +
                            "   '2':{" +
                            "      'timestamp':'2014-04-02T13:02:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.2" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }," +
                            "   '3':{" +
                            "      'timestamp':'2014-04-02T13:03:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.3" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }" +
                            "}");
                    step("Sequence Range, Reversed, Limited");
                    clientData("<List memberType='0-BACnetLogRecord'/>");
                    query("max-results=2&reverse=true&sequence-gt=1&sequence-lt=5");
                    get();
                    expectClientData("" +
                            "{" +
                            "   '$base':'List'," +
                            "   '$memberType':'0-BACnetLogRecord'," +
                            "   '$partial':true," +
                            "   '$next':{'$value':'', '$..matchAny':true}," +
                            "   '3':{" + // DELIBERATELY OUT OF ORDER from that which is received, we don't want the test fooled by send ordering
                            "      'timestamp':'2014-04-02T13:03:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.3" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }," +
                            "   '4':{" + // SEE ABOVE
                            "      'timestamp':'2014-04-02T13:04:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.4" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }" +
                            "}");
                    step("Sequence Range, Reversed, Limited, $next pointer test");
                    clientData("<List memberType='0-BACnetLogRecord'/>");
                    query("max-results=2&reverse=true&sequence-gt=1&sequence-lt=5");
                    get();
                    expectClientDataItemPresent(Meta.NEXT);
                    uri(getClientData().stringValueOf(Meta.NEXT, "<none>"));
                    clientData("<List memberType='0-BACnetLogRecord'/>");
                    get();
                    expectClientData("" +
                            "{" +
                            "   '$base':'List'," +
                            "   '$memberType':'0-BACnetLogRecord'," +
                            "   '$partial':true," +
                            "   '2':{" +
                            "      'timestamp':'2014-04-02T13:02:00-04:00'," +
                            "      'log-datum':{" +
                            "         'real-value':75.2" +
                            "      }," +
                            "      'status-flags':{}" +
                            "   }" +
                            "}");

                }
            }
    };
}
