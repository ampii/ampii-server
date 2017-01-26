// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.abstractions.AbstractBinaryData;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.definitions.Builtins;

/**
 * The implementation of the Raw base type. Most behavior is provided by super classes and {@link AbstractData}
 *
 * @author drobin
 */
public class RawData extends AbstractBinaryData {

    public RawData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.RAW);
    }

    @Override public Base    getBase() { return Base.RAW; }

}
