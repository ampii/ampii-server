// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.data.abstractions.AbstractTextData;
import org.ampii.xd.definitions.Builtins;

/**
 * The implementation of the ObjectIdentifier base type.
 * This provides value parsing but most other behavior is provided by super classes and {@link AbstractData}
 *
 * @author daverobin
 */
public class ObjectIdentifierData extends AbstractTextData {

    public ObjectIdentifierData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.OBJECTIDENTIFIER);
    }

    @Override public Base    getBase()  { return Base.OBJECTIDENTIFIER; }

    @Override public void    setValue(Object newValue) throws XDException {
        preread();
        // native java format is a String with two comma separated parts
        if (newValue instanceof String) {
            String[] parts = ((String) newValue).split(",");
            if (parts.length != 2) throw new XDException(Errors.VALUE_FORMAT, this, "Value '" + newValue + "' does not contain two comma-separated parts");
            try {
                int i = Integer.parseInt(parts[1]);
                if (i < 0 || i > 4197303) throw new XDException(Errors.VALUE_FORMAT, this, "Instance part of '" + newValue + "' is out of bounds");
            } catch (NumberFormatException e) {throw new XDException(Errors.VALUE_FORMAT, this, "Instance part of '" + newValue + "' is not a number");}
            // TODO validate type name
            value = (String)newValue;
        }
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class");
        markDirty();
    }

}
