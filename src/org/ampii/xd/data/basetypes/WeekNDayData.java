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
 * The implementation of the WeekNDayData base type.
 * This parses the value string but most other behavior is provided by super classes and {@link AbstractData}
 *
 * @author daverobin
 */
public class WeekNDayData  extends AbstractPatternData {

    public WeekNDayData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.WEEKNDAY);
    }

    @Override public Base    getBase()  { return Base.WEEKNDAY; }

    @Override public void    setValue(Object newValue) throws XDException {
        preread();
        if (newValue instanceof int[]) {
            if (((int[])newValue).length == 3) value = ((int[])newValue).clone();
            else throw new XDError(this, "setValue() is given an incorrect length");
        }
        else if (newValue instanceof String) value = parseWeekNDay(this,(String)newValue);
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class or length");
        markDirty();
    }

    @Override public String stringValue()  throws XDException {
        preread();
        if (value == null) return "*,*,*";
        if (value.length != 3) throw new XDError(this, "value has incorrect length ("+value.length+")");
        return  (value[0] == 255? "*" : String.format("%02d", value[0])) + "," +
                (value[1] == 255? "*" : String.format("%02d", value[1])) + "," +
                (value[2] == 255? "*" : String.format("%02d", value[2]));
    }

    public static int[]  parseWeekNDay(Data target, String s) throws XDException {
        int[] results = new int[3];
        String[] mwd = s.split(",");  // split into month-week#-dow
        if (mwd.length != 3) throw new XDException(Errors.VALUE_FORMAT, target, "Value '" + s + "' does not contain three comma-separated parts");
        try {
            if (mwd[0].equals("*")) results[0] = 255;
            else {
                results[0] = Integer.parseInt(mwd[0]);
                if (results[0] < 1 || results[0] > 14) throw new XDException(Errors.VALUE_FORMAT, target, "Month part of '" + s + "' is out of bounds");
            }
            if (mwd[1].equals("*")) results[1] = 255;
            else {
                results[1] = Integer.parseInt(mwd[1]);
                if (results[1] < 1 || results[1] > 9) throw new XDException(Errors.VALUE_FORMAT, target, "Week part of '" + s + "' is out of bounds");
            }
            if (mwd[2].equals("*")) results[2] = 255;
            else {
                results[2] = Integer.parseInt(mwd[2]);
                if (results[2] < 1 || results[2] > 7) throw new XDException(Errors.VALUE_FORMAT, target, "Day-of-week part of '" + s + "' is out of bounds");
            }
        } catch (NumberFormatException e) {throw new XDException(Errors.VALUE_FORMAT, target, "Value '" + s + "' contains invalid number");}
        return results;
    }

}
