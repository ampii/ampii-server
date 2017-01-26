// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.abstractions;

import org.ampii.xd.common.*;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.*;

/**
 * This is the abstract base for {@link PolyData} and {@link ParsedData}.
 **
 * @author drobin
 */
public class AbstractPolyData extends AbstractConstructedData {


    public AbstractPolyData(String name, Object... initializers) throws XDException { super(name, initializers); }

    // the base starts out as POLY but can be changed (not for externally visible use! POLY is not a *standard* base type)
    Base   base = Base.POLY;

    @Override public Base       getBase()                   { return base; }

    @Override public void       setBase(Base base)          { this.base = base; }

    @Override public boolean    isPoly()                    { return true; }   // needed since we can't tell by asking getBase() once it's been changed

    // all the primitive value stuff needs to be added to our AbstractConstructedData super

    Object value;

    @Override public boolean    canHaveValue()             { return true; }

    @Override public boolean    hasValue()                 throws XDException { return value != null; }

    @Override public Object     getValue()                 throws XDException { return value; }

    @Override public Object     getLocalValue()            { return value; }

    @Override public void       setLocalValue(Object newValue)  {
        // this is adapted from StringData because we usually just store POLY data as a string but we have to handle localization too
        if (newValue instanceof String)  { // we are given a simple string (probably!)
            value = newValue;
        }
        else if (newValue instanceof LocalizedStrings) { // we are given a complete set of locales
            value = new LocalizedStrings((LocalizedStrings)newValue); // make copy of what we are given so it's immutable
        }
        else if (newValue instanceof LocalizedString) { // we are given a single locale
            value = new LocalizedStrings(value,newValue); // this merges in new copy into any existing locales
        }
        else throw new XDError("setLocalValue() called with invalid object type",this,newValue);
    }

    @Override public void       setValue(Object newValue)  throws XDException {
        preread();
        setLocalValue(newValue);
    }

    @Override public String     stringValue()              throws XDException { preread(); return value == null? "<novalue>" : value.toString(); }
    @Override public boolean    booleanValue()             throws XDException { preread(); return value instanceof String && ((String)value).equals("true"); }
    @Override public int        intValue()                 throws XDException { preread(); return (int)longValue();     }
    @Override public long       longValue()                throws XDException { preread(); return parseLong(value, 0L); }
    @Override public float      floatValue()               throws XDException { preread(); return (float)doubleValue(); }
    @Override public double     doubleValue()              throws XDException { preread(); return parseDouble(value,0.0D); }
    @Override public byte[]     byteArrayValue()           throws XDException { preread(); return value instanceof byte[]? (byte[])value : new byte[0]; }
    @Override public StringSet  stringSetValue()           throws XDException { preread(); return value instanceof StringSet? (StringSet)value : new StringSet(); }

    private long parseLong(Object value, long defaultValue) {
        if (value == null) return defaultValue;
        try { return Long.parseLong(value.toString()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private double parseDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return defaultValue; }
    }


}
