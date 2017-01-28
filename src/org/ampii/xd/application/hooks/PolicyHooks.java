// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application.hooks;

import org.ampii.xd.data.Base;
import org.ampii.xd.data.Data;
import org.ampii.xd.application.ApplicationPolicy;
import org.ampii.xd.application.Policy;
import org.ampii.xd.bindings.DefaultBindingPolicy;

/**
 * This is *one* way to add custom policy handlers. Policy handlers determine what data or metadata can be created where.
 * <p>
 * The *default* policies defined in {@link ApplicationPolicy} and {@link DefaultBindingPolicy} are pretty minimal for
 * this codebase, in it's primary role as a simulator.
 * <p>
 * A real application should check where the data is located, who is doing it, or some other means to know what to allow.
 * If you want to add a custom policy handler without changing this core AMPII file, create a {@link PolicyHooks.External}
 * interface and inject it with {@link PolicyHooks#registerExternal}. Keep in mind that it will be called for every
 * {@link Data#createMetadata} or {@link Data#createChild}, so be efficient!
 * <p>
 * Alternatively, (and more appropriate in some cases) you can create return {@link Policy} objects from bindings because
 * the binding gets first crack at finding the policy.  This allows you to set individual policies, as fine grained as
 * bindings, without having to modify this system-wide policy.
 *
 * @author daverobin
 */
public class PolicyHooks {

    public interface External {
        Boolean allowCreate(Data target, String name, String type, Base base); // return null for default
        Boolean allowDelete(Data target, String name);                         // return null for default
    }

    private static External external;   // outside code can add its own hooks

    public  static void registerExternal(External hooks)   { external = hooks; }

    public static Boolean allowCreate(Data host, String name, String type, Base base) {
        if (external != null) {
            Boolean result = external.allowCreate(host,name,type,base);
            if (result != null) return result;
        }
        return null;
    }

    public static Boolean allowDelete(Data host, String name) {
        if (external != null) {
            Boolean result = external.allowDelete(host,name);
            if (result != null) return result;
        }
        return null;
    }

}
