// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import static org.ampii.xd.data.Meta.*;
import static org.ampii.xd.common.Errors.*;
import org.ampii.xd.data.abstractions.AbstractCalendarData;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.definitions.Builtins;

import javax.xml.bind.DatatypeConverter;
import java.util.Calendar;

/**
 * The implementation of the Date base type. Most behavior is provided by {@link AbstractData}
 *
 * @author daverobin
 */
public class DateData extends AbstractCalendarData {

    public DateData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.DATE);
    }

    @Override public Base    getBase()  { return Base.DATE; }

    @Override public void    setValue(Object newValue) throws XDException {
        preread();
        if      (newValue instanceof Calendar)  value = (Calendar)((Calendar)newValue).clone();
        else if (newValue instanceof String) {
            if (newValue.equals("----/--/--")) { value = null; set(UNSPECIFIEDVALUE,true); }
            else try { value = DatatypeConverter.parseDate((String) newValue); delete(UNSPECIFIEDVALUE); }
            catch (IllegalArgumentException e) { throw new XDException(VALUE_FORMAT, this, "Invalid date format"); }
        }
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class");
        markDirty();
    }

    @Override public String  stringValue()  throws XDException {
        preread();
        return value == null || booleanValueOf(UNSPECIFIEDVALUE, false)? "----/--/--" : DatatypeConverter.printDate(value);
    }


}
