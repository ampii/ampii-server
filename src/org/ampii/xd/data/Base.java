// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the list of standard base types, and methods for going to/from string representations
 *
 * @author drobin
 */
public enum Base {
    // indexes are shown just to make sure they match up with things like the toString array
    INVALID,                // 0 is not a real base type, for error return values only
    NULL,                   // 1
    BOOLEAN,                // 2
    UNSIGNED,               // 3
    INTEGER,                // 4
    REAL,                   // 5
    DOUBLE,                 // 6
    OCTETSTRING,            // 7
    STRING,                 // 8
    BITSTRING,              // 9
    ENUMERATED,             // 10
    DATE,                   // 11
    DATEPATTERN,            // 12
    DATETIME,               // 13
    DATETIMEPATTERN,        // 14
    TIME,                   // 15
    TIMEPATTERN,            // 16
    OBJECTIDENTIFIER,       // 17
    OBJECTIDENTIFIERPATTERN,// 18
    WEEKNDAY,               // 19
    SEQUENCE,               // 20
    ARRAY,                  // 21
    LIST,                   // 22
    SEQUENCEOF,             // 23
    CHOICE,                 // 24
    OBJECT,                 // 25
    BIT,                    // 26
    LINK,                   // 27
    ANY,                    // 28
    STRINGSET,              // 29
    COMPOSITION,            // 30
    COLLECTION,             // 31
    UNKNOWN,                // 32
    RAW,                    // 33
    POLY;                   // 34 not a standard base type - for internal use only

    public static String toString(Base base)       { return baseToString[base.ordinal()];  }

    public static Base   fromString(String string) { Base result = baseFromString.get(string);  return result!=null ? result : Base.INVALID; }

    private static final String[] baseToString = {
            "Invalid",                   // INVALID                  0
            "Null",                      // NULL                     1
            "Boolean",                   // BOOLEAN                  2
            "Unsigned",                  // UNSIGNED                 3
            "Integer",                   // INTEGER                  4
            "Real",                      // REAL                     5
            "Double",                    // DOUBLE                   6
            "OctetString",               // OCTETSTRING              7
            "String",                    // STRING                   8
            "BitString",                 // BITSTRING                9
            "Enumerated",                // ENUMERATED               10
            "Date",                      // DATE                     11
            "DatePattern",               // DATEPATTERN              12
            "DateTime",                  // DATETIME                 13
            "DateTimePattern",           // DATETIMEPATTERN          14
            "Time",                      // TIME                     15
            "TimePattern",               // TIMEPATTERN              16
            "ObjectIdentifier",          // OBJECTIDENTIFIER         17
            "ObjectIdentifierPattern",   // OBJECTIDENTIFIERPATTERN  18
            "WeekNDay",                  // WEEKNDAY                 19
            "Sequence",                  // SEQUENCE                 20
            "Array",                     // ARRAY                    21
            "List",                      // LIST                     22
            "SequenceOf",                // SEQUENCEOF               23
            "Choice",                    // CHOICE                   24
            "Object",                    // OBJECT                   25
            "Bit",                       // BIT                      26
            "Link",                      // LINK                     27
            "Any",                       // ANY                      28
            "StringSet",                 // STRINGSET                29
            "Composition",               // COMPOSITION              30
            "Collection",                // COLLECTION               31
            "Unknown",                   // UNKNOWN                  32
            "Raw",                       // RAW                      33
            "Poly",                      // POLY                     34
    };



    private static Map<String,Base> baseFromString;
    static {
        Map<String, Base> theMap = new HashMap<String,Base>();
        theMap.put("Invalid",                 Base.INVALID);
        theMap.put("Null",                    Base.NULL);
        theMap.put("Boolean",                 Base.BOOLEAN);
        theMap.put("Unsigned",                Base.UNSIGNED);
        theMap.put("Integer",                 Base.INTEGER);
        theMap.put("Real",                    Base.REAL);
        theMap.put("Double",                  Base.DOUBLE);
        theMap.put("OctetString",             Base.OCTETSTRING);
        theMap.put("String",                  Base.STRING);
        theMap.put("BitString",               Base.BITSTRING);
        theMap.put("Enumerated",              Base.ENUMERATED);
        theMap.put("Date",                    Base.DATE);
        theMap.put("DatePattern",             Base.DATEPATTERN);
        theMap.put("DateTime",                Base.DATETIME);
        theMap.put("DateTimePattern",         Base.DATETIMEPATTERN);
        theMap.put("Time",                    Base.TIME);
        theMap.put("TimePattern",             Base.TIMEPATTERN);
        theMap.put("ObjectIdentifier",        Base.OBJECTIDENTIFIER);
        theMap.put("ObjectIdentifierPattern", Base.OBJECTIDENTIFIERPATTERN);
        theMap.put("WeekNDay",                Base.WEEKNDAY);
        theMap.put("Sequence",                Base.SEQUENCE);
        theMap.put("Array",                   Base.ARRAY);
        theMap.put("List",                    Base.LIST);
        theMap.put("SequenceOf",              Base.SEQUENCEOF);
        theMap.put("Choice",                  Base.CHOICE);
        theMap.put("Object",                  Base.OBJECT);
        theMap.put("Bit",                     Base.BIT);
        theMap.put("Link",                    Base.LINK);
        theMap.put("Any",                     Base.ANY);
        theMap.put("StringSet",               Base.STRINGSET);
        theMap.put("Composition",             Base.COMPOSITION);
        theMap.put("Collection",              Base.COLLECTION);
        theMap.put("Unknown",                 Base.UNKNOWN);
        theMap.put("Raw",                     Base.RAW);
        theMap.put("Poly",                    Base.POLY);
        baseFromString = Collections.unmodifiableMap(theMap);
    }

}


