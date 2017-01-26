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
 * The implementation of the DatePattern base type. This handles parsing but most other behavior is provided by {@link AbstractData}
 *
 * @author drobin
 */
public class DatePatternData extends AbstractPatternData {

    public DatePatternData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.DATEPATTERN);
    }

    @Override public Base    getBase()  { return Base.DATEPATTERN; }

    @Override public void    setValue(Object newValue) throws XDException {
        preread();
        if (newValue instanceof int[]) {
            if (((int[])newValue).length == 4) value = ((int[])newValue).clone();
            else throw new XDError(this, "setValue() is given an incorrect length");
        }
        else if (newValue instanceof String) value = parseDatePattern(this,(String)newValue);
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class or length");
        markDirty();
    }

    @Override public String stringValue()  throws XDException {
        preread();
        if (value == null) return "*-*-* *";
        if (value.length != 4) throw new XDError(this, "value has incorrect length ("+value.length+")");
        return  (value[0] == 255? "*" : String.format("%04d", value[0])) + "-" +
                (value[1] == 255? "*" : String.format("%02d", value[1])) + "-" +
                (value[2] == 255? "*" : String.format("%02d", value[2])) + " " +
                (value[3] == 255? "*" : String.format("%d",   value[3]));
    }

    public static int[] parseDatePattern(Data target, String s) throws XDException {
        // formats are "Y-M-D" or "Y-M-D W"
        int[] results = new int[4];
        String[] parts = s.split(" ");
        String[] ymd = parts[0].split("-");
        String w = parts.length == 2 ? parts[1] : "*";
        if (ymd.length != 3) throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '" + s + "': date does not have three parts");
        try {
            if (ymd[0].equals("*")) results[0] = 255;
            else {
                results[0] = Integer.parseInt(ymd[0]);  // "YYYY"
                if (results[0] < 1900 || results[0] > 1900 + 255) throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '" + s + "': year part is out of bounds");
            }
            if (ymd[1].equals("*")) results[1] = 255;
            else {
                results[1] = Integer.parseInt(ymd[0]);  // "MM"
                if (results[1] < 0 || results[1] > 14) throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '" + s + "': month part is out of bounds");
            }
            if (ymd[2].equals("*")) results[2] = 255;
            else {
                results[2] = Integer.parseInt(ymd[2]);  // "DD"
                if (results[2] < 0 || results[2] > 34) throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '" + s + "': day-of-month part is out of bounds");
            }
            if (w.equals("*")) results[3] = 255;
            else {
                results[3] = Integer.parseInt(w);       // "W"
                if (results[3] < 1 || results[3] > 7) throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '" + s + "': day-of-week part is out of bounds");
            }
        } catch (NumberFormatException e) {throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '" + s + "': contains bad number format");}
        return results;
    }


}
