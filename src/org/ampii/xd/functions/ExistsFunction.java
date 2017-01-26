// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.functions;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.BooleanData;
import org.ampii.xd.data.basetypes.StringData;
import org.ampii.xd.data.Context;
import org.ampii.xd.resolver.Eval;

/**
 * An Implementation of the .../exists() URI function, called from {@link Functions}
 */
public class ExistsFunction extends Functions {
    // called from Function.java
    static Data  invoke(Data target, String argString, Context context) throws XDException {
        Data path    = new StringData("path");
        Data results = new BooleanData("results");
        parseArgs(argString, path);
        boolean found = false;
        try { found = Eval.eval(target, path.stringValue(), Eval.FOR_EXISTS) != null; } catch (XDException e) {}
        results.setValue(found);
        return results;
    }

}
