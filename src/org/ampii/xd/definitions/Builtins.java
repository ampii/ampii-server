// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.definitions;

import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.StringData;

/**
 * Builtins are the "ultimate prototypes" that all instances and other prototypes end up pointing to.
 * <p>
 * They are valid instances of their respective base type class and are used to define all the optional metadata for
 * that base type. This could have been hard-coded into the various base type classes themselves, but this technique
 * uses the existing mechanisms for creating optional items in general rather than any new methods to check what is
 * possible to create.
 * <p>
 * Builtins are constructed to be usable as prototypes, and all the base type classes will set the appropriate
 * builtin as their prototype when they are constructed. This is how the existing "create optional item" mechanism is
 * leveraged since that looks to the prototype to know what is possible.
 *
 * @author daverobin
 */
public class Builtins {

    private static Data makeBuiltin(Base base, String name, Object... initializers) throws XDException {
        Data builtin = DataFactory.make(base, name);
        builtin.setIsBuiltin(true);
        builtin.setIsPrototype(true);
        for (Object initializer : initializers) { // value or metadata
            if (initializer instanceof Data) { builtin.addLocal(((Data) initializer)); }
            else builtin.setValue(initializer);
        }
        return builtin;
    }

    //////////////////////////////////////////

    private static void addCommonMetadata(Data builtin) throws XDException {
        addOptional(builtin,
                makeBuiltin(Base.STRING,     Meta.ID),
                makeBuiltin(Base.STRING,     Meta.TYPE),
                makeBuiltin(Base.STRING,     Meta.EFFECTIVETYPE),
                makeBuiltin(Base.STRING,     Meta.EXTENDS),
                makeBuiltin(Base.STRING,     Meta.OVERLAYS),
                makeBuiltin(Base.ENUMERATED, Meta.NODETYPE, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetNodeType")),
                makeBuiltin(Base.STRING,     Meta.NODESUBTYPE),
                makeBuiltin(Base.STRING,     Meta.DISPLAYNAME),
                makeBuiltin(Base.STRING,     Meta.DESCRIPTION),
                makeBuiltin(Base.STRING,     Meta.DOCUMENTATION),
                makeBuiltin(Base.STRING,     Meta.COMMENT),
                makeBuiltin(Base.BOOLEAN,    Meta.WRITABLE),
                makeBuiltin(Base.BOOLEAN,    Meta.COMMANDABLE),
                makeBuiltin(Base.LIST,       Meta.FAILURES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Link")),
                makeBuiltin(Base.BOOLEAN,    Meta.READABLE),
                makeBuiltin(Base.STRING,     Meta.ASSOCIATEDWITH),
                makeBuiltin(Base.STRING,     Meta.REQUIREDWITH),
                makeBuiltin(Base.STRING,     Meta.REQUIREDWITHOUT),
                makeBuiltin(Base.STRING,     Meta.NOTPRESENTWITH),
                makeBuiltin(Base.ENUMERATED, Meta.WRITEEFFECTIVE),
                makeBuiltin(Base.BOOLEAN,    Meta.OPTIONAL),
                makeBuiltin(Base.BOOLEAN,    Meta.ABSENT),
                makeBuiltin(Base.ENUMERATED, Meta.VARIABILITY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetVariability")),
                makeBuiltin(Base.ENUMERATED, Meta.VOLATILITY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetVolatility")),
                makeBuiltin(Base.BOOLEAN,    Meta.ISMULTILINE),
                makeBuiltin(Base.BOOLEAN,    Meta.INALARM),
                makeBuiltin(Base.BOOLEAN,    Meta.OVERRIDDEN),
                makeBuiltin(Base.BOOLEAN,    Meta.FAULT),
                makeBuiltin(Base.BOOLEAN,    Meta.OUTOFSERVICE),
                makeBuiltin(Base.LIST,       Meta.LINKS, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Link")),
                makeBuiltin(Base.STRINGSET,  Meta.TAGS),
                makeBuiltin(Base.COLLECTION, Meta.VALUETAGS),
                makeBuiltin(Base.STRING,     Meta.AUTHREAD),
                makeBuiltin(Base.STRING,     Meta.AUTHWRITE),
                makeBuiltin(Base.BOOLEAN,    Meta.AUTHVISIBLE),
                makeBuiltin(Base.STRING,     Meta.HREF),
                makeBuiltin(Base.STRING,     Meta.SOURCEID),
                makeBuiltin(Base.STRING,     Meta.ETAG),
                makeBuiltin(Base.STRING,     Meta.TARGETTYPE),
                makeBuiltin(Base.STRING,     Meta.DISPLAYNAMEFORWRITING),
                makeBuiltin(Base.BOOLEAN,    Meta.NOTFORREADING),
                makeBuiltin(Base.BOOLEAN,    Meta.NOTFORWRITING),
                makeBuiltin(Base.UNSIGNED,   Meta.ERROR),
                makeBuiltin(Base.STRING,     Meta.ERRORTEXT),
                makeBuiltin(Base.UNSIGNED,   Meta.DISPLAYORDER),
                makeBuiltin(Base.DATETIME,   Meta.PUBLISHED),
                makeBuiltin(Base.DATETIME,   Meta.UPDATED),
                makeBuiltin(Base.STRING,     Meta.AUTHOR),
                makeBuiltin(Base.STRING,     Meta.ADDREV),
                makeBuiltin(Base.STRING,     Meta.REMREV),
                makeBuiltin(Base.STRING,     Meta.MODREV),
                makeBuiltin(Base.COLLECTION, Meta.REVISIONS),
                makeBuiltin(Base.ENUMERATED, Meta.WRITABLEWHEN),
                makeBuiltin(Base.STRING,     Meta.WRITABLEWHENTEXT),
                makeBuiltin(Base.ENUMERATED, Meta.REQUIREDWHEN),
                makeBuiltin(Base.STRING,     Meta.REQUIREDWHENTEXT),
                makeBuiltin(Base.UNSIGNED,   Meta.CONTEXTTAG),
                makeBuiltin(Base.UNSIGNED,   Meta.PROPERTYIDENTIFIER),
                makeBuiltin(Base.UNSIGNED,   Meta.COUNT),
                makeBuiltin(Base.LIST,       Meta.DESCENDANTS, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Link")),
                makeBuiltin(Base.LIST,       Meta.HISTORY,     makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "0-BACnetTrendLogRecord")),
                makeBuiltin(Base.BOOLEAN,    Meta.TRUNCATED),
                makeBuiltin(Base.BOOLEAN,    Meta.PARTIAL),
                makeBuiltin(Base.LINK,       Meta.VIA),
                makeBuiltin(Base.LINK,       Meta.SELF),
                makeBuiltin(Base.LINK,       Meta.NEXT),
                makeBuiltin(Base.LINK,       Meta.EDIT),
                makeBuiltin(Base.LINK,       Meta.SUBSCRIPTION),
                makeBuiltin(Base.LINK,       Meta.RELATED),
                makeBuiltin(Base.STRING,     Meta.ALTERNATE),
                // nonstandard things understood by this server
                makeBuiltin(Base.BOOLEAN,    Meta.AMPII_MATCH_ANY),
                makeBuiltin(Base.STRING,     Meta.AMPII_BINDING),
                makeBuiltin(Base.STRING,     Meta.AMPII_HISTORY_LOCATION),
                makeBuiltin(Base.COLLECTION, Meta.AMPII_DEFINITIONS),
                makeBuiltin(Base.COLLECTION, Meta.AMPII_TAG_DEFINITIONS)
        );
    }

    private static void addOptional(Data builtin, Data... list) throws XDException {
        // adds a list of metadata and marks them as optional
        // this saves a lot of typing below because everything is optional!
        for (Data item : list) {
            // add a new $optional to each item
            Data optional = makeBuiltin(Base.BOOLEAN, Meta.OPTIONAL, true);
            item.addLocal(optional);
            // now add the item to this
            builtin.addLocal(item);
        }
    }


    private static boolean initialized = false;

    private static Data nullPrototype;
    private static Data booleanPrototype;
    private static Data unsignedPrototype;
    private static Data integerPrototype;
    private static Data realPrototype;
    private static Data doublePrototype;
    private static Data octetStringPrototype;
    private static Data stringPrototype;
    private static Data bitStringPrototype;
    private static Data enumeratedPrototype;
    private static Data datePrototype;
    private static Data datePatternPrototype;
    private static Data dateTimePrototype;
    private static Data dateTimePatternPrototype;
    private static Data timePrototype;
    private static Data timePatternPrototype;
    private static Data objectIdentifierPrototype;
    private static Data objectIdentifierPatternPrototype;
    private static Data weekNDayPrototype;
    private static Data sequencePrototype;
    private static Data arrayPrototype;
    private static Data listPrototype;
    private static Data sequenceOfPrototype;
    private static Data choicePrototype;
    private static Data objectPrototype;
    private static Data bitPrototype;
    private static Data linkPrototype;
    private static Data anyPrototype;
    private static Data stringSetPrototype;
    private static Data compositionPrototype;
    private static Data collectionPrototype;
    private static Data unknownPrototype;
    private static Data rawPrototype;
    private static Data polyPrototype;

    public static void initialize() throws XDException {

        if (initialized) return;

        // BE CAREFUL REFACTORING THIS!   Bootstrapping is tricky.
        // For example, we need to create prototype for a base type like Null that contains a metadata of $displayName 
        // which is of type String, but the prototype for String hasn't been created yet, so what does $displayName use 
        // as it's prototype??? The solution is to first create *empty* prototypes of all base types, then populate them 
        // with their metadata. That way, each metadata has a base prototype to refer to when it is created, even if
        // that base prototype is not fully formed yet.
        // Yay, bootstrapping!

        // FIRST create empty shells
        anyPrototype                     = makeBuiltin(Base.ANY,                     Base.toString(Base.ANY));
        nullPrototype                    = makeBuiltin(Base.NULL,                    Base.toString(Base.NULL));
        booleanPrototype                 = makeBuiltin(Base.BOOLEAN,                 Base.toString(Base.BOOLEAN));
        unsignedPrototype                = makeBuiltin(Base.UNSIGNED,                Base.toString(Base.UNSIGNED));
        integerPrototype                 = makeBuiltin(Base.INTEGER,                 Base.toString(Base.INTEGER));
        realPrototype                    = makeBuiltin(Base.REAL,                    Base.toString(Base.REAL));
        doublePrototype                  = makeBuiltin(Base.DOUBLE,                  Base.toString(Base.DOUBLE));
        octetStringPrototype             = makeBuiltin(Base.OCTETSTRING,             Base.toString(Base.OCTETSTRING));
        stringPrototype                  = makeBuiltin(Base.STRING,                  Base.toString(Base.STRING));
        bitStringPrototype               = makeBuiltin(Base.BITSTRING,               Base.toString(Base.BITSTRING));
        enumeratedPrototype              = makeBuiltin(Base.ENUMERATED,              Base.toString(Base.ENUMERATED));
        datePrototype                    = makeBuiltin(Base.DATE,                    Base.toString(Base.DATE));
        datePatternPrototype             = makeBuiltin(Base.DATEPATTERN,             Base.toString(Base.DATEPATTERN));
        dateTimePrototype                = makeBuiltin(Base.DATETIME,                Base.toString(Base.DATETIME));
        dateTimePatternPrototype         = makeBuiltin(Base.DATETIMEPATTERN,         Base.toString(Base.DATETIMEPATTERN));
        timePrototype                    = makeBuiltin(Base.TIME,                    Base.toString(Base.TIME));
        timePatternPrototype             = makeBuiltin(Base.TIMEPATTERN,             Base.toString(Base.TIMEPATTERN));
        objectIdentifierPrototype        = makeBuiltin(Base.OBJECTIDENTIFIER,        Base.toString(Base.OBJECTIDENTIFIER));
        objectIdentifierPatternPrototype = makeBuiltin(Base.OBJECTIDENTIFIERPATTERN, Base.toString(Base.OBJECTIDENTIFIERPATTERN));
        weekNDayPrototype                = makeBuiltin(Base.WEEKNDAY,                Base.toString(Base.WEEKNDAY));
        sequencePrototype                = makeBuiltin(Base.SEQUENCE,                Base.toString(Base.SEQUENCE));
        arrayPrototype                   = makeBuiltin(Base.ARRAY,                   Base.toString(Base.ARRAY));
        listPrototype                    = makeBuiltin(Base.LIST,                    Base.toString(Base.LIST));
        sequenceOfPrototype              = makeBuiltin(Base.SEQUENCEOF,              Base.toString(Base.SEQUENCEOF));
        choicePrototype                  = makeBuiltin(Base.CHOICE,                  Base.toString(Base.CHOICE));
        objectPrototype                  = makeBuiltin(Base.OBJECT,                  Base.toString(Base.OBJECT));
        bitPrototype                     = makeBuiltin(Base.BIT,                     Base.toString(Base.BIT));
        linkPrototype                    = makeBuiltin(Base.LINK,                    Base.toString(Base.LINK));
        stringSetPrototype               = makeBuiltin(Base.STRINGSET,               Base.toString(Base.STRINGSET));
        compositionPrototype             = makeBuiltin(Base.COMPOSITION,             Base.toString(Base.COMPOSITION));
        collectionPrototype              = makeBuiltin(Base.COLLECTION,              Base.toString(Base.COLLECTION));
        unknownPrototype                 = makeBuiltin(Base.UNKNOWN,                 Base.toString(Base.UNKNOWN));
        rawPrototype                     = makeBuiltin(Base.RAW,                     Base.toString(Base.RAW));
        polyPrototype                    = makeBuiltin(Base.POLY, Base.toString(Base.POLY));

        // THEN fill in the metadata

        addCommonMetadata(anyPrototype);
        addOptional(anyPrototype,
                makeBuiltin(Base.STRINGSET, Meta.ALLOWEDTYPES)
        );

        addCommonMetadata(nullPrototype);

        addCommonMetadata(booleanPrototype);
        addOptional(booleanPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Boolean")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.BOOLEAN, Meta.RELINQUISHDEFAULT)
        );

        addCommonMetadata(unsignedPrototype);
        addOptional(unsignedPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Unsigned")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.UNSIGNED, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUM),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUM),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.RESOLUTION),
                makeBuiltin(Base.ENUMERATED, Meta.UNITS, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetEngineeringUnits")),
                makeBuiltin(Base.STRING, Meta.UNITSTEXT)
        );

        addCommonMetadata(integerPrototype);
        addOptional(integerPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Integer")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.INTEGER, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.INTEGER, Meta.MINIMUM),
                makeBuiltin(Base.INTEGER, Meta.MAXIMUM),
                makeBuiltin(Base.INTEGER, Meta.MINIMUMFORWRITING),
                makeBuiltin(Base.INTEGER, Meta.MAXIMUMFORWRITING),
                makeBuiltin(Base.INTEGER, Meta.RESOLUTION),
                makeBuiltin(Base.ENUMERATED, Meta.UNITS, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetEngineeringUnits")),
                makeBuiltin(Base.STRING, Meta.UNITSTEXT)
        );

        addCommonMetadata(realPrototype);
        addOptional(realPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Real")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.REAL, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.REAL, Meta.MINIMUM),
                makeBuiltin(Base.REAL, Meta.MAXIMUM),
                makeBuiltin(Base.REAL, Meta.MINIMUMFORWRITING),
                makeBuiltin(Base.REAL, Meta.MAXIMUMFORWRITING),
                makeBuiltin(Base.REAL, Meta.RESOLUTION),
                makeBuiltin(Base.ENUMERATED, Meta.UNITS, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetEngineeringUnits")),
                makeBuiltin(Base.STRING, Meta.UNITSTEXT)
        );

        addCommonMetadata(doublePrototype);
        addOptional(doublePrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Double")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.DOUBLE, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.DOUBLE, Meta.MINIMUM),
                makeBuiltin(Base.DOUBLE, Meta.MAXIMUM),
                makeBuiltin(Base.DOUBLE, Meta.MINIMUMFORWRITING),
                makeBuiltin(Base.DOUBLE, Meta.MAXIMUMFORWRITING),
                makeBuiltin(Base.DOUBLE, Meta.RESOLUTION),
                makeBuiltin(Base.ENUMERATED, Meta.UNITS, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetEngineeringUnits")),
                makeBuiltin(Base.STRING, Meta.UNITSTEXT)
        );

        addCommonMetadata(octetStringPrototype);
        addOptional(octetStringPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "OctetString")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.OCTETSTRING, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMLENGTHFORWRITING),
                makeBuiltin(Base.STRING, Meta.MEDIATYPE)
        );

        addCommonMetadata(stringPrototype);
        addOptional(stringPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "String")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.STRING, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMENCODEDLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMENCODEDLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMENCODEDLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMENCODEDLENGTHFORWRITING),
                makeBuiltin(Base.STRING, Meta.MEDIATYPE)
        );

        addCommonMetadata(bitStringPrototype);
        addOptional(bitStringPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "BitString")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.COLLECTION, Meta.NAMEDBITS, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Bit")),
                makeBuiltin(Base.BITSTRING, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.UNSIGNED, Meta.LENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMLENGTHFORWRITING)
        );

        addCommonMetadata(enumeratedPrototype);
        addOptional(enumeratedPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Unsigned")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.ENUMERATED, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUM),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUM),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMLENGTHFORWRITING)
        );

        addCommonMetadata(datePrototype);
        addOptional(datePrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Date")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.DATE, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.DATE, Meta.MINIMUM),
                makeBuiltin(Base.DATE, Meta.MAXIMUM),
                makeBuiltin(Base.DATE, Meta.MINIMUMLENGTHFORWRITING),
                makeBuiltin(Base.DATE, Meta.MAXIMUMLENGTHFORWRITING),
                makeBuiltin(Base.BOOLEAN, Meta.UNSPECIFIEDVALUE)
        );

        addCommonMetadata(datePatternPrototype);
        addOptional(datePatternPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "DatePattern")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.DATEPATTERN, Meta.RELINQUISHDEFAULT)
        );

        addCommonMetadata(dateTimePrototype);
        addOptional(dateTimePrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "DateTime")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.DATETIME, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.DATETIME, Meta.MINIMUM),
                makeBuiltin(Base.DATETIME, Meta.MAXIMUM),
                makeBuiltin(Base.DATETIME, Meta.MINIMUMLENGTHFORWRITING),
                makeBuiltin(Base.DATETIME, Meta.MAXIMUMLENGTHFORWRITING),
                makeBuiltin(Base.BOOLEAN, Meta.UNSPECIFIEDVALUE)
        );

        addCommonMetadata(dateTimePatternPrototype);
        addOptional(dateTimePatternPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "DateTimePatern")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.DATETIMEPATTERN, Meta.RELINQUISHDEFAULT)
        );

        addCommonMetadata(timePrototype);
        addOptional(timePrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Time")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.TIME, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.TIME, Meta.MINIMUM),
                makeBuiltin(Base.TIME, Meta.MAXIMUM),
                makeBuiltin(Base.TIME, Meta.MINIMUMLENGTHFORWRITING),
                makeBuiltin(Base.TIME, Meta.MAXIMUMLENGTHFORWRITING),
                makeBuiltin(Base.BOOLEAN, Meta.UNSPECIFIEDVALUE)
        );

        addCommonMetadata(timePatternPrototype);
        addOptional(timePatternPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "TimePatern")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.TIMEPATTERN, Meta.RELINQUISHDEFAULT)
        );

        addCommonMetadata(objectIdentifierPrototype);
        addOptional(objectIdentifierPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "ObjectIdentifier")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.OBJECTIDENTIFIER, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUM),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUM),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMLENGTHFORWRITING),
                makeBuiltin(Base.STRING, Meta.OBJECTTYPE),
                makeBuiltin(Base.BOOLEAN, Meta.UNSPECIFIEDVALUE)
        );

        addCommonMetadata(objectIdentifierPatternPrototype);
        addOptional(objectIdentifierPatternPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "ObjectIdentifierPattern")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.OBJECTIDENTIFIERPATTERN, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUM),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUM),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMLENGTHFORWRITING),
                makeBuiltin(Base.STRING, Meta.OBJECTTYPE)
        );

        addCommonMetadata(weekNDayPrototype);
        addOptional(weekNDayPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "WeekNDay")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.WEEKNDAY, Meta.RELINQUISHDEFAULT)
        );

        addCommonMetadata(sequencePrototype);
        addOptional(sequencePrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Sequence")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.SEQUENCE, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.STRING, Meta.MEMBERTYPE),
                makeBuiltin(Base.LIST, Meta.MEMBERTYPEDEFINITION, makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE, 1))
        );

        addCommonMetadata(arrayPrototype);
        addOptional(arrayPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Array")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.ARRAY, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.STRING, Meta.MEMBERTYPE),
                makeBuiltin(Base.LIST, Meta.MEMBERTYPEDEFINITION, makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE, 1)),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMSIZE),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZEFORWRITING)
        );

        addCommonMetadata(listPrototype);
        addOptional(listPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "List")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.LIST, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.STRING, Meta.MEMBERTYPE),
                makeBuiltin(Base.LIST, Meta.MEMBERTYPEDEFINITION, makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE, 1)),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMSIZE),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZEFORWRITING)
        );

        addCommonMetadata(sequenceOfPrototype);
        addOptional(sequenceOfPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "SequenceOf")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.SEQUENCEOF, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.STRING, Meta.MEMBERTYPE),
                makeBuiltin(Base.LIST, Meta.MEMBERTYPEDEFINITION, makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE, 1)),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMSIZE),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZEFORWRITING)
        );

        addCommonMetadata(choicePrototype);
        addOptional(choicePrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Choice")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.CHOICE, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.COLLECTION, Meta.CHOICES),
                makeBuiltin(Base.STRING, Meta.ALLOWEDCHOICES)
        );

        addCommonMetadata(objectPrototype);

        addCommonMetadata(bitPrototype);
        addOptional(bitPrototype,
                makeBuiltin(Base.UNSIGNED, Meta.BIT)
        );

        addCommonMetadata(linkPrototype);
        addOptional(linkPrototype,
                makeBuiltin(Base.STRING, Meta.MEDIATYPE),
                makeBuiltin(Base.STRING, Meta.REPRESENTS),
                makeBuiltin(Base.STRING, Meta.REL)
        );

        addCommonMetadata(stringSetPrototype);
        addOptional(stringSetPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "StringSet")),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMENCODEDLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMENCODEDLENGTH),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMENCODEDLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMENCODEDLENGTHFORWRITING)
        );

        addCommonMetadata(compositionPrototype);

        addCommonMetadata(collectionPrototype);
        addOptional(collectionPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Collection")),
                makeBuiltin(Base.STRING, Meta.MEMBERTYPE),
                makeBuiltin(Base.LIST, Meta.MEMBERTYPEDEFINITION, makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE, 1)),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMSIZE),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZEFORWRITING)
        );

        addCommonMetadata(unknownPrototype);
        addOptional(unknownPrototype,
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES, makeBuiltin(Base.STRING, Meta.MEMBERTYPE, "Unknown")),
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.UNKNOWN, Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.STRING, Meta.MEMBERTYPE),
                makeBuiltin(Base.LIST, Meta.MEMBERTYPEDEFINITION, makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE, 1)),
                makeBuiltin(Base.UNSIGNED, Meta.MINIMUMSIZE),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE),
                makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZEFORWRITING)
        );

        addOptional(rawPrototype,
                makeBuiltin(Base.ARRAY, Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.ARRAY, Meta.RELINQUISHDEFAULT)
        );

        addCommonMetadata(polyPrototype);
        addOptional(polyPrototype,
                // this is completely free-form no-rules data.
                // usually used to hold parsed data, especially like that from JSON that might not know its $base.
                // things like $minimum are Strings here because we don't know what kind they actually are.
                // all collections are declared with $memberType UNDEFINED because the default of ANY is actually more restrictive.
                makeBuiltin(Base.COLLECTION, Meta.NAMEDVALUES),
                makeBuiltin(Base.ARRAY,      Meta.PRIORITYARRAY, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetPriorityArray")),
                makeBuiltin(Base.POLY,       Meta.RELINQUISHDEFAULT),
                makeBuiltin(Base.STRING,     Meta.ALLOWEDTYPES),
                makeBuiltin(Base.POLY,       Meta.MINIMUM),
                makeBuiltin(Base.POLY,       Meta.MAXIMUM),
                makeBuiltin(Base.POLY,       Meta.MINIMUMFORWRITING),
                makeBuiltin(Base.POLY,       Meta.MAXIMUMFORWRITING),
                makeBuiltin(Base.POLY,       Meta.RESOLUTION),
                makeBuiltin(Base.ENUMERATED, Meta.UNITS, makeBuiltin(Base.STRING, Meta.TYPE, "0-BACnetEngineeringUnits")),
                makeBuiltin(Base.STRING,     Meta.UNITSTEXT),
                makeBuiltin(Base.STRING,     Meta.MEDIATYPE),
                makeBuiltin(Base.BOOLEAN,    Meta.UNSPECIFIEDVALUE),
                makeBuiltin(Base.STRING,     Meta.OBJECTTYPE),
                makeBuiltin(Base.STRING,     Meta.MEMBERTYPE),
                makeBuiltin(Base.LIST,       Meta.MEMBERTYPEDEFINITION, makeBuiltin(Base.UNSIGNED, Meta.MAXIMUMSIZE, 1)),
                makeBuiltin(Base.COLLECTION, Meta.CHOICES),
                makeBuiltin(Base.STRING,     Meta.ALLOWEDCHOICES),
                makeBuiltin(Base.UNSIGNED,   Meta.BIT),
                makeBuiltin(Base.UNSIGNED,   Meta.MINIMUMLENGTH),
                makeBuiltin(Base.UNSIGNED,   Meta.MAXIMUMLENGTH),
                makeBuiltin(Base.UNSIGNED,   Meta.MINIMUMSIZE),
                makeBuiltin(Base.UNSIGNED,   Meta.MAXIMUMSIZE),
                makeBuiltin(Base.UNSIGNED,   Meta.MAXIMUMSIZEFORWRITING),
                makeBuiltin(Base.UNSIGNED,   Meta.MINIMUMLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED,   Meta.MAXIMUMLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED,   Meta.MINIMUMENCODEDLENGTH),
                makeBuiltin(Base.UNSIGNED,   Meta.MAXIMUMENCODEDLENGTH),
                makeBuiltin(Base.UNSIGNED,   Meta.MINIMUMENCODEDLENGTHFORWRITING),
                makeBuiltin(Base.UNSIGNED,   Meta.MAXIMUMENCODEDLENGTHFORWRITING)
        );

        initialized = true;
    }

    public static Data findPrototypeOfBase(String name) {
        Base base = Base.fromString(name);
        if (base == Base.INVALID) return null;
        return getPrototypeOfBase(base);
    }

    public static Data getPrototypeOfBase(Base base) { // builtins are also valid as prototypes
        switch (base) {
            case NULL:                   return nullPrototype;
            case BOOLEAN:                return booleanPrototype;
            case UNSIGNED:               return unsignedPrototype;
            case INTEGER:                return integerPrototype;
            case REAL:                   return realPrototype;
            case DOUBLE:                 return doublePrototype;
            case OCTETSTRING:            return octetStringPrototype;
            case STRING:                 return stringPrototype;
            case BITSTRING:              return bitStringPrototype;
            case ENUMERATED:             return enumeratedPrototype;
            case DATE:                   return datePrototype;
            case DATEPATTERN:            return datePatternPrototype;
            case DATETIME:               return dateTimePrototype;
            case DATETIMEPATTERN:        return dateTimePatternPrototype;
            case TIME:                   return timePrototype;
            case TIMEPATTERN:            return timePatternPrototype;
            case OBJECTIDENTIFIER:       return objectIdentifierPrototype;
            case OBJECTIDENTIFIERPATTERN:return objectIdentifierPatternPrototype;
            case WEEKNDAY:               return weekNDayPrototype;
            case SEQUENCE:               return sequencePrototype;
            case ARRAY:                  return arrayPrototype;
            case LIST:                   return listPrototype;
            case SEQUENCEOF:             return sequenceOfPrototype;
            case CHOICE:                 return choicePrototype;
            case OBJECT:                 return objectPrototype;
            case BIT:                    return bitPrototype;
            case LINK:                   return linkPrototype;
            case ANY:                    return anyPrototype;
            case STRINGSET:              return stringSetPrototype;
            case COMPOSITION:            return compositionPrototype;
            case COLLECTION:             return collectionPrototype;
            case UNKNOWN:                return unknownPrototype;
            case RAW:                    return rawPrototype;
            case POLY:                   return polyPrototype;
        }
        throw new XDError("Can't find prototype for base type "+base);
    }

}
