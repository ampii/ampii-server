package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.LocalizedStrings;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.abstractions.AbstractPolyData;

/**
 * PolyData is a typeless holder, a hybrid primitive/constructed base type that accepts both value and children.
 * <p>
 * It is also the only pseudo-polymorphic type that you can call setBase() on. It is only called "pseudo" polymorphic
 * because it doesn't really do any error checking the way real base types do.
 * <p>
 * The value is always a String (or {@link LocalizedStrings}) and there are no validation checks of any kind, so it's
 * not a suitable replacement at run time for real base types like EnumeratedData or ListData.
 * <p>
 * Since it has no rules checking, it is really only for temporary purposes like freshly parsed data from XML or JSON
 * that will be later assigned to real base types. And that is actually handled by the {@link ParsedData} extension.
 *
 * @author drobin
 */
public class PolyData extends AbstractPolyData {

    public PolyData(String name, Object... initializers) throws XDException { super(name, initializers); }

}
