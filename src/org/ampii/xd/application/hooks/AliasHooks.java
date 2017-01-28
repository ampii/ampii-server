// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application.hooks;

import org.ampii.xd.data.Data;
import org.ampii.xd.resolver.Alias;

/**
 * This is a way to add extensions to the {@link Alias} default behavior for URI aliases, like .blt, $history, etc.
 * <p>
 * Aliases are things that do not exist as data items at the location named in the URI. They are internal "redirections".
 * <p>
 * If you want to add an alias without changing this core AMPII file, create an {@link AliasHooks.External} interface and
 * inject it with {@link #registerExternal}
 *
 * @author daverobin
 */
public class AliasHooks {

    public interface External {
        Data resolve(Data data, String name);
    }

    private static External external; // outside code can provide custom aliases here

    public static void registerExternal(External hooks)   { external = hooks; }

    public static Data resolve(Data data, String name)  {
        if (external != null) return external.resolve(data, name);
        return null;
    }

}
