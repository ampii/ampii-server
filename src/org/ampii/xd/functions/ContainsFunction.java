// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.functions;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.basetypes.BooleanData;
import org.ampii.xd.data.basetypes.StringData;

/**
 * An Implementation of the .../contains() URI function, called from {@link Functions}
 */
public class ContainsFunction extends Functions {

    static Data invoke(Data target, String argString) throws XDException {
        Data match   = new StringData("match");
        Data results = new BooleanData("results");
        parseArgs(argString, match);
        String compareTo = match.stringValue();
        switch (target.getBase()) {
            case STRING:
                results.setValue(target.stringValue().contains(compareTo));
                break;
            case STRINGSET:
            case BITSTRING:
                results.setValue(target.stringSetValue().containsComponent(compareTo));
                break;
            default:
                results.setValue(false);
        }
        return results;
    }
}
