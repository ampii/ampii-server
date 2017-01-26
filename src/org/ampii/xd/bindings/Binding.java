// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.bindings;

import org.ampii.xd.application.Historian.PeriodMethod;
import org.ampii.xd.application.Policy;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import java.util.Calendar;

/**
 * Implement this interface and attach it to a data item with the {@link Meta#AMPII_BINDING} metadata to provide a
 * dynamic connection to back end data.
 * <p>
 * See {@link org.ampii.xd.security.AuthManager} or search for the many uses of {@link Binding} for examples.
 *
 * @author drobin
 */
public interface Binding {
    void         preread(Data target)                   throws XDException;
    Data         prefind(Data target, String name)      throws XDException;
    Data         prepost(Data target, Data given)       throws XDException;
    boolean      commit(Data target)                    throws XDException;
    boolean      discard(Data target);
    Policy       getPolicy();                           // the DefaultBinding returns a very restrictive Policy that doesn't allow creating any metadata. Override if you do allow.
    Integer      getTotalCount();                       // true count of children for sparse bindings, or null
    Integer      getTotalLength();                      // true length of value for large-value bindings, or null
    DataList     getContextualizedChildren(Data target) throws XDException;
    DataList     getContextualizedMetadata(Data target) throws XDException;
    String       getContextualizedValue(Data target)    throws XDException; // only used by GET alt=plain with skip & max-results, for Strings and OctetStrings
    Data         findHistoryLogBuffer(Data data);       // return null if no history is available
    String       computeHistoryPeriodic(Data data, Calendar start, int period, int count, PeriodMethod method) throws XDException; // return null if no history is available
}
