// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application.hooks;

import org.ampii.xd.bindings.Binding;
import org.ampii.xd.application.Bindings;
import org.ampii.xd.data.Data;

/**
 * This is a way to hook the mechanism in {@link Bindings} to add a new binding type to tie data to some kind of backend processing.
 * <p>
 * At the moment, this is based on a metadata string. This is bad because it is rather slow to do the lookup,
 * but it is good because nothing special is needed to persist it.
 * <p>
 * There is no automagical connection between classes wanting to provide bindings and the hard-coded lookup in {@link Bindings}
 * If you want to add an binding type without changing {@link Bindings}, create an {@link BindingHooks.External} interface and
 * inject it with {@link #registerExternal}
 *
 * @author daverobin
 */
public class BindingHooks {

    public interface External {
        Binding getBinding(Data data, String bindingName);
    }

    private static External external;   // outside code can add its own hooks

    public  static void registerExternal(External hooks)   { external = hooks; }

    public static Binding getBinding(Data data, String bindingName) {
        if (external != null) {
            Binding binding = external.getBinding(data, bindingName);
            if (binding != null) return binding;
        }
        return null;
    }




}
