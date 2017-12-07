// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.StringSet;
import org.ampii.xd.common.XDException;
import org.ampii.xd.server.Request;

/**
 * A static collection of all the miscellaneous "rules" imposed by the standard.
 */
public class Rules
{
    public static boolean isHidden(Data data)     { return isHidden(data.getName()); }
    public static boolean isHidden(String name)   { return name.startsWith("..") || name.startsWith("$.."); }
    public static boolean isMetadata(Data   data) { return data.getName().startsWith("$"); }
    public static boolean isChild(Data   data)    { return !data.getName().startsWith("$"); }
    public static boolean isMetadata(String name) { return name.startsWith("$"); }
    public static boolean isChild(String name)    { return !name.startsWith("$"); }

    public static boolean isExtendedMetadata(Data data) { return isExtendedMetadata(data.getName()); }
    public static boolean isExtendedMetadata(String name) {
        // must be like "$555-foo", "$org.foo.blah", or "$..internalThings", and NOT "$blah" or "$$blah' (shortest possible is "$1-x")
        return name.length()>3 && name.charAt(0)=='$' && name.charAt(1)!='$' && (name.contains("-") || name.contains("."));
    }
    
    public static boolean isStandardMetadata(Data data) { return isStandardMetadata(data.getName()); }
    public static boolean isStandardMetadata(String name) {
        // must be like "$blah" and NOT "$555-foo", "$org.foo.blah", "$..internalThings", or "$$blah" (shortest possible is "$x")
        return name.length()>1 && name.charAt(0)=='$' && name.charAt(1)!='$' && !(name.contains("-") || name.contains("."));
    }

    public static boolean isServerSpecificMetadata(String name) {
        // must be like "$..blah"
        return name.startsWith("$..");
    }

    public static boolean inheritsFromDefinition(String name) {
        return !doesNotInheritFromDefinition.containsComponent(name); // all others, including proprietary extensions, are inherited
    }

    public static boolean isNeverFilteredOut(String name) {
        return neverFilteredOut.containsComponent(name);
    }

    public static boolean inheritsFromParent(String name) {
        return inheritsFromParents.containsComponent(name); // all others, including proprietary extensions, are inherited
    }

    public static boolean isNotAllowedInInstances(String name) {return notAllowedInInstances.containsComponent(name); }

    public static boolean notAllowedForPut(String name) {
        return notAllowedForPut.containsComponent(name);
    }

    public static boolean notAllowedForPost(String name) {
        return notAllowedForPost.containsComponent(name);
    }

    public static Data    findBasisForRelativeLink(Data link) {
        // try to find the first ancestor of the link that is not a collection of some kind
        Data result = link;
        for(;;) {
            if (!result.hasParent()) break; // if we ran out of ancestors, give up
            result = result.getParent();
            Base base = result.getBase(); // we skip over collections of links, collections of collections of links, etc.
            if (!(base == Base.COLLECTION || base == Base.LIST || base == Base.ARRAY || base == Base.SEQUENCEOF)) break;
        }
        return result;
    }

