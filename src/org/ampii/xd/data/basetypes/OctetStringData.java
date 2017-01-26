// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.bindings.Binding;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.abstractions.AbstractBinaryData;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.definitions.Builtins;
import org.ampii.xd.data.Context;

/**
 * The implementation of the OctetString base type.
 * This provides value validation but most other behavior is provided by super classes and {@link AbstractData}
 *
 * @author drobin
 */
public class OctetStringData extends AbstractBinaryData {

    public OctetStringData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.OCTETSTRING);
    }

    @Override public Base    getBase() { return Base.OCTETSTRING; }

    protected boolean        commitValue() throws XDException  {
        // called while committing in AbstractData to handle special cases for committing value
        // We need to deal with merging bytes into original, which only happens in plain text.
        // Since this involved possible merging, large-value bindings have to handle this themselves in their commit()
        // we only handle normal in-memory values here.
        Context context = getContext();
        if (context.getAlt().equals("plain")) {
            byte[] newValue = value; // the "new value" is our local value before the commit
            if (context.hasSkip()) { // if given a 'skip' then merge newValue into current value
                byte[] currentValue  = original.byteArrayValue();
                int    currentLength = currentValue.length;
                int    newLength     = newValue.length;
                int    skip          = context.getSkip();
                if (skip > currentLength || skip < 0) { // an append
                    byte[] combined = new byte[currentLength + newLength];
                    System.arraycopy(currentValue,0,combined,0            ,currentLength);
                    System.arraycopy(newValue,    0,combined,currentLength,newLength);
                    newValue = combined; // assign below
                } else { // an overwrite
                    int combinedLength = Math.max(skip + newLength, currentLength);
                    byte[] combined = new byte[combinedLength];
                    System.arraycopy(currentValue, 0, combined, 0,    skip);
                    System.arraycopy(newValue,     0, combined, skip, newLength);
                    if (currentLength > skip+newLength) // any original left over at the end?
                        System.arraycopy(currentValue, skip+newLength, combined, skip+newLength, currentLength-(skip+newLength));
                    newValue = combined; // assign below
                }
            }
            original.setLocalValue(newValue);
            return true;
        }
        return false;
    }

    protected int   getCurrentLength() throws XDException {
        Integer result = null;
        Binding binding = findBinding(); // large-value bindings will hook this, so let the binding have a chance to override the default local length
        if (binding != null) result = binding.getTotalLength();
        return result != null? result : value != null? value.length : 0;
    }

    protected void  validateLength(int givenLength) throws XDException {  // overridden from base class to handle merging
        Context context = getContext();
        int resultLength = givenLength;
        if (context.hasSkip()) {
            int currentLength = getCurrentLength();
            int skip = context.getSkip();
            if (skip > currentLength || skip == -1) { // is this an append?
                resultLength = currentLength + givenLength;
            }
            else if (context.hasSkip()) { // is this in insert?
                resultLength = context.getSkip() + givenLength;
                if (resultLength < currentLength) resultLength = currentLength;
            }
        }
        super.validateLength(resultLength);
    }

}
