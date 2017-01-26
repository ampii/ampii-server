// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.functions;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.BooleanData;
import org.ampii.xd.data.basetypes.StringData;

/**
 * An Implementation of the .../endsWith() URI function, called from {@link Functions}
 */
public class EndsWithFunction extends Functions {

    static Data invoke(Data target, String argString) throws XDException {
        Data match   = new StringData("match");
        Data results = new BooleanData("results");
        parseArgs(argString, match);
        String compareTo = match.stringValue();
        if (target.canHaveValue()) results.setValue(target.stringValue().endsWith(compareTo));
        else throw new XDException(Errors.FUNCTION_TARGET,target,"Invalid target base type for function");
        return results;
    }
}
