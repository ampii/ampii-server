// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.abstractions;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.basetypes.*;

/**
 * This abstract class provides the common behavior for all primitive base types, like {@link StringData} and {@link UnsignedData}.
 * Keep in mind that things like {@link StringSetData} and {@link BitStringData} are composed of multiple string components,
 * and StringData can have multiple localized values, but they are all nonetheless considered "primitive" base types.
 *
 * @author daverobin
 */
public abstract class AbstractPrimitiveData extends AbstractData {

    public AbstractPrimitiveData(String name, Object... initializers) throws XDException { super(name, initializers); }

    @Override public boolean  canHaveValue()  { return true; }

}
