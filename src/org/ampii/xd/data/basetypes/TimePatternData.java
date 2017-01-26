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
 * The implementation of the TimeData base type.
 * This parses the value string but most other behavior is provided by super classes and {@link AbstractData}
 *
 * @author drobin
 */
public class TimePatternData extends AbstractPatternData {

    public TimePatternData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.TIMEPATTERN);
    }

    @Override public Base    getBase()  { return Base.TIMEPATTERN; }

    @Override public void    setValue(Object newValue) throws XDException {
        preread();
        if (newValue instanceof int[]) {
            if (((int[])newValue).length == 4) value = ((int[])newValue).clone();
            else throw new XDError(this, "setValue() is given an incorrect length");
        }
        else if (newValue instanceof String) value = parseTimePattern(this,(String)newValue);
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class or length");
        markDirty();
    }

    @Override public String stringValue() throws XDException {
        preread();
        if (value == null) return "*:*:*.*";
        if (value.length != 4) throw new XDError("value has incorrect length ("+value.length+")", this, value);
        return  (value[0] == 255? "*" : String.format("%02d", value[0])) + ":" +
                (value[1] == 255? "*" : String.format("%02d", value[1])) + ":" +
                (value[2] == 255? "*" : String.format("%02d", value[2])) + "." +
                (value[3] == 255? "*" : String.format("%02d", value[3]));
    }

    public static int[]  parseTimePattern(Data target, String s) throws XDException {
        int[] results = new int[4];
        String[] hmsh  = s.split("[:\\.]");
        if (hmsh.length != 4) throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '"+s+"': time does not have four components");
        try {
            if (hmsh[0].equals("*")) results[0] = 255;
            else {
                results[0] = Integer.parseInt(hmsh[0]);  // "hh"
                if (results[0] < 0 || results[0] > 24) throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '" + s + "': hours part is out of bounds");
            }
            if (hmsh[1].equals("*")) results[1] = 255;
            else {
                results[1] = Integer.parseInt(hmsh[1]);  // "mm"
                if (results[1] < 0 || results[1] > 59) throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '" + s + "': minutes part is out of bounds");
            }
            if (hmsh[2].equals("*")) results[2] = 255;
            else {
                results[2] = Integer.parseInt(hmsh[2]);  // "ss"
                if (results[2] < 0 || results[2] > 59) throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '" + s + "': seconds part is out of bounds");
            }
            if (hmsh[3].equals("*")) results[3] = 255;
            else {
                results[3] = Integer.parseInt(hmsh[3]);    // "nn"
                if (results[3] < 0 || results[3] > 99) throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '" + s + "': hundredths part is out of bounds");
            }
        } catch (NumberFormatException e) { throw new XDException(Errors.VALUE_FORMAT, target, "Invalid value '"+s+"': contains bad number format"); }
        return results;
    }


}
