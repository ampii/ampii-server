// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application.hooks;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Context;
import org.ampii.xd.functions.Functions;

/**
 * This is a way to add extensions to the {@link Functions} handler for adding URI functions, like startWith(...).
 * <p>
 * If you want to add a custom fundtion without changing the core AMPII files, create a {@link FunctionHooks.External}
 * interface and inject it with {@link FunctionHooks#registerExternal}.
 *
 * @author drobin
 */
public class FunctionHooks {

    public interface External {
        Data invoke(Data target, String function, String argString, Context context) throws XDException; // return non-null if handled
    }

    private static External external;  // outside code can add its own hooks.

    public static void registerExternal(External hooks) { external = hooks; }

    // returns null if function was not handled
    public static Data invoke(Data target, String function, String argString, Context context) throws XDException {
        if (external != null) return external.invoke(target,function,argString,context);
        return null;
    }

}
