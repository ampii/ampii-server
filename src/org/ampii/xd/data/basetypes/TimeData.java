// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.abstractions.AbstractCalendarData;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.definitions.Builtins;

import javax.xml.bind.DatatypeConverter;
import java.util.Calendar;

/**
 * The implementation of the TimeData base type.
 * This checks or special string values but most other behavior is provided by super classes and {@link AbstractData}
 *
 * @author daverobin
 */
public class TimeData extends AbstractCalendarData {

    public TimeData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.TIME);
    }

    @Override public Base    getBase()  { return Base.TIME; }

    @Override public void    setValue(Object newValue) throws XDException {
        preread();
        if      (newValue instanceof Calendar)  value = (Calendar)((Calendar)newValue).clone();
        else if (newValue instanceof String) {
            if (newValue.equals("--:--:--")) { value = null; set(Meta.UNSPECIFIEDVALUE, true); }
            else try { value = DatatypeConverter.parseTime((String)newValue); delete(Meta.UNSPECIFIEDVALUE); }
            catch (IllegalArgumentException e) { throw new XDException(Errors.VALUE_FORMAT, this, "Invalid time format"); }
        }
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class");
        markDirty();
    }

    @Override public String  stringValue() throws XDException {
        preread();
        return value == null || booleanValueOf(Meta.UNSPECIFIEDVALUE, false) ? "--:--:--" : DatatypeConverter.printTime(value);
    }

}
