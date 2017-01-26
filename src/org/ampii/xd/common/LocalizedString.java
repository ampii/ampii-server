// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.common;

import org.ampii.xd.database.DataStore;

/**
 * This is just a wrapper to hold a single string and its locale.
 * {@link LocalizedStrings} can be used to hold a collection of these and provide lookups.
 *
 * @author drobin
 */
public class LocalizedString {

    private String locale;
    private String value;

    public LocalizedString(String locale, String value) {
        this.locale = (locale == null || locale.isEmpty())? DataStore.getDatabaseLocaleString() : locale;
        this.value  = value == null? "" : value;
    }

    public boolean isDefaultLocale()       { return locale.equals(DataStore.getDatabaseLocaleString()); }
    public String  getLocale()             { return locale; }
    public String  getValue()              { return value;  }
    public void    setValue(String value)  { this.value = value; }
    // there is no setLocale() - you can't change locale once created

}
