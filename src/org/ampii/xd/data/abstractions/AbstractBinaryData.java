// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.abstractions;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.basetypes.*;
import javax.xml.bind.DatatypeConverter;

/**
 * This abstract class provides the common behavior for {@link OctetStringData} and {@link RawData}.
 *
 * @author daverobin
 */
public abstract class AbstractBinaryData extends AbstractPrimitiveData {

    protected byte[] value;

    public AbstractBinaryData(String name, Object... initializers) throws XDException { super(name, initializers); }

    @Override public boolean hasValue()        throws XDException { preread(); return value != null;}

    @Override public Object  getValue()        throws XDException { preread(); return value; }

    @Override public Object  getLocalValue()   { return value; }

    @Override public void    setLocalValue(Object value)  {   // "no check" means no high level validity/permissions checks.  but we can't let an invalid datatype in!
        if (value instanceof byte[]) this.value = (byte[])value;
        else throw new XDError("setLocalValue() called with invalid object type",this,value);
    }

    @Override public void    setValue(Object newValue) throws XDException {
        preread();
        byte[] tempValue;
        if      (newValue instanceof byte[])   tempValue = ((byte[])newValue).clone();
        else if (newValue instanceof String) {
            try { tempValue = DatatypeConverter.parseHexBinary((String) newValue); }
            catch (IllegalArgumentException e) { throw new XDException(Errors.VALUE_FORMAT, this, "Invalid hex format"); }
        }
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class");
        validateLength(tempValue.length); // validateLength() will be overridden by OctetString for merged length calculation
        value = tempValue;
        markDirty();
    }

    protected void validateLength(int length) throws XDException { // this is overridden by OctetString for validating merged length
        if (length < longValueOf(Meta.MINIMUMLENGTHFORWRITING, 0L)) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value exceeds minimum for writing");
        if (length < longValueOf(Meta.MINIMUMLENGTH, 0L)) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value exceeds minimum");
        if (length > longValueOf(Meta.MAXIMUMLENGTH, Long.MAX_VALUE)) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value exceeds maximum for writing");
        if (length > longValueOf(Meta.MAXIMUMLENGTHFORWRITING, Long.MAX_VALUE)) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value exceeds maximum");
    }

    @Override public boolean booleanValue()   throws XDException { preread(); return value != null && ((byte[])value).length != 0; }

    @Override public String  stringValue()    throws XDException { preread(); return value != null? DatatypeConverter.printHexBinary(value) : ""; }

    @Override public byte[]  byteArrayValue() throws XDException { preread(); return value != null? ((byte[])value).clone() : new byte[0]; }


}

