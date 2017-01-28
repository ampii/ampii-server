// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data;

/**
 * This is the list of standard Metadata names (to prevent typos!).
 *
 * @author daverobin
 */
public class Meta {
    
    public static final String NAME                           = "$name";
    public static final String VALUE                          = "$value";
    public static final String BASE                           = "$base";
    public static final String TYPE                           = "$type";
    public static final String EXTENDS                        = "$extends";
    public static final String OVERLAYS                       = "$overlays";
    public static final String MEMBERTYPE                     = "$memberType";
    public static final String WRITABLE                       = "$writable";
    public static final String OPTIONAL                       = "$optional";
    public static final String UNITS                          = "$units";
    public static final String MINIMUM                        = "$minimum";
    public static final String MAXIMUM                        = "$maximum";
    public static final String RESOLUTION                     = "$resolution";
    public static final String MINIMUMLENGTH                  = "$minimumLength";
    public static final String MAXIMUMLENGTH                  = "$maximumLength";
    public static final String MINIMUMSIZE                    = "$minimumSize";
    public static final String MAXIMUMSIZE                    = "$maximumSize";
    public static final String MAXIMUMSIZEFORWRITING          = "$org.ampii.maximumSizeForWriting"; // set to 0 for clearable-only
    public static final String MINIMUMSIZEFORWRITING          = "$org.ampii.minimumSizeForWriting"; // set to 1 to ensure non-empty
    public static final String VARIABILITY                    = "$variability";
    public static final String VOLATILITY                     = "$volatility";
    public static final String WRITEEFFECTIVE                 = "$writeEffective";
    public static final String ALLOWEDTYPES                   = "$allowedTypes";
    public static final String ALLOWEDCHOICES                 = "$allowedChoices";
    public static final String DISPLAYNAME                    = "$displayName";
    public static final String DISPLAYORDER                   = "$displayOrder";
    public static final String DESCRIPTION                    = "$description";
    public static final String COMMENT                        = "$comment";
    public static final String LENGTH                         = "$length";
    public static final String ERROR                          = "$error";
    public static final String ABSENT                         = "$absent";
    public static final String CONTEXTTAG                     = "$contextTag";
    public static final String PROPERTYIDENTIFIER             = "$propertyIdentifier";
    public static final String COMMANDABLE                    = "$commandable";
    public static final String BIT                            = "$bit";
    public static final String READABLE                       = "$readable";
    public static final String MINIMUMFORWRITING              = "$minimumForWriting";
    public static final String MAXIMUMFORWRITING              = "$maximumForWriting";
    public static final String MINIMUMLENGTHFORWRITING        = "$minimumLengthForWriting";
    public static final String MAXIMUMLENGTHFORWRITING        = "$maximumLengthForWriting";
    public static final String MINIMUMENCODEDLENGTH           = "$minimumEncodedLength";
    public static final String MAXIMUMENCODEDLENGTH           = "$maximumEncodedLength";
    public static final String MINIMUMENCODEDLENGTHFORWRITING = "$minimumEncodedLengthForWriting";
    public static final String MAXIMUMENCODEDLENGTHFORWRITING = "$maximumEncodedLengthForWriting";
    public static final String ASSOCIATEDWITH                 = "$associatedWith";
    public static final String REQUIREDWITH                   = "$requiredWith";
    public static final String REQUIREDWITHOUT                = "$requiredWithout";
    public static final String NOTPRESENTWITH                 = "$notPresentWith";
    public static final String WRITABLEWHEN                   = "$writableWhen";
    public static final String REQUIREDWHEN                   = "$requiredWhen";
    public static final String WRITABLEWHENTEXT               = "$writableWhenText";
    public static final String REQUIREDWHENTEXT               = "$requiredWhenText";
    public static final String TARGET                         = "$target";
    public static final String TARGETTYPE                     = "$targetType";
    public static final String HREF                           = "$href";
    public static final String DOCUMENTATION                  = "$documentation";
    public static final String ERRORTEXT                      = "$errorText";
    public static final String UNITSTEXT                      = "$unitsText";
    public static final String PRIORITYARRAY                  = "$priorityArray";
    public static final String DISPLAYNAMEFORWRITING          = "$displayNameForWriting";
    public static final String HASHISTORY                     = "$hasHistory";
    public static final String HISTORY                        = "$history";
    public static final String UNSPECIFIEDVALUE               = "$unspecifiedValue";
    public static final String NAMEDVALUES                    = "$namedValues";
    public static final String NAMEDBITS                      = "$namedBits";
    public static final String CHOICES                        = "$choices";
    public static final String MEMBERTYPEDEFINITION           = "$memberTypeDefinition";
    public static final String LINKS                          = "$links";
    public static final String TAGS                           = "$tags";
    public static final String NOTFORREADING                  = "$notForReading";
    public static final String NOTFORWRITING                  = "$notForWriting";
    public static final String PUBLISHED                      = "$published";
    public static final String ISMULTILINE                    = "$isMultiline";
    public static final String TRUNCATED                      = "$truncated";
    public static final String INALARM                        = "$inAlarm";
    public static final String OVERRIDDEN                     = "$overridden";
    public static final String FAULT                          = "$fault";
    public static final String OUTOFSERVICE                   = "$outOfService";
    public static final String NODETYPE                       = "$nodeType";
    public static final String NODESUBTYPE                    = "$nodeSubtype";
    public static final String COUNT                          = "$count";
    public static final String MEDIATYPE                      = "$mediaType";
    public static final String AUTHREAD                       = "$authRead";
    public static final String AUTHWRITE                      = "$authWrite";
    public static final String AUTHVISIBLE                    = "$authVisible";
    public static final String CHILDREN                       = "$children";
    public static final String DESCENDANTS                    = "$descendants";
    public static final String RELINQUISHDEFAULT              = "$relinquishDefault";
    public static final String ETAG                           = "$etag";
    public static final String NEXT                           = "$next";
    public static final String SELF                           = "$self";
    public static final String VIA                            = "$via";
    public static final String PHYSICAL                       = "$physical";
    public static final String RELATED                        = "$related";
    public static final String ALTERNATE                      = "$alternate";
    public static final String UPDATED                        = "$updated";
    public static final String AUTHOR                         = "$author";
    public static final String EDIT                           = "$edit";
    public static final String FAILURES                       = "$failures";
    public static final String SUBSCRIPTION                   = "$subscription";
    public static final String ID                             = "$id";
    public static final String SOURCEID                       = "$sourceId";
    public static final String ADDREV                         = "$addRev";
    public static final String REMREV                         = "$remRev";
    public static final String MODREV                         = "$modRev";
    public static final String DATAREV                        = "$dataRev";
    public static final String REVISIONS                      = "$revisions";
    public static final String VALUETAGS                      = "$valueTags";
    public static final String PARTIAL                        = "$partial";
    public static final String OBJECTTYPE                     = "$objectType";
    public static final String REPRESENTS                     = "$represents";
    public static final String VIAEXTERNAL                    = "$viaExternal";
    public static final String VIAMAP                         = "$viaMap";
    public static final String REL                            = "$rel";

    // these are for internal use only.
    // starting with '..' means they won't leak out of the box (and we don't have to prefix them with 'org.ampii')
    public static final String AMPII_BINDING                  = "$..binding";
    public static final String AMPII_HISTORY_LOCATION         = "$..historyLocation";
    public static final String AMPII_DEFINITIONS              = "$org.ampii.ui.definitions";    // special case for making definitions in the playground
    public static final String AMPII_TAG_DEFINITIONS          = "$org.ampii.ui.tagDefinitions"; // special case for making tag definitions in the playground
    public static final String AMPII_MATCH_ANY                = "$..matchAny";       // used by tests to match any received value


}
