// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application;

import org.ampii.xd.application.hooks.HistoryHooks;
import org.ampii.xd.bindings.Binding;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Meta;
import org.ampii.xd.resolver.Eval;
import org.ampii.xd.resolver.Fabricator;
import java.util.Calendar;


/**
 * Responsible for finding and processing histories (can be extended with hooks and bindings).
 *
 * @author daverobin
 */
public class Historian {

    public static final int  PERIOD_MONTH = -1; // these are special values used for int period, otherwise it's in seconds
    public static final int  PERIOD_YEAR  = -2;

    public enum PeriodMethod { INTERPOLATION, AVERAGE, MINIMUM, MAXIMUM, AFTER, BEFORE, CLOSEST, DEFAULT, ENDING_AVERAGE, ENDING_MINIMUM, ENDING_MAXIMUM, FAKE } // FAKE is AMPII-specific

    /**
     * Called by {@link Fabricator} to find or make the log buffer corresponding to .../$history.
     * <p>
     * This will return a ListData that contains the log buffer, which is most likely a ListData with a binding on it to
     * fill in the records/children when getContextualizedChildren() is called, but it could also just be a fabricated
     * ListData with the requested records in it and no binding - that decision is up to the hook or binding on the target.
     */
    public static Data findHistoryLogBuffer(Data target) throws XDException {

        // We first give the application-specific hook a chance to find or make the log buffer
        Data result = HistoryHooks.findHistoryLogBuffer(target);
        if (result != null) return result;
        // if the hook didn't process it, then check with the binding on the target
        Binding binding = target.findBinding();
        if (binding != null) result = binding.findHistoryLogBuffer(target);
        if (result != null) return result;
        // if neither handled it, then do the default by looking for a pointer to the log buffer
        Data historyLocation = target.find(Meta.AMPII_HISTORY_LOCATION);
        if (historyLocation != null) return Eval.eval(target, historyLocation.stringValue());
        return null; // not found, no history
    }

    /**
     * Called by HistoryPeriodicFunction class to make the results for .../historyPeriodic(...) URI function.
     */
    public static String computeHistoryPeriodic(Data target, Calendar start, int period, int count, PeriodMethod method) throws XDException {
        // We first give the application-specific hook a chance to produce the results
        String result = HistoryHooks.computeHistoryPeriodic(target, start, period, count, method);
        if (result != null) return result;
        // if the hook didn't process it, then check with the binding on the target
        Binding binding = target.findBinding();
        if (binding != null) result = binding.computeHistoryPeriodic(target, start, period, count, method);
        if (result != null) return result;
        // if neither of those handled it, then do computing ourselves on the log buffer
        Data buffer = findHistoryLogBuffer(target);
        if (buffer != null) {
            // one last chance to check with hooks and bindings... this time on the log buffer itself
            result = HistoryHooks.computeHistoryPeriodic(buffer, start, period, count, method);
            if (result != null) return result;
            // if the hook didn't process it, then check with the binding on the log buffer
            binding = buffer.findBinding();
            if (binding != null) result = binding.computeHistoryPeriodic(buffer, start, period, count, method);
            if (result != null) return result;
            // ok all that failed... time to do the calculations directly on whatever records are in the buffer (probably just for simulation)
            return defaultComputeHistoryPeriodic(buffer, start, period, count, method);
        }
        return null; // not found, no history
    }


    private static String defaultComputeHistoryPeriodic(Data buffer, Calendar start, int period, int count, PeriodMethod method) throws XDException {
        StringBuilder results = new StringBuilder();
        for (int i = 0; i < count; i++) {
            results.append(defaultComputeOneInterval(buffer, start, period, method) + "\n");
            switch (period) {
                case PERIOD_MONTH: start.add(Calendar.MONTH,1); break;
                case PERIOD_YEAR:  start.add(Calendar.YEAR,1);  break;
                default:           start.add(Calendar.SECOND,period);
            }
        }
        return results.toString();
    }

    private static String defaultComputeOneInterval(Data buffer, Calendar start, int period, PeriodMethod method) throws XDException {
        // TODO make a better a simulation for this based on actual trend record data!
        switch(method) {
            case FAKE:
                return String.valueOf(start.getTimeInMillis()/1000); // generate fake data for testing
            case INTERPOLATION:
            case AVERAGE:
            case MINIMUM:
            case MAXIMUM:
            case AFTER:
            case BEFORE:
            case CLOSEST:
            case DEFAULT:
            case ENDING_AVERAGE:
            case ENDING_MINIMUM:
            case ENDING_MAXIMUM:
                return "? " + Errors.ARG_NOT_SUPPORTED + " method not implemented";
            default:
                throw new XDError("The method for computeOneInterval() is not a valid enum value");// internal error - should have been caught earlier
        }
    }

}
