// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.data.abstractions.AbstractPatternData;
import org.ampii.xd.definitions.Builtins;

/**
 * The implementation of the DateTimePattern base type. This handles parsing but most other behavior is provided by {@link AbstractData}
 *
 * @author daverobin
 */
public class DateTimePatternData extends AbstractPatternData {

    public DateTimePatternData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.DATETIMEPATTERN);
    }

    @Override public Base    getBase()  { return Base.DATETIMEPATTERN; }

    @Override public void    setValue(Object newValue) throws XDException {
        preread();
        if (newValue instanceof int[]) {
            if (((int[])newValue).length == 8) value = ((int[])newValue).clone();
            else throw new XDError("setValue() is given an incorrect length", this, newValue);
        }
        else if (newValue instanceof String) value = parseDateTimePattern(this,(String)newValue);
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class or length");
        markDirty();
    }

    @Override public String stringValue()  throws XDException {
        preread();
        if (value == null) return "*-*-* * *:*:*.*";
        if (value.length != 8) throw new XDError(this, "value has incorrect length ("+value.length+")");
        return  (value[0] == 255? "*" : String.format("%04d", value[0])) + "-" +
                (value[1] == 255? "*" : String.format("%02d", value[1])) + "-" +
                (value[2] == 255? "*" : String.format("%02d", value[2])) + " " +
                (value[3] == 255? "*" : String.format("%d",   value[3])) + " " +
                (value[4] == 255? "*" : String.format("%02d", value[4])) + ":" +
                (value[5] == 255? "*" : String.format("%02d", value[5])) + ":" +
                (value[6] == 255? "*" : String.format("%02d", value[6])) + "." +
                (value[7] == 255? "*" : String.format("%02d", value[7]));
    }

    public static int[]  parseDateTimePattern(Data target, String s) throws XDException {
        // formats are "YYYY-MM-DD hh:mm:ss.nn" or "YYYY-MM-DD W hh:mm:ss.nn
        int[] results = new int[8];
        int lastSpace = s.lastIndexOf(' ');
        if (lastSpace == -1) throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '" + s + "': does not contain a separating space");
        String datePart = s.substring(0, lastSpace);
        String timePart = s.substring(lastSpace+1);
        int[] dateResults = DatePatternData.parseDatePattern(target,datePart);
        int[] timeResults = TimePatternData.parseTimePattern(target,timePart);
        results[0] = dateResults[0];
        results[1] = dateResults[1];
        results[2] = dateResults[2];
        results[3] = dateResults[3];
        results[4] = timeResults[0];
        results[5] = timeResults[1];
        results[6] = timeResults[2];
        results[7] = timeResults[3];
        return results;
    }


}
