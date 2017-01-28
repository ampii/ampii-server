// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.managers;

import org.ampii.xd.bindings.Binding;
import org.ampii.xd.bindings.DefaultBinding;
import org.ampii.xd.common.Log;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Meta;


/**
 * Manages the "/callback" (default name) location for posted subscription callbacks.
 * <p>
 * This really doesn't do anything yet... or ever? Most of this would be done with a real custom binding by a real
 * application, not just a simulator/tester. In fact, the subscription tests actually provide their <b>own</b> callback
 * target, not this default "/callback"
 *
 * @author daverobin
 */
public class CallbackManager {

    private static Binding theBinding = new DefaultBinding() {
        @Override public Data prepost(Data target, Data data) throws XDException  { return CallbackManager.prepost(data); }
    };

    public static Binding getBinding()      { return theBinding; }

    private static Data prepost(Data data) throws XDException {
        Log.logInfo("Callback received for " + data.stringValueOf(Meta.SUBSCRIPTION, "{unknown}"));
        //
        // TODO What should we do for a simulator other than log and allow the data to be posted?
        //
        return data;
    }

}
