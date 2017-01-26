// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.common;

import org.ampii.xd.database.DataStore;

import java.util.ArrayList;

/**
 * A holder for mulitiple {@link LocalizedString} objects. This is useful as the "value" of a StringData since it is
 * localizable and can simultaneously contains values for multiple locales.
 *
 * @author drobin
 */
public class LocalizedStrings extends ArrayList<LocalizedString> {

    public LocalizedStrings()  {  }

    public LocalizedStrings(Object... initializers) {
        for (Object initializer : initializers) {
            if      (initializer instanceof String)           put((String)initializer);
            else if (initializer instanceof LocalizedStrings) for (LocalizedString locstr : (LocalizedStrings)initializer) put(locstr);
            else if (initializer instanceof LocalizedString)  put((LocalizedString)initializer);
            // ignore other types
        }
    }

    public  void    put(String s)           { put(DataStore.getDatabaseLocaleString(), s); }

    public  void    put(LocalizedString ls) { put(ls.getLocale(), ls.getValue()); }

    public  void    put(String locale, String value) {
        locale = _cleanLocale(locale);
        LocalizedString existing = _find(locale);
        if (existing != null) existing.setValue(value);
        else add(new LocalizedString(locale,value));
    }

    public  String  get(String locale)  {
        // language from the standard is quoted in the numbered comments below
        if (locale == null) locale = DataStore.getDatabaseLocaleString(); // if given null, we will assume the default locale
        // 1) If there is a value with a locale matching exactly the 'locale' parameter, then the server shall return that value.
        for (LocalizedString ls : this) if (ls.getLocale().equals(locale)) return ls.getValue();
        // 2) If there are one or more values with a locale matching the language portion of the 'locale' parameter, then the server shall return one of those values, the selection of which is a local mater.
        String language = locale.contains("-") ? locale.substring(0,locale.indexOf('-')) : locale;
        for (LocalizedString ls : this) if (ls.getLocale().startsWith(language)) return ls.getValue();
        // 3) If there is a value with a locale matching exactly the system default locale, then the server shall return that value.
        String defaultLocale = DataStore.getDatabaseLocaleString();
        for (LocalizedString ls : this) if (ls.getLocale().equals(defaultLocale)) return ls.getValue();
        // 4) If there are one or more values with a locale matching the language portion of the system default locale, then the server shall return one of those values, the selection of which is a local mater.
        language = locale.contains("-") ? locale.substring(0,locale.indexOf('-')) : locale;
        for (LocalizedString ls : this) if (ls.getLocale().startsWith(language)) return ls.getValue();
        // 5) The server shall return any value, the selection of which is a local matter.
        return size()>0 ? get(0).getValue() : "";
    }

    public boolean equals(Object other) {
        if (!(other instanceof LocalizedStrings)) return false;
        LocalizedStrings that = (LocalizedStrings)other;
        for (LocalizedString ls : this) { if (!that.get(ls.getLocale()).equals(ls.getValue())) return false; }
        for (LocalizedString ls : that) { if (!this.get(ls.getLocale()).equals(ls.getValue())) return false; }
        return true;
    }

    ///////////////////////////////////////////////

    // _find() has none of the fancy search rules of get()
    private LocalizedString _find(String locale) {
        locale = _cleanLocale(locale);
        for (LocalizedString ls : this) if (ls.getLocale().equals(locale)) { return ls; }
        return null;
    }

    private  String _cleanLocale(String locale) {
        return (locale == null || locale.isEmpty()) ? DataStore.getDatabaseLocaleString() : locale;
    }



}