    public static String  getNextAvailableChildName(Data target, String prefix) throws XDException {
        switch (target.getBase()) {
            case ARRAY:
            case LIST:
            case SEQUENCEOF:
            case UNKNOWN:
                if (target.booleanValueOf(Meta.PARTIAL, false)) { // if data is marked $partial, then the enforced numbering is relaxed
                    if (prefix.isEmpty()) return Integer.toString(target.getCount() + 1);
                    try { Integer.parseInt(prefix); } catch (NumberFormatException e) { throw new XDException(Errors.CANNOT_CREATE, target,"Invalid name '"+prefix+"'; Base type "+target.getBase()+" requires numeric member names");}
                    if (target.find(prefix)==null) return prefix; // if partial and number is not already used, then allow it.
                    else throw new XDException(Errors.CANNOT_CREATE, target,"Cannot reuse name of existing data member '"+prefix+"'"); // else complain
                }
                else return Integer.toString(target.getCount() + 1);
            case POLY:
            case COLLECTION:
                // if we are not given a useful prefix, then use position as the default
                if (prefix.length() == 0) prefix = Integer.toString(target.getCount() + 1);
                // if a child does not exist with the prefix, then use it as is for the name
                if (target.find(prefix) == null) return prefix;
                int next = 1;
                for (Data child : target.getChildren()) {
                    if (child.getName() == null) continue; // how did this happen? addChild() should protect against this
                    if (!child.getName().startsWith(prefix)) continue;             // looking for foo but found bar
                    String remainder = child.getName().substring(prefix.length()); // found foo or foo...
                    if (remainder.isEmpty()) continue;                        // if was just foo then keep looking
                    char first = remainder.charAt(0);                         // find the first thing after foo
                    if (first == '_') remainder = remainder.substring(1);     // if it's _ it might be foo_nnn
                    if (remainder.isEmpty()) continue;                        // if was actually "foo_" then keep looking
                    first = remainder.charAt(0);                              // find the next char after the _
                    if (first < '0' || first > '9') continue;                 // if it's not a digit, never mind
                    try { //finally, we found a number to compare against
                        int existing = Integer.parseInt(remainder);
                        if (existing >= next) next = existing + 1;
                    } catch (NumberFormatException e) { } // if it's not actually a number, then never mind
                }
                return prefix.isEmpty() ? Integer.toString(next) : prefix + "_" + Integer.toString(next);
            default:
                return prefix;
        }
    }

    public static boolean canRenumberChildren(Base base) {
        switch (base) {
            case LIST: case SEQUENCEOF:
                return true;
            default:
                return false;
        }
    }

    public static void    validateGlobalRequestOptions(Request request) throws XDException { // check some broad rules before individual dispatch to GetHandler, PutHandler, etc.
        if (request.getHeader("Authorization") != null && !request.isTLS && !Application.allowUnsecuredAuth)
            throw new XDException(Errors.AUTH_INVALID, "Don't provide an 'Authorization' header without TLS!");
    }

    public static void    validateGetDataRequestOptions(Request request) throws XDException {
        // TODO much can be done here...
    }

    public static void    validatePutDataRequestOptions(Request request) throws XDException {
        // TODO much more can be done here...
        if (!request.getParameter("max-results","").equals("")) throw new XDException(Errors.INCONSISTENT_VALUES,"Can't include query parameter 'max-results' for a PUT");
        if (!request.getParameter("alt","").equals("plain")) { // if not plain, then you can't include 'skip'
            if (!request.getParameter("skip","").equals("")) throw new XDException(Errors.INCONSISTENT_VALUES,"Can't include query parameter 'skip' except with alt=plain");
        }
    }

    public static void    validatePostDataRequestOptions(Request request) throws XDException {
        // TODO much more can be done here...
        if (request.hasParameter("max-results") || request.hasParameter("skip")) throw new XDException(Errors.INCONSISTENT_VALUES,"Can't include query parameter 'max-results' or 'skip' for a PUT");
    }

    public static void validateDeleteDataRequestOptions(Request request) throws XDException {
        // TODO much can be done here...
    }

    ///////////////////////////////////////////////////////////////////////////
    
