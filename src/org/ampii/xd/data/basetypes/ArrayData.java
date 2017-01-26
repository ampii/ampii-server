// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.abstractions.AbstractConstructedData;
import org.ampii.xd.definitions.Builtins;
import org.ampii.xd.data.abstractions.AbstractData;

/**
 * The implementation of the Array base type. Most behavior is provided by {@link AbstractData}
 *
 * @author drobin
 */
public class ArrayData extends AbstractConstructedData {

    public ArrayData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.ARRAY);
    }

    @Override public Base getBase() { return Base.ARRAY; }

}
