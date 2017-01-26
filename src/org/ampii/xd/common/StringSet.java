// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.common;

import org.ampii.xd.data.Data;
import org.ampii.xd.data.basetypes.StringSetData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Contains a set of strings like and methods for querying, adding and removing the component strings.
 * Useful as the "value" of {@link StringSetData}, and many other uses like the 'metadata' query parameter.
 */
public class StringSet {

    private List<String> components = new ArrayList<String>();

    public StringSet()                 { }

    public StringSet(String string)    {
        if (string.contains(";")) for (String component : string.split(";")) components.add(component);
        else components.add(string);
    }

    public StringSet(StringSet source) { components = new ArrayList<String>(source.components); }

    public StringSet(String[]  source) { components.addAll(Arrays.asList(source)); }

    public void add(String string)     { if (!containsComponent(string)) components.add(string); }

    public void add(StringSet source)  { for (String string : source.components) add(string); }

    public void add(String... source)  { for (String string : Arrays.asList(source)) add(string); }

    public void remove(String string)  { components.remove(string); }

    public void update(String string)  {
        if (string.contains("+") || string.contains("-")) {   // selectively add and remove components
            for (String component : string.split(";")) {
                if (component.startsWith("+")) {
                    component = component.substring(1);
                    if (!components.contains(component)) components.add(component);

                }
                else if (component.startsWith("-")) {
                    components.remove(component.substring(1));
                }
                else  {
                    // no complain here. validatePossibleValue() will catch mixing of +/- and plain
                    if (!components.contains(component)) components.add(component);
                }
            }
        }
        else {
            components = new ArrayList<String>(); // replace all
            components.addAll(Arrays.asList(string.split(";")));
        }
    }

    public int      size()             { return components.size(); }

    public String   toString()  {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String component : components) {
            if (!first) sb.append(';');
            sb.append(component);
            first = false;
        }
        return sb.toString();
    }

    public boolean  containsComponent(String component) {
        for (String s : components)
            if (s.equals(component))
                return true;
        return false;
    }


    public List<String> getComponents() { return new ArrayList<String>(components); }

    public void validateValue(Data target, String newValue)  throws XDException { validateValue(target,newValue,null);}

    public void validateValue(Data target, String newValue, List<String> validComponents) throws XDException {
        boolean doingPlain=false, doingPlusMinus=false;
        for (String component : newValue.split(";")) {
            if (component.startsWith("+") || component.startsWith("-") ) {
                if (doingPlain) throw new XDException(Errors.VALUE_FORMAT, target, "Can't combine +/- syntax with plain entries");
                doingPlusMinus = true;
                if (validComponents != null) validateComponent(target, component.substring(1), validComponents);
            } else {
                if (doingPlusMinus) throw new XDException(Errors.VALUE_FORMAT, target, "Can't combine +/- syntax with plain entries");
                doingPlain = true;
                if (validComponents != null) validateComponent(target, component, validComponents);
            }
        }
    }

    protected void validateComponent(Data target, String component, List<String> validComponents) throws XDException {
        if (validComponents == null) return;
        for (String valid : validComponents) if (valid.equals(component)) return;
        else throw new XDException(Errors.VALUE_FORMAT, target, "String segment '"+ component +"' is not a valid member");
    }


}