    public static final StringSet allMetadata = new StringSet(new String[] {
            Meta.NAME,
            Meta.VALUE,
            Meta.BASE,
            Meta.TYPE,
            Meta.EXTENDS,
            Meta.OVERLAYS,
            Meta.MEMBERTYPE,
            Meta.WRITABLE,
            Meta.OPTIONAL,
            Meta.UNITS,
            Meta.MINIMUM,
            Meta.MAXIMUM,
            Meta.RESOLUTION,
            Meta.MINIMUMLENGTH,
            Meta.MAXIMUMLENGTH,
            Meta.MINIMUMSIZE,
            Meta.MAXIMUMSIZE,
            Meta.MAXIMUMSIZEFORWRITING,
            Meta.VARIABILITY,
            Meta.VOLATILITY,
            Meta.WRITEEFFECTIVE,
            Meta.ALLOWEDTYPES,
            Meta.ALLOWEDCHOICES,
            Meta.DISPLAYNAME,
            Meta.DESCRIPTION,
            Meta.COMMENT,
            Meta.LENGTH,
            Meta.ERROR,
            Meta.ABSENT,
            Meta.CONTEXTTAG,
            Meta.PROPERTYIDENTIFIER,
            Meta.COMMANDABLE,
            Meta.BIT,
            Meta.READABLE,
            Meta.MINIMUMFORWRITING,
            Meta.MAXIMUMFORWRITING,
            Meta.MINIMUMLENGTHFORWRITING,
            Meta.MAXIMUMLENGTHFORWRITING,
            Meta.MINIMUMENCODEDLENGTH,
            Meta.MAXIMUMENCODEDLENGTH,
            Meta.MINIMUMENCODEDLENGTHFORWRITING,
            Meta.MAXIMUMENCODEDLENGTHFORWRITING,
            Meta.ASSOCIATEDWITH,
            Meta.REQUIREDWITH,
            Meta.REQUIREDWITHOUT,
            Meta.NOTPRESENTWITH,
            Meta.WRITABLEWHEN,
            Meta.REQUIREDWHEN,
            Meta.WRITABLEWHENTEXT,
            Meta.REQUIREDWHENTEXT,
            Meta.TARGET,
            Meta.HREF,
            Meta.DOCUMENTATION,
            Meta.ERRORTEXT,
            Meta.UNITSTEXT,
            Meta.PRIORITYARRAY,
            Meta.DISPLAYNAMEFORWRITING,
            Meta.HASHISTORY,
            Meta.HISTORY,
            Meta.UNSPECIFIEDVALUE,
            Meta.NAMEDVALUES,
            Meta.NAMEDBITS,
            Meta.CHOICES,
            Meta.MEMBERTYPEDEFINITION,
            Meta.LINKS,
            Meta.TAGS,
            Meta.NOTFORREADING,
            Meta.NOTFORWRITING,
            Meta.PUBLISHED,
            Meta.ISMULTILINE,
            Meta.TRUNCATED,
            Meta.INALARM,
            Meta.OVERRIDDEN,
            Meta.FAULT,
            Meta.OUTOFSERVICE,
            Meta.NODETYPE,
            Meta.COUNT,
            Meta.MEDIATYPE,
            Meta.AUTHREAD,
            Meta.AUTHWRITE,
            Meta.CHILDREN,
            Meta.DESCENDANTS,
            Meta.RELINQUISHDEFAULT,
            Meta.ETAG,
            Meta.NEXT,
            Meta.SELF,
            Meta.VIA,
            Meta.PHYSICAL,
            Meta.RELATED,
            Meta.ALTERNATE,
            Meta.UPDATED,
            Meta.AUTHOR,
            Meta.EDIT,
            Meta.FAILURES,
            Meta.SUBSCRIPTION,
            Meta.ID,
            Meta.SOURCEID,
            Meta.ADDREV,
            Meta.REMREV,
            Meta.MODREV,
            Meta.DATAREV,
            Meta.REVISIONS,
            Meta.TARGETTYPE,
            Meta.OBJECTTYPE,
            Meta.VALUETAGS,
            Meta.REPRESENTS,
            Meta.VIAEXTERNAL,
            Meta.VIAMAP,
            Meta.REL,
    });

    public static final StringSet alwaysMetadata = new StringSet(new String[] {  // can't be suppressed (except with nonstandard "-xxx" form)
            Meta.NAME,
            Meta.TRUNCATED,
            Meta.ETAG,
            Meta.NEXT,
            Meta.SELF,
            Meta.SUBSCRIPTION
    });

    public static final StringSet doesNotInheritFromDefinition = new StringSet(new String[] {
            Meta.EXTENDS,
            Meta.OVERLAYS,
            //Meta.OPTIONAL,
            Meta.ERROR,
            Meta.ABSENT,
            Meta.ERRORTEXT,
            Meta.HASHISTORY,
            Meta.HISTORY,
            Meta.TRUNCATED,
            Meta.INALARM,
            Meta.OVERRIDDEN,
            Meta.FAULT,
            Meta.OUTOFSERVICE,
            Meta.COUNT,
            Meta.CHILDREN,
            Meta.DESCENDANTS,
            Meta.ETAG,
            Meta.NEXT,
            Meta.SELF,
            Meta.UPDATED,
            Meta.AUTHOR,
            Meta.EDIT,
            Meta.FAILURES,
            Meta.SUBSCRIPTION,
            Meta.ID,
            Meta.SOURCEID,
    });

