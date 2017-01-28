// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.abstractions;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.basetypes.*;

/**
 * This abstract class provides the common behavior for the integer-based data {@link IntegerData} and {@link UnsignedData}.
 *
 * @author daverobin
 */
public abstract class AbstractWholeNumberData extends AbstractPrimitiveData {

    // basis for IntegerData and UnsignedData

    protected Long value;

    public AbstractWholeNumberData(String name, Object... initializers) throws XDException { super(name, initializers); }

    @Override public boolean hasValue()      throws XDException { preread(); return value != null; }

    @Override public Object  getValue()      throws XDException { preread(); return value; }

    @Override public Object  getLocalValue() { return value; }

    @Override public void setLocalValue(Object value)  {   // "no check" means no high level validity/permissions checks.  but we can't let an invalid datatype in!
        if (value instanceof Long) this.value = (Long)value;
        else if (value instanceof Integer) this.value = (long)((Integer)value);
        else throw new XDError("setLocalValue() called with invalid object type",this,value);
    }

    @Override public void    setValue(Object newValue) throws XDException { // native java format is Long but accepts String
        preread();
        Long tempValue;
        if      (newValue instanceof Long)    tempValue = (Long)newValue;
        else if (newValue instanceof Integer) tempValue = ((Integer)newValue).longValue();
        else if (newValue instanceof String) {
            try { tempValue = Long.parseLong((String)newValue); }
            catch (NumberFormatException e) { throw new XDException(Errors.VALUE_FORMAT, this,"Invalid number format", newValue); }
        }
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class");
        if (getBase() == Base.UNSIGNED && tempValue < 0) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Unsigned data cannot be assigned a negative value");
        long minimum = longValueOf(Meta.MINIMUM, Long.MIN_VALUE);
        long maximum = longValueOf(Meta.MAXIMUM, Long.MAX_VALUE);
        if (tempValue < minimum) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value of " + tempValue + " exceeds minimum of " + minimum);
        if (tempValue > maximum) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value of " + tempValue + " exceeds maximum of " + maximum);
        value = tempValue;
        markDirty();
    }

    @Override public String  stringValue()  throws XDException { preread(); return value != null? value.toString() : "<novalue>"; }

    @Override public boolean booleanValue() throws XDException { preread(); return value != null && value != 0; }

    @Override public int     intValue()     throws XDException { preread(); return value != null? value.intValue() : 0; }

    @Override public long    longValue()    throws XDException { preread(); return value != null? value : 0L; }

    @Override public float   floatValue()   throws XDException { preread(); return value != null? value.floatValue() : 0.0F; }

    @Override public double  doubleValue()  throws XDException { preread(); return value != null? value.doubleValue() : 0.0; }

}
