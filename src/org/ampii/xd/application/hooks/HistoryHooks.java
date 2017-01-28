// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application.hooks;

import org.ampii.xd.application.Historian.PeriodMethod;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.application.Historian;

import java.util.Calendar;

/**
 * This is a way to add extensions to the {@link Historian} handler for finding and handling histories.
 * <p>
 * If you want to add a custom history handling without changing the core AMPII files, create a {@link HistoryHooks.External}
 * interface and inject it with {@link HistoryHooks#registerExternal}.
 *
 * @author daverobin
 */
public class HistoryHooks {

    public interface External {
        Data   findHistoryLogBuffer(Data target) throws XDException;
        String computeHistoryPeriodic(Data target, Calendar start, int period, int count, PeriodMethod method) throws XDException;
    }

    private static External external;  // outside code can add its own hooks.

    public static void    registerExternal(External hooks) { external = hooks; }

    public static Data    findHistoryLogBuffer(Data target) throws XDException {
        if (external != null) return external.findHistoryLogBuffer(target);
        return null;
    }

    public static String  computeHistoryPeriodic(Data target, Calendar start, int period, int count, PeriodMethod method) throws XDException {
        if (external != null) return external.computeHistoryPeriodic(target, start, period, count, method);
        return null;
    }

}
