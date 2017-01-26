// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.managers;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.*;
import org.ampii.xd.data.basetypes.StringData;
import org.ampii.xd.database.Session;

/**
 * Manages the contents of the /.info data item.
 * <p>
 * This is rather silly class at the moment. It just makes /.info/software-version dynamic from Application class so
 * we don't have to update the config XML file or make a Binding just for this.  Maybe it will do more in the future.
 *
 * @author drobin
 */
public class InfoManager {

    public static void init() throws XDException {
        Session.atomicPut("InfoManager",".../.info/software-version",new StringData("",Application.version));
    }
}