    public static final StringSet inheritsFromParents = new StringSet(new String[] {
            Meta.WRITABLE,
            Meta.VARIABILITY,
            Meta.VOLATILITY,
            Meta.WRITEEFFECTIVE,
            Meta.ERROR,
            Meta.ABSENT,
            Meta.READABLE,
            Meta.ERRORTEXT,
            Meta.PUBLISHED,
            Meta.AUTHREAD,
            Meta.AUTHWRITE,
            Meta.AUTHVISIBLE,
            Meta.UPDATED,
            Meta.AUTHOR
    });

    public static boolean parentallyInheritedIsDifferentFromDefault(Data data, String metaName) throws XDException {
        Data effVal = data.findEffective(metaName);
        if (effVal != null) {
            switch (metaName) {
                // these are true by default
                case Meta.AUTHVISIBLE:
                case Meta.READABLE:       return !effVal.booleanValue();
                // these are false by default
                case Meta.ABSENT:
                case Meta.WRITABLE:       return effVal.booleanValue();
                // these are null by default
                case Meta.VARIABILITY:
                case Meta.ERROR: // (zero is a valid error value, so null is the default)
                case Meta.ERRORTEXT:
                case Meta.PUBLISHED:
                case Meta.UPDATED:
                case Meta.AUTHOR:
                case Meta.AUTHREAD:
                case Meta.AUTHWRITE:      return effVal.hasValue();
            }

        }
        return false;
    }


    public static final StringSet neverFilteredOut = new StringSet(new String[] {
            Meta.PARTIAL,
            Meta.NEXT,
            Meta.ABSENT,
            Meta.FAILURES,
            Meta.HREF,
            Meta.SELF,
            Meta.SUBSCRIPTION,
            Meta.TRUNCATED,
            Meta.HREF,
    });

    public static final StringSet typeMetadata = new StringSet(new String[] {
            Meta.BASE,
            Meta.TYPE,
            Meta.EXTENDS,
            Meta.MEMBERTYPE,
            Meta.MEMBERTYPEDEFINITION,
            Meta.OVERLAYS,
            Meta.ALLOWEDTYPES,
            Meta.ALLOWEDCHOICES,
            Meta.OPTIONAL,
            Meta.ABSENT,
            Meta.HREF,
            Meta.TARGETTYPE,
            Meta.OBJECTTYPE,
    });

    public static final StringSet tagMetadata =  new StringSet(new String[] {
            Meta.NODETYPE,
            Meta.TAGS,
            Meta.VALUETAGS,
            Meta.HREF
    });
    
    public static final StringSet linkMetadata =  new StringSet(new String[] {
            Meta.LINKS,
            Meta.SELF,
            Meta.EDIT,
            Meta.ALTERNATE,
            Meta.VIA,
            Meta.RELATED,
            Meta.PHYSICAL,
            Meta.NEXT,
            Meta.SUBSCRIPTION,
            Meta.HREF,
            Meta.REPRESENTS,
            Meta.VIAEXTERNAL,
            Meta.VIAMAP,
            Meta.REL,
            Meta.MEDIATYPE,
    });

    public static final StringSet uiMetadata =  new StringSet(new String[] {
            Meta.DISPLAYNAME,
            Meta.DISPLAYNAMEFORWRITING,
            Meta.DESCRIPTION,
            Meta.COMMENT,
            Meta.HREF
    });

    public static final StringSet docMetadata =  new StringSet(new String[] {
            Meta.DOCUMENTATION,
            Meta.HREF
    });

