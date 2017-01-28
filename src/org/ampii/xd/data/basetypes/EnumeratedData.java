// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.data.abstractions.AbstractTextData;
import org.ampii.xd.definitions.Builtins;

/**
 * The implementation of the Enumerated base type.
 * This handles value checking but most other behavior is provided by {@link AbstractData}
 *
 * @author daverobin
 */
public class EnumeratedData extends AbstractTextData {

    public EnumeratedData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.ENUMERATED);
    }

    @Override public Base getBase()    { return Base.ENUMERATED; }

    @Override public void setValue(Object newValue) throws XDException {
        preread();
        String tempValue;
        if (newValue instanceof String) tempValue = (String) newValue;
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class");
        Data namedValues = findEffective(Meta.NAMEDVALUES);
        if (namedValues != null) {
            if (namedValues.find(tempValue) == null) { // named value not found, let's see if it's a number
                try {
                    int i = Integer.parseInt(tempValue);
                    if (i < longValueOf(Meta.MINIMUM, Long.MIN_VALUE))
                        throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value exceeds minimum");
                    if (i > longValueOf(Meta.MAXIMUM, Long.MAX_VALUE))
                        throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Value exceeds maximum");
                } catch (NumberFormatException e) {
                    throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Enumeration value '" + tempValue + "' not a number and not found in $namedValues");
                }
            }
        }
        value = tempValue;
        markDirty();
    }
}