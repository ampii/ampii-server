// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.abstractions;

import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.basetypes.*;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * This abstract class provides the common behavior for time-based data like {@link DateData}, {@link TimeData}, etc.
 *
 * @author daverobin
 */
public abstract class AbstractCalendarData extends AbstractPrimitiveData {

    protected Calendar value;

    public AbstractCalendarData(String name, Object... initializers) throws XDException { super(name, initializers); }

    @Override public boolean hasValue()      throws XDException { preread(); return value != null; }

    @Override public Object  getValue()      throws XDException { preread(); return value != null? value.clone() : null; }

    @Override public Object  getLocalValue() { return value != null? value.clone() : null; }

    @Override public void    setLocalValue(Object value)  {   // "no check" means no high level validity/permissions checks.  but we can't let an invalid datatype in!
        if (value instanceof Calendar) this.value = (Calendar)value;
        else throw new XDError("setLocalValue() called with invalid object type",this,value);
    }

    @Override public boolean  booleanValue()  throws XDException { preread(); return value != null; }

    @Override public Calendar calendarValue() throws XDException { preread(); return value != null ? (Calendar)value.clone() : new GregorianCalendar(); }

    // subclasses handle setValue() and stringValue()

}
