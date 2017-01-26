// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.bindings.Binding;
import org.ampii.xd.common.*;
import org.ampii.xd.data.*;
import org.ampii.xd.data.abstractions.*;
import org.ampii.xd.definitions.Builtins;
import org.ampii.xd.data.Context;

/**
 * Extends the basic {@link AbstractTextData} by adding support for localized values. The this.value member is declared
 * to be an Object and other text types like  {@link EnumeratedData} require it to only be a single String.
 * But in this subclass, it can be either a String or a {@link LocalizedStrings} object.
 *
 * @author drobin
 */
public class StringData extends AbstractTextData {

    public StringData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        setIsLocalizable(true);  // generic StringData is localizable by default
        prototype = Builtins.getPrototypeOfBase(Base.STRING);
    }

    @Override public Base    getBase()  { return Base.STRING; }

    protected boolean commitValue() throws XDException  {
        // called while committing in AbstractData to handle special cases for committing value
        // We need to deal with merging string into original and handling setting one locale at a time, both of which only happen in plain text.
        // Since this involved possible merging, large-value bindings have to handle this themselves, we only do normal values.
        Context context = getContext();
        if (context.getAlt().equals("plain")) { // lots of magic is afforded to plain text access for strings
            if (!(value instanceof String)) throw new XDError("commit(): alt=plain value is not a String"); // internal error, this should not happen
            String newValue = (String)value;
            if (context.hasSkip()) { // if given a 'skip' then merge string
                Object originalValue = original.getValue();
                String currentValue = (originalValue instanceof LocalizedStrings) ? ((LocalizedStrings)originalValue).get(context.getLocale()) : original.stringValue();
                int skip = context.getSkip();
                if (skip > currentValue.length() || skip < 0) { // an append
                    newValue = currentValue + newValue;
                } else { // an overwrite
                    newValue = currentValue.substring(0, skip) + newValue;  // first part
                    if (currentValue.length() > newValue.length())
                        newValue += currentValue.substring(newValue.length()); // any left over?
                }
            }
            // now that we have our result string, which locale is it for?
            if (context.hasLocale()) {
                LocalizedStrings strings = new LocalizedStrings(original.getValue());
                strings.put(context.getLocale(), newValue);
                original.setLocalValue(strings);
            } else {
                original.setLocalValue(newValue); // not given 'locale', so remove all locales - Rule (1) of "Setting values" in W.17
            }
            return true;
        }
        else {
            return false;
        }
    }

    @Override public Object  getValue() throws XDException {  // returns either String or LocalizedStrings object
        preread();
        return (value instanceof LocalizedStrings)? new LocalizedStrings(value) : value;
    }

    @Override public void setLocalValue(Object value)  {
        if (value instanceof String || value instanceof LocalizedStrings || value == null) this.value = value;
        else throw new XDError(this, "setLocalValue() is given an unknown class");
    }

    @Override public void    setValue(Object newValue) throws XDException {
        preread();
        if (newValue instanceof String)  {  // we are given a simple string...
            // This removes all localizations (yes, Rule 1 of "setting data" in W.17 seems harsh but it enforces consistency!)
            // However, it *is* possible for internal clients to modify a single locale by calling this with a 'locale' set in the context.
            validateMergedLength((String)newValue); // since single strings can be merged by a context 'skip', we can't just use our superclass' validateLength()
            value = newValue;
        }
        else if (newValue instanceof LocalizedStrings) { // we are given a complete set of locales, so check length of each one
            for (LocalizedString ls : (LocalizedStrings)newValue) validateLength(ls.getValue().length());
            value = new LocalizedStrings((LocalizedStrings)newValue); // make copy of what we are given so it's immutable
        }
        else if (newValue instanceof LocalizedString) { // we are given a single locale, so check length of value
            validateLength(((LocalizedString)newValue).getValue().length());
            value = new LocalizedStrings(value,newValue); // this merges in new copy into any existing locales
        }
        else {
            super.setValue(newValue); // handle the rest
        }
        markDirty();
    }

    @Override public String    stringValue()  throws XDException { // gets string value in context's locale
        preread();
        return _stringValueForLocale(getContext().getLocale());
    }

    private String _stringValueForLocale(String locale)  throws XDException {
        preread();
        if (value instanceof String)           return (String)value;
        if (value instanceof LocalizedStrings) return ((LocalizedStrings)value).get(locale); // LocalizedStrings.get() does all the hard work in Clause W.17
        return "";
    }

    @Override public boolean  booleanValue()  throws XDException { preread(); return !stringValue().isEmpty(); }


    protected int   getCurrentLength() throws XDException {
        Integer result = null;
        Binding binding = findBinding(); // large-value bindings will hook this, so let the binding have a chance to override the default local length
        if (binding != null) result = binding.getTotalLength();
        return result != null? result : stringValue().length();
    }

    private void   validateMergedLength(String newValue) throws XDException {
        Context context = getContext();
        int givenLength = newValue.length();
        int resultLength = givenLength;
        if (context.hasSkip()) {
            int currentLength = getCurrentLength(); // getCurrentLength can be hooked by large-value bindings
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
