// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.functions;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.BooleanData;
import org.ampii.xd.data.Context;
import org.ampii.xd.server.Server;

/**
 * An Implementation of the .../remote() URI function, called from {@link Functions}
 */
public class RemoteFunction extends Functions {

    static Data  invoke(Data target, String argString, Context context) throws XDException {
        // remote() is defined as returns false if data is on local server, otherwise true.
        return new BooleanData("results", !Server.isOurData(target.stringValue()));
    }

}