    public static final StringSet dataMetadata =  new StringSet(new String[] {
            Meta.BASE,
            Meta.WRITABLE,
            Meta.READABLE,
            Meta.OPTIONAL,
            Meta.ABSENT,
            Meta.UNITS,
            Meta.UNITSTEXT,
            Meta.VOLATILITY,
            Meta.VARIABILITY,
            Meta.MINIMUM,
            Meta.MAXIMUM,
            Meta.MINIMUMFORWRITING,
            Meta.MAXIMUMFORWRITING,
            Meta.RESOLUTION,
            Meta.MINIMUMLENGTH,
            Meta.MAXIMUMLENGTH,
            Meta.MINIMUMENCODEDLENGTH,
            Meta.MAXIMUMENCODEDLENGTH,
            Meta.MINIMUMLENGTHFORWRITING,
            Meta.MAXIMUMLENGTHFORWRITING,
            Meta.MINIMUMENCODEDLENGTHFORWRITING,
            Meta.MAXIMUMENCODEDLENGTHFORWRITING,
            Meta.MINIMUMSIZE,
            Meta.MAXIMUMSIZE,
            Meta.MAXIMUMSIZEFORWRITING,
            Meta.ASSOCIATEDWITH,
            Meta.REQUIREDWITH,
            Meta.REQUIREDWITHOUT,
            Meta.NOTPRESENTWITH,
            Meta.NOTFORREADING,
            Meta.NOTFORWRITING,
            Meta.NAMEDVALUES,
            Meta.NAMEDBITS,
            Meta.BIT,
            Meta.WRITEEFFECTIVE,
            Meta.ISMULTILINE,
            Meta.WRITABLEWHEN,
            Meta.WRITABLEWHENTEXT,
            Meta.REQUIREDWHEN,
            Meta.REQUIREDWHENTEXT,
            Meta.CONTEXTTAG,
            Meta.PROPERTYIDENTIFIER,
            Meta.COMMANDABLE,
            Meta.HREF,
            Meta.ADDREV,
            Meta.REMREV,
            Meta.MODREV,
            Meta.DATAREV,
            Meta.REVISIONS
    });

    public static final StringSet authMetadata =  new StringSet(new String[] {
            Meta.AUTHREAD,
            Meta.AUTHWRITE,
            Meta.HREF
    });

    public static final StringSet changeMetadata =  new StringSet(new String[] {
            Meta.PUBLISHED,
            Meta.UPDATED,
            Meta.AUTHOR,
            Meta.HREF
    });

    public static final StringSet valueMetadata =  new StringSet(new String[] {
            Meta.VALUE,
            Meta.ERROR,
            Meta.ERRORTEXT,
            Meta.UNSPECIFIEDVALUE,
            Meta.LENGTH,
            Meta.INALARM,
            Meta.OVERRIDDEN,
            Meta.FAULT,
            Meta.OUTOFSERVICE,
            Meta.TRUNCATED,
            Meta.MEDIATYPE,
            Meta.ETAG,
            Meta.PRIORITYARRAY,
            Meta.RELINQUISHDEFAULT,
            Meta.FAILURES,
            Meta.HREF,
    });

    public static final StringSet notAllowedForPut = new StringSet(new String[] {
            // the standard says that if computed metadata, 'count', 'children', 'descendants', 'truncated', 'history',
            // 'etag', 'next', 'self', 'edit', 'failures', 'subscription', or 'id' are present in data given to a server, then
            // a WS_ERR_VALUE_FORMAT error shall be indicated.
            Meta.COUNT,
            Meta.CHILDREN,
            Meta.DESCENDANTS,
            Meta.TRUNCATED,
            Meta.HISTORY,
            Meta.ETAG,
            Meta.NEXT,
            Meta.SELF,
            Meta.EDIT,
            Meta.FAILURES,
            Meta.SUBSCRIPTION,
            Meta.ID,
    });
    public static final StringSet notAllowedForPost = new StringSet(new String[] {
            // this is just like the list for PUT except that we allow 'subscription' for COV POSTs
            Meta.COUNT,
            Meta.CHILDREN,
            Meta.DESCENDANTS,
            Meta.TRUNCATED,
            Meta.HISTORY,
            Meta.ETAG,
            Meta.NEXT,
            Meta.SELF,
            Meta.EDIT,
            Meta.FAILURES,
            Meta.ID,
    });

    public static final StringSet notAllowedInInstances = new StringSet(new String[] {
            Meta.EXTENDS,
            Meta.OVERLAYS,
            Meta.OPTIONAL,
            Meta.MEMBERTYPEDEFINITION,
            Meta.NOTFORREADING,
            Meta.NOTFORWRITING,
            Meta.ADDREV,
            Meta.REMREV,
            Meta.MODREV,
            Meta.DATAREV,
            Meta.REVISIONS,
    });


}
