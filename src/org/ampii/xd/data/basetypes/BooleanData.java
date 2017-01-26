// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.data.abstractions.AbstractPrimitiveData;
import org.ampii.xd.definitions.Builtins;

/**
 * The implementation of the Boolean base type. Most behavior is provided by {@link AbstractData}
 *
 * @author drobin
 */
public class BooleanData extends AbstractPrimitiveData {

    Boolean value;

    public BooleanData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.BOOLEAN);
    }

    @Override public Base    getBase()        { return Base.BOOLEAN; }

    @Override public boolean hasValue()       throws XDException { preread(); return value != null; }

    @Override public Object  getValue()       throws XDException { preread(); return value; }

    @Override public Object  getLocalValue()  { return value; }

    @Override public void setLocalValue(Object value)    {  // "local" means no high level validity/permissions checks.  but we can't let an invalid datatype in!
        if (value instanceof Boolean) this.value = (Boolean)value;
        else throw new XDError("setLocalValue() called with invalid object type",this,value);
    }

    @Override public void    setValue(Object newValue) throws XDException { // native java format is Boolean but accepts String
        preread();
        if      (newValue instanceof Boolean) value = (Boolean)newValue;
        else if (newValue instanceof String)  value = ((String)newValue).equals("true");
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class");
        markDirty();
    }

    @Override public String   stringValue()  throws XDException { preread(); return value != null? (value?"true":"false") : "<novalue>"; }

    @Override public boolean  booleanValue() throws XDException { preread(); return value != null? value : false; }

}
