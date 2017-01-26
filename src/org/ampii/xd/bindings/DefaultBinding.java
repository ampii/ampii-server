// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.bindings;

import org.ampii.xd.application.Historian.PeriodMethod;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.DataList;
import org.ampii.xd.application.Policy;

import java.util.Calendar;

/**
 * This is a Binding that does nothing, to be overridden by someone who does. See {@link Binding} for explanation
 * of these methods.
 * <p>
 * Extending this base class can simplify your code because you only need to override the methods that you need.
 * <p>
 * Note that the DefaultBinding returns a very restrictive {@link Policy} that doesn't allow storing any metadata.
 * Override if you <i>do</i> allow it.
 *
 * @author drobin
 */
public class DefaultBinding implements Binding {
    @Override public void         preread(Data target)                 throws XDException { }
    @Override public Data         prefind(Data target, String name)    throws XDException { return null;  }  // non-null = "use this"; else look for it with findLocal()
    @Override public Data         prepost(Data target, Data given)     throws XDException { return given; }
    @Override public boolean      commit(Data target)                  throws XDException { return false; } // false = "I didn't handle it"
    @Override public boolean      discard(Data target)                                    { return false; } // false = "I didn't handle it"
    @Override public DataList     getContextualizedChildren(Data data) throws XDException { return null;  }  // null means filter the local children
    @Override public DataList     getContextualizedMetadata(Data data) throws XDException { return null;  }  // null means filter the local metadata
    @Override public String       getContextualizedValue(Data data)    throws XDException { return null;  }  // null means use local value
    @Override public Policy       getPolicy()                                             { return DefaultBindingPolicy.getThePolicy();}
    @Override public Integer      getTotalCount()                                         { return null;  }
    @Override public Integer      getTotalLength()                                        { return null;  }
    @Override public Data         findHistoryLogBuffer(Data data)                         { return null;  }
    @Override public String       computeHistoryPeriodic(Data data, Calendar start, int period, int count, PeriodMethod method)  throws XDException { return null; }
}
