// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.abstractions;

import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.basetypes.*;

/**
 * This abstract class provides the common behavior for data that is based on string patterns that are parsed into a set of numbers, like {@link DatePatternData} and {@link WeekNDayData}.
 *
 * @author drobin
 */
public abstract class AbstractPatternData extends AbstractPrimitiveData {

    protected int[] value;

    public AbstractPatternData(String name, Object... initializers) throws XDException { super(name, initializers); }

    @Override public boolean  hasValue()      throws XDException { preread(); return value != null; }

    @Override public Object   getValue()      throws XDException { preread(); return value != null? value.clone() : null; }

    @Override public Object   getLocalValue() { return value != null? value.clone() : null; }

    @Override public void     setLocalValue(Object value)  {
        if (value instanceof int[]) this.value = (int[])value;
        else throw new XDError("setLocalValue() called with invalid object type",this,value);
    }

    @Override public boolean  booleanValue()  throws XDException { preread(); return value != null; }

    // subclasses handle setValue() and stringValue()


}
