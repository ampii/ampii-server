// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application;

import org.ampii.xd.application.hooks.PolicyHooks;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.Data;
import org.ampii.xd.database.DataStore;

public class ApplicationPolicy implements Policy {

    // This is the Policy that is used by AbstractData if not overridden by a Binding for data item-based extensions. See also DefaultBindingPolicy.
    // This policy can also be overridden by PolicyHooks for path-based policy extensions.

    // See comment in PolicyHooks about methods to make application-wide or instance-specific extensions to this default policy.

    static ApplicationPolicy thePolicy = new ApplicationPolicy();

    private ApplicationPolicy() {} // private constructor to ensure only one static copy

    public static Policy getPolicy() { return thePolicy; }

    public boolean allowCreate(Data host, String name, String type, Base base) {
        Boolean result = PolicyHooks.allowCreate(host, name, type, base); // ask the applilcation hook if this can be created
        if (result != null) return result;
        return DataStore.getPolicy().allowCreate(host, name, type, base); // ask the datastore if it can be created
    }

    public boolean allowDelete(Data host, String name) {
        Boolean result = PolicyHooks.allowDelete(host,name); // ask the application hook if this can be deleted
        if (result != null) return result;
        return DataStore.getPolicy().allowDelete(host,name); // ask the datastore if it can be deleted
    }




}
