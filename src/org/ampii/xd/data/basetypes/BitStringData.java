package org.ampii.xd.data.basetypes;


import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Meta;
import org.ampii.xd.common.StringSet;
import org.ampii.xd.data.abstractions.AbstractStringSetData;
import org.ampii.xd.definitions.Builtins;

public class BitStringData extends AbstractStringSetData {

    public BitStringData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.BITSTRING);
    }

    @Override public Base getBase() {
        return Base.BITSTRING;
    }

    @Override public void setValue(Object newValue) throws XDException {
        preread();
        StringSet tempValue;
        if (newValue instanceof StringSet)   tempValue = new StringSet((StringSet)newValue);
        else if (newValue instanceof String) tempValue = ((String)newValue).isEmpty() ? new StringSet() : new StringSet((String)newValue);
        else if (newValue == null) throw new XDError(this, "setValue() is given a null");
        else                       throw new XDError(this, "setValue() is given an unknown class");
        validateValue(tempValue);
        value = tempValue;
        markDirty();
    }

    @Override public void  validateConsistency()  throws XDException {
        int max = effectiveIntValueOf(Meta.MAXIMUMLENGTH, Integer.MAX_VALUE);
        int min = effectiveIntValueOf(Meta.MINIMUMLENGTH, 0);
        int len = effectiveIntValueOf(Meta.LENGTH, -1);
        if (len != -1 && (len > max || len < min)) throw new XDException(Errors.VALUE_FORMAT,"$length not with limits");
        if (value != null) validateValue(value);
    }

    private void  validateValue(StringSet value)  throws XDException {
        int max = effectiveIntValueOf(Meta.MAXIMUMLENGTH, Integer.MAX_VALUE);
        int min = effectiveIntValueOf(Meta.MINIMUMLENGTH, 0);
        int len = effectiveIntValueOf(Meta.LENGTH, -1);
        Data namedBits = findEffective(Meta.NAMEDBITS);
        for (String component : value.getComponents()) { // now check any numbers in the value
            if (isNumeric(component)) {
                int i = Integer.parseInt(component);
                if (i > max || i > len && len != -1) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Bit identifier '"+component+"' exceeds bit string length");
            }
            else { // not a number, better be a valid name if we have $namedBits
                if (namedBits != null && namedBits.find(component) == null) throw new XDException(Errors.VALUE_OUT_OF_RANGE, this, "Bit identifier '" + component + "' not in $namedBits");
            }
        }
    }

    private static boolean isNumeric(String str) {
        for (char c : str.toCharArray())  if (!Character.isDigit(c)) return false;
        return true;
    }


}
