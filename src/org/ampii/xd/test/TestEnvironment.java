// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test;

import org.ampii.xd.data.Data;
import java.util.function.Consumer;

/**
 * The environment parameters (timeouts, etc) established by the {@link Tester} for the individual {@link Test} instances to use.
 *
 * @author drobin
 */
public class TestEnvironment {

    public String          defaultFormat;
    public int             covCallbackFailTime;
    public int             logCallbackFailTime;
    public int             subsRecordRemovalFailTime;
    public int             multiRecordRemovalFailTime;
    public Consumer<Test>  stepCallback;

    public TestEnvironment(String defaultFormat, Data testPars, Consumer<Test> step) {
        this.defaultFormat = defaultFormat;
        this.covCallbackFailTime         = (int)testPars.longValueOf("covCallbackFailTime", 1100);
        this.logCallbackFailTime         = (int)testPars.longValueOf("logCallbackFailTime", 1100);
        this.subsRecordRemovalFailTime   = (int)testPars.longValueOf("subsRecordRemovalFailTime", 1100);
        this.multiRecordRemovalFailTime  = (int)testPars.longValueOf("multiRecordRemovalFailTime", 1100);
        this.stepCallback                = step;
    }

    public void stepTest(Test test)  { if (stepCallback != null)  stepCallback.accept(test); }

}
