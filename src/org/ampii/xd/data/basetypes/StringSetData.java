// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.data.abstractions.AbstractStringSetData;
import org.ampii.xd.definitions.Builtins;

/**
 * The implementation of the StringSet base type. Most behavior is provided by super classes and {@link AbstractData}
 *
 * @author daverobin
 */
public class StringSetData extends AbstractStringSetData {

    public StringSetData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.STRINGSET);
    }

    @Override public Base    getBase() { return Base.STRINGSET; }

}
