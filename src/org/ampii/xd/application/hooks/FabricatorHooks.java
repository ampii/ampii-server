// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application.hooks;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.resolver.Fabricator;

/**
 * This is a way to add extensions to the {@link Fabricator} default behavior for ephemeral responses, like $children,
 * that are created on the fly.
 * <p>
 * If you want to add a custom fabricator without changing the core AMPII files, create a {@link FabricatorHooks.External}
 * interface and inject it with {@link FabricatorHooks#registerExternal}.
 *
 * @author daverobin
 */
public class FabricatorHooks {

    public interface External {
        Data fabricate(Data data, String name) throws XDException;
    }

    private static External external; // outside code can provide custom fabrication here

    public static void registerExternal(External hooks)   { external = hooks; }

    public static Data fabricate(Data data, String name) throws XDException {
        if (external != null) return external.fabricate(data, name);
        return null;
    }
}
