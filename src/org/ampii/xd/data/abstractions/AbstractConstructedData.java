// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.abstractions;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.basetypes.*;

/**
 * This abstract class provides the common behavior for constructed data types like {@link CollectionData}, {@link ObjectData}, etc.
 *
 * @author daverobin
 */
public abstract class AbstractConstructedData extends AbstractData {

    public AbstractConstructedData(String name, Object... initializers) throws XDException { super(name, initializers); }

    @Override public boolean   canHaveChildren() { return true; }

    @Override public boolean   booleanValue()    throws XDException { preread(); return getCount() != 0; } // every class overrides booleanValue for filter evaluation

}
