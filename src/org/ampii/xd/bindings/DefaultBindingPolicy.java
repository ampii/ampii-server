// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.bindings;

import org.ampii.xd.data.Base;
import org.ampii.xd.data.Data;
import org.ampii.xd.application.Policy;
import org.ampii.xd.data.Rules;

/**
 * The DefaultBinding returns a very restrictive {@link Policy} that doesn't allow storing any metadata.
 * Override if you <i>do</i> allow it.
 *
 * @author daverobin
 */
public class DefaultBindingPolicy implements Policy {

    private static DefaultBindingPolicy thePolicy = new DefaultBindingPolicy();

    public  static Policy getThePolicy() { return thePolicy; }

    public boolean allowCreate(Data host, String name, String type, Base base) {
        return Rules.isChild(name); // assume true for children and false for metadata - bindings will override this if different
    }

    public boolean allowDelete(Data host, String name) {
        return true; // assume true for both children and metadata - bindings will override this if different
    }


}
