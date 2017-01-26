// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.abstractions;

import org.ampii.xd.common.*;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.basetypes.*;

/**
 * This abstract class provides the common behavior for the text-based data {@link StringData}, {@link LinkData}, and {@link EnumeratedData}.
 *
 * @author drobin
 */
public abstract class AbstractTextData extends AbstractPrimitiveData {

    protected Object value; // this is declared as an "Object" because StringData can replace it with LocalizedStrings

    public AbstractTextData(String name, Object... initializers) throws XDException { super(name, initializers); }

    @Override public boolean hasValue()      throws XDException { preread(); return value != null; }

    @Override public Object  getValue()      throws XDException { preread(); return value; }

    @Override public Object  getLocalValue() { return value; }

    @Override public void    setLocalValue(Object value)  {   // "no check" means no high level validity/permissions checks.  but we can't let an invalid datatype in!
        if (value instanceof String || value == null) this.value = value;  // StringData will override this to allow LocalizedStrings
        else throw new XDError("setLocalValue() called with invalid object type",this,value);
    }

    @Override public void    setValue(Object newValue) throws XDException {
        // this abstract string class accepts only single String value, everything else is an error of some kind
        // the StringData subclass overrides this and will also accept localized strings
        if  (newValue instanceof String)  {
            validateLength(((String)newValue).length());
            value = newValue;
        }
        else if (newValue instanceof LocalizedStrings || newValue instanceof LocalizedString) throw new XDError(this, "setValue() was given localized value(s) for non-localizable data");
        else if (newValue == null)  throw new XDError(this,"setValue() is given a null");
        else throw new XDError(this,"setValue() is given an unknown class");
        markDirty();
    }

    @Override public String  stringValue()              throws XDException { preread(); return value instanceof String? (String)value : ""; }

    @Override public boolean booleanValue()             throws XDException { preread(); return value instanceof String && !((String)value).isEmpty(); }

    // this is also used by StringData subclass too to validate each localized value
    protected void validateLength(int length)  throws XDException {
        if (length < longValueOf(Meta.MINIMUMLENGTH, 0L)) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value violates minimum length for writing");
        if (length < longValueOf(Meta.MINIMUMLENGTHFORWRITING, 0L)) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value violates minimum length");
        if (length > longValueOf(Meta.MAXIMUMLENGTH, Long.MAX_VALUE)) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value exceeds maximum length");
        if (length > longValueOf(Meta.MAXIMUMLENGTHFORWRITING, Long.MAX_VALUE)) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value exceeds maximum length for writing");
    }
}

