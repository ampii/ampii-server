// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.abstractions;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.*;

/**
 * This abstract class provides the common behavior for floating point base types {@link RealData} and {@link DoubleData}.
 *
 * @author daverobin
 */
public abstract class AbstractFloatingPointData extends AbstractPrimitiveData {

    protected Double value;

    public AbstractFloatingPointData(String name, Object... initializers) throws XDException { super(name, initializers); }

    @Override public boolean hasValue()       throws XDException { preread(); return value != null; }

    @Override public Object  getValue()       throws XDException { preread(); return value; }

    @Override public Object  getLocalValue()  { return value; }

    @Override public void setLocalValue(Object value) {
        if (value instanceof Double) this.value = (Double)value;
        else throw new XDError("setLocalValue() called with invalid object type",this,value);
    }

    @Override public void    setValue(Object newValue) throws XDException { // native java format is Long but accepts String
        preread();
        Double tempValue;
        if      (newValue instanceof Double)   tempValue = (Double)newValue;
        else if (newValue instanceof Float)    tempValue = ((Float)newValue).doubleValue();
        else if (newValue instanceof String) {
            try { tempValue = Double.parseDouble((String)newValue); }
            catch (NumberFormatException e) { throw new XDException(Errors.VALUE_FORMAT, this,"Invalid number format"); }
        }
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class");
        double minimum = doubleValueOf(Meta.MINIMUM, Double.NEGATIVE_INFINITY);
        double maximum = doubleValueOf(Meta.MAXIMUM, Double.POSITIVE_INFINITY);
        if (tempValue < minimum) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value of " + tempValue + "exceeds minimum of " + minimum);
        if (tempValue > maximum) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value of " + tempValue + "exceeds maximum of " + maximum);
        value = tempValue;
        markDirty();
    }

    @Override public String  stringValue()  throws XDException { preread(); return value != null? value.toString() : "<novalue>"; }

    @Override public boolean booleanValue() throws XDException { preread();  return value != null && value != 0.0D; }

    @Override public double  doubleValue()  throws XDException { preread();  return value != null? value : 0.0D; }

    @Override public float   floatValue()   throws XDException { preread();  return value != null? value.floatValue() : 0.0F; }

    @Override public int     intValue()     throws XDException { preread();  return value != null? value.intValue() : 0; }

    @Override public long    longValue()    throws XDException { preread();  return value != null? value.longValue() : 0; }

}
