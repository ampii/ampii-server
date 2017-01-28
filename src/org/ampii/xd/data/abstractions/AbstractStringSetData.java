// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.abstractions;

import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.common.StringSet;
import org.ampii.xd.data.basetypes.*;

/**
 * This abstract class provides the common behavior for the StringSet-based {@link StringSetData} and {@link BitStringData}.
 *
 * @author daverobin
 */
public abstract class AbstractStringSetData extends AbstractPrimitiveData {

    protected StringSet value;

    public AbstractStringSetData(String name, Object... initializers) throws XDException { super(name, initializers); }

    @Override public boolean hasValue()       throws XDException { preread(); return value != null; }

    @Override public Object  getValue()       throws XDException { preread(); return value != null? new StringSet(value) : null; }

    @Override public Object  getLocalValue()  { return value != null? new StringSet(value) : null; }

    @Override public void    setLocalValue(Object value)  {   // "no check" means no high level validity/permissions checks.  but we can't let an invalid datatype in!
        if (value instanceof StringSet) this.value = (StringSet)value;
        else throw new XDError("setLocalValue() called with invalid object type",this,value);
    }

    @Override public void    setValue(Object newValue) throws XDException { // native java format is StringSet but accepts String
        preread();
        if      (newValue instanceof StringSet) value = new StringSet((StringSet)newValue);
        else if (newValue instanceof String)    value = new StringSet((String)newValue);
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class");
        markDirty();
    }

    @Override public String  stringValue()  throws XDException { preread();  return value != null? value.toString() : ""; }

    @Override public boolean booleanValue() throws XDException { preread();  return value != null && value.size() != 0; }


}
