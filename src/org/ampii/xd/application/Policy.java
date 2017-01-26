// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application;

import org.ampii.xd.data.Base;
import org.ampii.xd.data.Data;

/**
 * An interface to determine if the server policy for a data item allows the creation or deletion of data or metadata.
 * This is in addition to writability and authorization and basically indicates if the server can create storage for
 * the item or if the item should not be removable for some reason.  Policies can be dynamic and the default policy can
 * be overridden by bindings or hooks.
 *
 * @author drobin
 */
public interface Policy {
    boolean allowCreate(Data target, String name, String type, Base base);
    boolean allowDelete(Data target, String name);
}
