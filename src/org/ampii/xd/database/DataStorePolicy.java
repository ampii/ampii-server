// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.database;

import org.ampii.xd.data.Base;
import org.ampii.xd.data.Data;
import org.ampii.xd.application.Policy;
import org.ampii.xd.application.hooks.PolicyHooks;

/**
 * The default {@link Policy} for items in the datastore.
 * See comment in {@link PolicyHooks} about methods to make application-wide or instance-specific extensions to this
 * boring default policy.
 *
 * @author drobin
 */
public class DataStorePolicy implements Policy {

    private static DataStorePolicy thePolicy = new DataStorePolicy();

    private DataStorePolicy() {} // no public constructor - only one static copy is made

    public  static Policy getThePolicy() { return thePolicy; }

    public boolean allowCreate(Data host, String name, String type, Base base) {
        return true; // the datastore stores anything by default
    }

    public boolean allowDelete(Data host, String name) {
        return true; // if you can create it, you can delete it.  the datastore itself doesn't care.
    }

}
