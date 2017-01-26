// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.functions;

import org.ampii.xd.application.Historian;
import org.ampii.xd.application.Historian.PeriodMethod;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.DateTimeData;
import org.ampii.xd.data.basetypes.IntegerData;
import org.ampii.xd.data.basetypes.StringData;

import java.util.Calendar;

/**
 * An Implementation of the .../historyPeriodic() URI function, called from {@link Functions}
 */
public class HistoryPeriodicFunction extends Functions {

    static Data invoke(Data target, String argString) throws XDException {
        Data startArg  = new DateTimeData("start");
        Data periodArg = new StringData("period", "hour");
        Data countArg  = new IntegerData("count", 24);
        Data methodArg = new StringData("method", "default");
        parseArgs(argString, startArg, periodArg, countArg, methodArg);
        // now validate the args
        if (!startArg.hasValue()) throw new XDException(Errors.MISSING_PARAMETER, "The 'start' argument to historyPeriodic() is required");
        Calendar start = startArg.calendarValue();
        int count = countArg.intValue(); // nothing to validate
        int period; 
        switch (periodArg.stringValue()) {
            case "second": period = 1;                      break;
            case "minute": period = 60;                     break;
            case "hour":   period = 60 * 60;                break;
            case "day":    period = 60 * 60 * 24;           break;
            case "month":  period = Historian.PERIOD_MONTH; break;
            case "year":   period = Historian.PERIOD_YEAR;  break;
            default: try { period = Integer.parseInt(periodArg.stringValue());} catch (NumberFormatException e) {throw new XDException(Errors.PARAM_VALUE_FORMAT, "The 'period' argument must be a number or a reserved word");}
        }
        PeriodMethod method;
        switch(methodArg.stringValue()) {
            case "org.ampii.fake": method = PeriodMethod.FAKE;           break;
            case "interpolation":  method = PeriodMethod.INTERPOLATION;  break;
            case "average":        method = PeriodMethod.AVERAGE;        break;
            case "minimum":        method = PeriodMethod.MINIMUM;        break;
            case "maximum":        method = PeriodMethod.MAXIMUM;        break;
            case "after":          method = PeriodMethod.AFTER;          break;
            case "before":         method = PeriodMethod.BEFORE;         break;
            case "closest":        method = PeriodMethod.CLOSEST;        break;
            case "default":        method = PeriodMethod.DEFAULT;        break;
            case "ending-average": method = PeriodMethod.ENDING_AVERAGE; break;
            case "ending-minimum": method = PeriodMethod.ENDING_MINIMUM; break;
            case "ending-maximum": method = PeriodMethod.ENDING_MAXIMUM; break;
            default: throw new XDException(Errors.PARAM_OUT_OF_RANGE,"The 'method' argument is not one of the allowed string values");
        }
        String results = Historian.computeHistoryPeriodic(target, start, period, count, method);
        if (results == null) throw new XDException(Errors.NO_HISTORY,target,"The data item does not has an associated history");
        return new StringData("results",results);
    }
}
