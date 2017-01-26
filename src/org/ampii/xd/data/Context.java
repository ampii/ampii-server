// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data;

import org.ampii.xd.client.Client;
import org.ampii.xd.common.*;
import org.ampii.xd.database.Session;
import org.ampii.xd.data.basetypes.CollectionData;
import org.ampii.xd.database.DataStore;
import org.ampii.xd.marshallers.Parser;
import org.ampii.xd.resolver.Eval;
import org.ampii.xd.security.Authorizer;
import org.ampii.xd.server.HTTP;
import org.ampii.xd.server.Request;

import javax.xml.bind.DatatypeConverter;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.*;

/**
 * A Context is the holder of a user's query-specific parameters, usually from an HTTP request, but can also be
 * used by internal callers to affect authorization and marshalling for various purposes.
 * <p>
 * It also provides the default filtering/selecting code for operating on query parameters.
 * <p>
 * This default code can be overridden by a Binding's getContextualizedXxxx() methods, but that overriding code must
 * then take into account *all* the appropriate context parameters, as the default code does.  So it will have to
 * duplicate the functionality of the getContextualizedXxxx() here. To make that easier, it can call public helpers
 * in this class like filterChildren(), canIncludeChild(), etc.
 * <p>
 * A Context is usually appended to the "session root" (see {@link Session}, which is the root of the data tree that is
 * handed around to various HTTP handlers. The context from that root is retrievable from any data under that root with
 * getContext().
 *
 * @author drobin
 */
public class Context {

    private Request            request;        // the HTTP request; will be null for internal operations
    private Data               target;         // this is the target of the operation, i.e., the last segment of a URI
    private String             description;
    private Authorizer         authorizer = new Authorizer(); // defaults to inGodMode(), see Authorizer class
    private Calendar           published_ge;
    private Calendar           published_gt;
    private Calendar           published_le;
    private Calendar           published_lt;
    private Integer            sequence_ge;
    private Integer            sequence_gt;
    private Integer            sequence_le;
    private Integer            sequence_lt;
    private Integer            skip;
    private Integer            max_results;
    private Integer            depth;
    private Integer            descendantDepth;
    private Integer            priority;
    private Boolean            reverse;
    private String[]           filterFilter;      // for the 'filter' query parameter
    private String[]           selectFilter;      // for the 'select' query parameter (with no paths)
    //private List<String[]>   selectFilter;      // this is for the 'select' query parameter... IF PATHS ARE ALLOWED

    public boolean             hasMetadataFilter()                         { return metadataFilter != null; }
    public StringSet           getMetadataFilter()                         { return metadataFilter != null? metadataFilter : new StringSet(); }
    public void                setMetadataFilter(StringSet metadataFilter) { this.metadataFilter = metadataFilter; }

    private StringSet          metadataFilter;    // for the 'metadata' query parameter
    private boolean            metadataFilterIncludesExtensions;
    private boolean            metadataFilterIncludesDefinitions;
    private Object             external;          // external http environment, not used by core AMPII code (for when this is wrapped by another HTTP server)
    private String             locale;
    private String             alt = "json";      // default format, overridden with "alt" query parameter in QueryContext

    public int                 cur_inDefinition;     // counting semaphore used by inDefinitionContext(), enterDefinitionContext(), etc.
    public int                 cur_depth;            // changed as callers descend/ascend
    public int                 cur_descendantDepth;  // changed as callers descend/ascend
    public int                 cur_descendantNumber; // changed as descendant links are created
    public int                 cur_skip;             // changed (decremented) as callers attempt to add items
    public int                 cur_max;              // limit of children in results at top level (cur_depth==1)

    public Context() { this("none") ; } // makes context with no filtering

    public Context(String description) {  // makes context with no filtering
        this.description = description;
    }

    // for Server contexts: parses received query string and sets all the criteria for the Context, which is used by the Generators
    public Context(Request request) throws XDException {
        this.request = request;
        external = request.external; // for binding to external server environments (not used by core AMPII code)
        evaluateParameters(request.parameters);
        if (!alt.equals("plain")) locale = null; // if we are given a locale and not plain, we must ignore it.
    }

    // for Client contexts: parses given query string and sets all the criteria for the Context, which is used by the Generators
    public Context(String description, String clientParameters) throws XDException {
        this(description);
        evaluateParameters(HTTP.parseParameters(clientParameters));
    }

    public void     setTarget(Data target)    { this.target = target; }

    public boolean  isTarget(Data data)       { return data == target; }

    public boolean  isBelowTarget(Data data)  { // is my parent or other ancestor the target?
        for (data = data.getParent(); data != null; data = data.getParent())
            if (isTarget(data)) return true;
        return false;
    }

    public void    enterDefinitionContext() { cur_inDefinition++; }

    public void    exitDefinitionContext()  { cur_inDefinition--; }

    public boolean inDefinitionContext()    { return cur_inDefinition != 0; }

    public boolean canDescend()             { return cur_depth <= getDepth(); }

    public boolean canInclude(String meta)  { return metadataFilter == null || metadataFilter.containsComponent(meta); }

    public boolean canIncludeValue()        { return metadataFilter == null || metadataFilter.containsComponent(Meta.VALUE); }

    public boolean canIncludeBase()         { return metadataFilter == null || metadataFilter.containsComponent(Meta.BASE) || inDefinitionContext(); }

    public boolean canIncludeType()         { return metadataFilter == null || metadataFilter.containsComponent(Meta.TYPE) || inDefinitionContext(); }

    public boolean canIncludeDefinitions()  { return metadataFilterIncludesDefinitions; }

    public boolean canInclude(Data data) throws XDException {  return data.isChild() ? canIncludeChild(data) : canIncludeMetadata(data);  }

    public void resetCur() {
        cur_inDefinition    = 0;
        cur_depth           = 0;
        cur_descendantDepth = 0;
        cur_descendantNumber= 0;
        cur_skip            = skip        != null? skip : 0;
        cur_max             = max_results != null? max_results : Integer.MAX_VALUE;
    }

    public boolean    hasRequest()                           { return request != null; }
    public Request    getRequest()                           { return request != null? request : new Request(); }
    public void       setRequest(Request request)            { this.request = request; }

    public String     getDescription()                       { return description; }
    public void       setDescription(String description)     { this.description = description; }

    public boolean    hasAuthorizer()                        { return authorizer != null; }
    public Authorizer getAuthorizer()                        { return authorizer != null? authorizer : new Authorizer(); }
    public void       setAuthorizer(Authorizer authorizer)   { this.authorizer = authorizer;}

    public boolean    hasPublished_ge()                      { return published_ge != null; }
    public Calendar   getPublished_ge()                      { return published_ge != null ? published_ge : new GregorianCalendar(); }
    public void       setPublished_ge(Calendar published_ge) { this.published_ge = published_ge; }

    public boolean    hasPublished_gt()                      { return published_gt != null; }
    public Calendar   getPublished_gt()                      { return published_gt != null ? published_gt : new GregorianCalendar(); }
    public void       setPublished_gt(Calendar published_ge) { this.published_gt = published_gt; }

    public boolean    hasPublished_le()                      { return published_le != null; }
    public Calendar   getPublished_le()                      { return published_le != null ? published_le : new GregorianCalendar(); }
    public void       setPublished_le(Calendar published_ge) { this.published_le = published_le; }

    public boolean    hasPublished_lt()                      { return published_lt != null; }
    public Calendar   getPublished_lt()                      { return published_lt != null ? published_lt : new GregorianCalendar(); }
    public void       setPublished_lt(Calendar published_ge) { this.published_lt = published_lt; }

    public boolean    hasSequence_lt()                       { return sequence_lt != null; }
    public int        getSequence_lt()                       { return sequence_lt != null? sequence_lt : Integer.MAX_VALUE; }
    public void       setSequence_lt(Integer sequence_lt)    { this.sequence_lt = sequence_lt; }

    public boolean    hasSequence_le()                       { return sequence_le != null; }
    public int        getSequence_le()                       { return sequence_le != null? sequence_le : Integer.MAX_VALUE; }
    public void       setSequence_le(Integer sequence_le)    { this.sequence_le = sequence_le; }

    public boolean    hasSequence_gt()                       { return sequence_gt != null; }
    public int        getSequence_gt()                       { return sequence_gt != null? sequence_gt : 0; }
    public void       setSequence_gt(Integer sequence_gt)    { this.sequence_gt = sequence_gt; }

    public boolean    hasSequence_ge()                       { return sequence_ge != null; }
    public int        getSequence_ge()                       { return sequence_ge != null? sequence_ge : -1; }
    public void       setSequence_ge(Integer sequence_ge)    { this.sequence_ge = sequence_ge; }

    public boolean    hasSkip()                              { return  skip != null; }
    public int        getSkip()                              { return  skip != null? skip : 0; }
    public void       setSkip(Integer skip)                  { this.skip = skip; }

    public boolean    hasMaxResults()                        { return max_results != null; }
    public int        getMaxResults()                        { return max_results != null? max_results : Integer.MAX_VALUE; }
    public void       setMaxResults(Integer max_results)     { this.max_results = max_results; }

    public boolean    hasDepth()                             { return depth != null; }
    public int        getDepth()                             { return depth != null? depth : Integer.MAX_VALUE; }
    public void       setDepth(Integer depth)                { this.depth = depth; }

    // TODO do we want to set a limit on descendant depth if it is not specified?
    public boolean    hasDescendantDepth()                    { return descendantDepth != null; }
    public int        getDescendantDepth()                    { return descendantDepth != null? descendantDepth : Integer.MAX_VALUE ; }
    public void       setDescendantDepth(Integer descendantDepth) { this.descendantDepth = descendantDepth; }

    public boolean    hasPriority()                           { return priority != null; }
    public int        getPriority()                           { return priority != null? priority : 16 ; }
    public void       setPriority(Integer priority)           { this.priority = priority; }

    public boolean    hasReverse()                            { return reverse != null; }
    public boolean    getReverse()                            { return reverse != null? reverse : false ; }
    public void       setReverse(Boolean reverse)             {   this.reverse = reverse; }

    public boolean    hasLocale()                             { return locale != null && !locale.isEmpty();  }
    public String     getLocale()                             { return locale != null? locale : DataStore.getDatabaseLocaleString(); }
    public void       setLocale(String locale)                { this.locale = locale; }

    public boolean    hasExternal()                           { return external != null; }
    public Object     getExternal()                           { return external; } // can be null!
    public void       setExternal(Object external)            { this.external = external; }

    public boolean    isAlt(String test)                      { return alt.equals(test); }
    public String     getAlt()                                { return alt; }
    public void       setAlt(String alt)                      { this.alt = alt; }

    public DataList makeContextualizedChildren(Data data) throws XDException {
        DataList children = data.getChildren();
        if (getReverse()) Collections.reverse(children);
        return filterChildren(children.iterator(), data.getContext().isTarget(data));
    }

    public String makeContextualizedValue(Data data) throws XDException {
        // this is called if there is no binding to handle this explicitly and just makes a substring of existing local value.
        // chances are that this will mostly not be used because anyone doing a skip/max on a string or octet string is
        // probably doing it for something that has a large-value binding, like file data, and the binding will handle this.
        if (data.getBase() == Base.OCTETSTRING) {
            if (isTarget(data) && getAlt().equals("plain") && hasRequest() && getRequest().method.equals("GET")) {
                byte[] newValue = data.byteArrayValue();
                if ((hasSkip() || hasMaxResults())) {
                    int from = getSkip();
                    int to = hasMaxResults() ? from + getMaxResults() : newValue.length;
                    if (from < 0) throw new XDException(Errors.PARAM_OUT_OF_RANGE, "'skip' cannot be negative");
                    if (from > newValue.length) from = newValue.length;
                    if (to > newValue.length) to = newValue.length;
                    // if we are in fact returning a substring, then set appropriate things in view
                    if (from > 0 || to < newValue.length) newValue = Arrays.copyOfRange(newValue, from, to);
                }
                return DatatypeConverter.printHexBinary(newValue);
            }
        }
        if (data.getBase() == Base.STRING) {
            if (isTarget(data) && getAlt().equals("plain") && hasRequest() && getRequest().method.equals("GET")) {
                String newValue = data.stringValue();
                if ((hasSkip() || hasMaxResults())) {
                    int from = getSkip();
                    int to   = hasMaxResults() ? from + getMaxResults() : newValue.length();
                    if (from < 0) throw new XDException(Errors.PARAM_OUT_OF_RANGE, "'skip' cannot be negative");
                    if (from > newValue.length()) from = newValue.length();
                    if (to   > newValue.length()) to = newValue.length();
                    if (from > 0 || to < newValue.length()) newValue = newValue.substring(from, to);
                }
                return newValue;
            }
        }
        return data.stringValue();
    }


    public DataList makeContextualizedMetadata(Data data) throws XDException {
        // there is no filtering in definition context, otherwise it is guided by the 'metadata' query parameter
        return inDefinitionContext()? data.getMetadata() : makeFilteredMetadataList(data.getMetadata());
    }

    // helpers for the above

    public DataList filterChildren(Iterator<Data> iterator, boolean isTarget) throws XDException {
        DataList results = new DataList();
        int      skip    = getSkip();
        int      max     = getMaxResults();
        while (iterator.hasNext()) {  // iterate through candidate children from the data source and add to the results if they make the cut
            Data candidate = iterator.next();
            if (!authorizer.checkVisible(candidate) || Rules.isHidden(candidate)) continue; // invisible and hidden do not affect $partial
            if (isTarget) {                   // if we are the URI target data, then filter/skip/etc apply
                if (!canIncludeChild(candidate)) {  // if this candidate didn't make the cut, mark that we are partial, then try next candidate
                    results.partial = true;
                    continue;
                }
                if (skip > 0) {  // for every result that *would* go in the list, skip over the first 'skip-results', then try next candidate
                    skip--;
                    results.partial = true;
                    continue;
                }
                if (max == 0) {  // 'max-result' *could* start out 0 (dumb client or evil tester), so we have to check it first
                    results.next = Client.makeSimpleNextPointer(this);
                    results.partial = true;
                    break; // no reason in continue with next child
                }
                results.add(candidate);  // yay! someone made the cut!
                if (--max == 0) { // if we've now hit max, then don't bother to check any more.
                    results.next = Client.makeSimpleNextPointer(this);
                    results.partial = true;
                    break;
                }
            } else results.add(candidate);  // not target, so filter/skip/etc do not apply, just include everything that is not invisible or hidden
        }
        return results;
    }

    public DataList   makeFilteredMetadataList(DataList metadata) {
        DataList results = new DataList();          // start by assuming that none make the cut
        for (Data meta : metadata) {
            if (canIncludeMetadata(meta)) results.add(meta); // yay! the client wants me!
        }
        return results;
    }

    public boolean    canIncludeMetadata(Data meta)  {
        if (inDefinitionContext())                    return true;
        if (Rules.isNeverFilteredOut(meta.getName())) return true; // some things like $next are never filtered
        if (!authorizer.checkVisible(meta))           return false;
        if (Rules.isHidden(meta))                     return false; // "$..xxx" metadata is internal AMPII data, hidden from external view
        else {
            if (Rules.isStandardMetadata(meta)) {
                if (metadataFilter == null || metadataFilter.containsComponent(meta.getName())) return true;
            } else {  // proprietary (aka "extended" metadata)
                if (metadataFilterIncludesExtensions) return true;
            }
        }
        return false; // default is to not include metadata
    }

    public boolean canIncludeChild(Data child) throws XDException {
        if (inDefinitionContext())           return true;
        if (!authorizer.checkVisible(child)) return false;
        if (Rules.isHidden(child))           return false; // "..xxx" children are internal AMPII data, hidden from external view
        if (isTarget(child.getParent())) {   // if this is the first level of children under the target, then filter/select/range apply
            if (!isSelected(child))          return false;
            if (!isFiltered(child))          return false;
            if (published_ge != null || published_gt != null || published_le != null || published_lt != null) {
                Data publishedMetadata = child.find(Meta.PUBLISHED);
                if (publishedMetadata == null)
                    publishedMetadata = child.find("timestamp"); // if $published metadata not found, check for "timestamp" child
                if (publishedMetadata != null && publishedMetadata.getBase() == Base.DATETIME) {
                    Calendar published = publishedMetadata.calendarValue();
                    if (published_ge != null &&  published.before(published_ge))                                    return false;
                    if (published_gt != null && (published.before(published_gt) || published.equals(published_gt))) return false;
                    if (published_le != null &&  published.after(published_le))                                     return false;
                    if (published_lt != null && (published.after(published_lt)  || published.equals(published_lt))) return false;
                }
            }
            if (sequence_ge != null || sequence_gt != null || sequence_le != null || sequence_lt != null) {
                try {
                    int sequence = Integer.parseInt(child.getName());
                    if (sequence_ge != null && sequence <  sequence_ge) return false;
                    if (sequence_gt != null && sequence <= sequence_gt) return false;
                    if (sequence_le != null && sequence >  sequence_le) return false;
                    if (sequence_lt != null && sequence >= sequence_lt) return false;
                } catch (NumberFormatException e) { return false; } // if name is not numeric, don't include
            }
        }
        return true; // default is to include children
    }

    ///////////////////////////////////////////////////////////////////////////////
    //private

    private int     filterIndex;
    private Data    filterCandidate;


    // This code is for IF SELECT CAN CONTAIN PATHS (hopefully not!)
    // private boolean isSelected(Data candidate) {
    //    // This checks the depth of each select expression against the depth of the candidate data.
    //    // the candidates are filtered only at the level matching the select path depth
    //    // but  select=*/*/failure would select the 'failure' in any record (and filter out all peers to 'failure', like 'real-value')
    //    // This code actually allows non-* leading segments, but this has proven to lead to possibly non-intuitive results.
    //    // e.g. select=24220/log-datum/failure/error-class would not actually filter much. it only filters out peers of 'error-class' in that
    //    // *one* very specific path.  Since that select statement only applies to peers of 24220/log-datum/failure/error-class, peers
    //    // to "24220" are non-intuitively included since the selection didn't say anything about that level.
    //    // However, this syntax may yet have usefulness...
    //    // e.g. /.bacnet?select=*/*/*/present-value;*/4321/binary-value,3/active-text
    //    // returns the present value of all objects in all devices... *and* the active-text in one very specific object!
    //    // It remains to be seen if this utility is worth the possible confusion.
    //    if (selectFilter == null) return true;
    //    boolean depthWasFound = false;
    //    boolean childWasFound = false;
    //    for ( String[] segments : selectFilter) {
    //        //TODO$$$ this is broken
    //        if (segments.length == cur_depth) {   // if the filter applies to my level: i.e. {base}/child/grandchild applies only to grandchildren
    //            depthWasFound = true;
    //            Data temp = candidate;
    //            boolean allSegmentsMatch = true;
    //            for (int i=segments.length-1; i>=0; i--) {
    //                if (!(segments[i].equals("*") || segments[i].equals(Path.getPathName(temp)))) { allSegmentsMatch=false; break; }
    //                temp = temp.getParent();
    //                if (temp == null) { allSegmentsMatch=false; break; }
    //            }
    //            if (allSegmentsMatch) { childWasFound = true; break; }
    //        }
    //    }
    //    return !depthWasFound || childWasFound;
    //}


    // This code is for if 'select' can *not* contain paths (hopefully)
    private boolean isSelected(Data candidate) {
        if (selectFilter == null) return true;
        String name = candidate.getName();
        try {
            for (String selector : selectFilter) {
                if (selector.equals(name)) return true;
                if (selector.equals(".optional") && candidate.isOptional()) return true;
                if (selector.equals(".required") && !candidate.isOptional()) return true;
            }
        } catch (XDException e) { return false; }
        return false;
    }

    private boolean isFiltered(Data candidate)  {
        if (cur_depth > 1 || filterFilter == null) return true;
        filterIndex = 0;
        filterCandidate = candidate; // this is global for a variety of yucky reasons
        // we should have checked the filter syntax earlier with checkFilterExpression(),
        // so this really shouldn't throw an exception at this point
        try { return doFilterExpression(); }
        catch (Exception e) { throw new XDError(candidate, "Problem with filter expression snuck through initial check", e); }
    }

    private static final int OP_EQ  = 1;
    private static final int OP_NE  = 2;
    private static final int OP_GT  = 3;
    private static final int OP_GE  = 4;
    private static final int OP_LT  = 5;
    private static final int OP_LE  = 6;

    public void checkFilterExpression() throws XDException { // used to pre-check expression syntax
        filterCandidate = new CollectionData("..checkExpression");
        doFilterExpression();
    }

    public Boolean doFilterExpression() throws XDException {
        Boolean result = doBoolExp();
        String nextToken = peekToken();
        while (nextToken != null) {
            if      (nextToken.equals("and")){ { getToken(); result = doAnd(result); } }
            else if (nextToken.equals("or")) { { getToken(); result = doOr(result);  } }
            else if (nextToken.equals(")"))  break;
            else throw new XDException(Errors.PARAM_SYNTAX, "Unexpected operation in filter expression:"+nextToken);
            nextToken = peekToken();
        }
        return result == null? false : result;
    }

    private Boolean doBoolExp() throws XDException {
        String leftToken = getToken();
        String nextToken = peekToken();
        Boolean result;
        if (leftToken.equals("(")) {
            result = doParen();
        }
        else if (leftToken.equals("not") &&
                !nextToken.equals("and") && // all of this is to enable the evil case of where "not" is a legitimate child name
                !nextToken.equals("or")  && !nextToken.equals("eq") && !nextToken.equals("ne") && !nextToken.equals("gt") &&
                !nextToken.equals("ge")  && !nextToken.equals("lt") && !nextToken.equals("le") && !nextToken.equals(")")) {
            result = doBoolExp();
            if (result != null) result = !result;
        }
        else {
            Data leftData;
            try { leftData = Eval.eval(filterCandidate, leftToken); }
            catch (XDException e) { leftData = null; } // errors in filter expressions are OK
            nextToken = peekToken();
            if (nextToken != null) {
                if      (nextToken.equals("eq"))  { getToken(); result = doCompOp(leftData,OP_EQ); }
                else if (nextToken.equals("ne"))  { getToken(); result = doCompOp(leftData,OP_NE); }
                else if (nextToken.equals("gt"))  { getToken(); result = doCompOp(leftData,OP_GT); }
                else if (nextToken.equals("ge"))  { getToken(); result = doCompOp(leftData,OP_GE); }
                else if (nextToken.equals("lt"))  { getToken(); result = doCompOp(leftData,OP_LT); }
                else if (nextToken.equals("le"))  { getToken(); result = doCompOp(leftData,OP_LE); }
                else if (nextToken.equals("and") || nextToken.equals("or") || nextToken.equals(")")) result = (leftData==null)?null:leftData.booleanValue();
                else throw new XDException(Errors.PARAM_SYNTAX, "Unexpected operation in filter expression:"+nextToken);
            }
            else result = (leftData==null)?null:leftData.booleanValue();
        }
        return result;
    }

    private Boolean doParen() throws XDException {
        Boolean result =  doFilterExpression();
        if (!getToken().equals(")")) throw new XDException(Errors.PARAM_SYNTAX, "Filter expression missing right parenthesis");
        return result;
    }

    // this uses a Boolean object (as opposed to a boolean) so it can return null, true, or false
    private Boolean doCompOp(Data leftData, int op) throws XDException {
        String rightToken =  getToken();
        if (leftData == null) return null;
        switch (op) {
            case OP_EQ: return  leftData.stringValue().equals(rightToken);
            case OP_NE: return !leftData.stringValue().equals(rightToken);
            case OP_GT:
            case OP_GE:
            case OP_LT:
            case OP_LE:
                Double leftDouble,rightDouble;
                try { leftDouble = Double.parseDouble(leftData.stringValue()); }
                catch (NumberFormatException e) { return null; }  // return null if left side doesn't evaluate to a number
                try { rightDouble = Double.parseDouble(rightToken); }
                catch (NumberFormatException e) { throw new XDException(Errors.PARAM_VALUE_FORMAT, "Filter expression: comparison to non-number '"+rightToken+"'"); }
                switch (op) {
                    case OP_GT: return leftDouble >  rightDouble;
                    case OP_GE: return leftDouble >= rightDouble;
                    case OP_LT: return leftDouble <  rightDouble;
                    case OP_LE: return leftDouble <= rightDouble;
                }
            default: throw new XDException(Errors.INTERNAL_ERROR, "Internal Error: unexpected case in doCompOp");
        }
    }

    // AND logic:
    // if both sides are null, the result is null
    // if one side is null, the results is false
    private Boolean doAnd(Boolean left) throws XDException {
        Boolean right = doBoolExp();
        if (left==null && right==null) return null;
        if (left==null || right==null) return false;
        return left && right;

    }

    // OR logic:
    // if both sides are null, the result is null
    // if one side is null, the result is the other side
    private Boolean doOr(Boolean left) throws XDException {
        Boolean right = doFilterExpression();
        if (left==null && right==null) return null;
        if (left==null)  return right;
        if (right==null) return left;
        return left || right;
    }

    private String getToken() throws XDException {
        if (filterIndex < filterFilter.length) return filterFilter[filterIndex++];
        else throw new XDException(Errors.PARAM_SYNTAX, "Expected more in filter expression");
    }

    private String peekToken() throws XDException {
        if (filterIndex < filterFilter.length) return filterFilter[filterIndex];
        else return null;
    }

    public String toString() {
        StringBuilder results = new StringBuilder("Context{");
        if (description != null)     results.append(" desc='").append(description).append("'");
        if (published_ge != null)    results.append(" pub-ge=").append(published_ge);
        if (published_gt != null)    results.append(" pub-gt=").append(published_gt);
        if (published_le != null)    results.append(" pub-le=").append(published_le);
        if (published_lt != null)    results.append(" pub-lt=").append(published_lt);
        if (sequence_ge != null)     results.append(" seq-ge=").append(sequence_ge);
        if (sequence_gt != null)     results.append(" seq-gt=").append(sequence_gt);
        if (sequence_le != null)     results.append(" seq-le=").append(sequence_le);
        if (sequence_lt != null)     results.append(" seq-lt=").append(sequence_lt);
        if (skip != null)            results.append(" skip=").append(skip);
        if (max_results != null)     results.append(" max=").append(max_results);
        if (depth != null)           results.append(" depth=").append(depth);
        if (descendantDepth != null) results.append(" ddepth=").append(descendantDepth);
        if (priority != null)        results.append(" priority=").append(priority);
        if (external != null)        results.append(" ext=").append(external.toString());
        if (locale != null && !locale.isEmpty() && !locale.equals(DataStore.getDatabaseLocaleString())) results.append(" locale=").append(locale);
        if (filterFilter != null && filterFilter.length!=0) {
            results.append(" filterFilter=");
            for (String filter: filterFilter) results.append("'").append(filter).append("';");
        }
        // This code is for if paths are still allowed in select parameter (hopefully not)
        // if (selectFilter != null && selectFilter.size()!=0) {
        //    results.append(" selectFilter=");
        //    for (String[] selectors: selectFilter){
        //        results.append("[");
        //        for (String select: selectors)
        //            results.append("'").append(select).append("';");
        //        results.append("]");
        //    }
        //}
        // This code is for if paths are *not* allowed in select parameter
         if (selectFilter != null && selectFilter.length!=0) {
            results.append(" selectFilter=");
            for (String selector: selectFilter) results.append("'").append(selector).append(";");
        }
        results.append(" }");
        return results.toString();
    }

    ///////////////////////////////////

    private void evaluateParameters(Map<String,String> parameters) throws XDException {
        metadataFilter = new StringSet(Rules.valueMetadata); // default to "value-related" metadata
        for (String name : parameters.keySet()) {
            String value = parameters.get(name);
            try {
                if      (name.equals("published-ge"))     published_ge    = DatatypeConverter.parseDateTime(value);
                else if (name.equals("published-gt"))     published_gt    = DatatypeConverter.parseDateTime(value);
                else if (name.equals("published-le"))     published_le    = DatatypeConverter.parseDateTime(value);
                else if (name.equals("published-lt"))     published_lt    = DatatypeConverter.parseDateTime(value);
                else if (name.equals("sequence-ge"))      sequence_ge     = Integer.valueOf(value);
                else if (name.equals("sequence-gt"))      sequence_gt     = Integer.valueOf(value);
                else if (name.equals("sequence-le"))      sequence_le     = Integer.valueOf(value);
                else if (name.equals("sequence-lt"))      sequence_lt     = Integer.valueOf(value);
                else if (name.equals("skip"))             skip            = Integer.valueOf(value);
                else if (name.equals("max-results"))      max_results     = Integer.valueOf(value);
                else if (name.equals("select"))           selectFilter    = parseSelectString(value);
                else if (name.equals("filter"))           { filterFilter  = parseFilterString(value); checkFilterExpression(); }
                else if (name.equals("metadata"))         parseMetadataString(value);
                else if (name.equals("depth"))            depth           = Integer.valueOf(value);
                else if (name.equals("descendant-depth")) descendantDepth = Integer.valueOf(value);
                else if (name.equals("priority"))         priority        = Integer.valueOf(value);
                else if (name.equals("reverse"))          reverse         = Boolean.valueOf(value);
                else if (name.equals("locale"))           locale          = value;
                else if (name.equals("alt"))              alt             = value;
            }
            catch (NumberFormatException e)    {
                throw new XDException(Errors.PARAM_VALUE_FORMAT,"query parameter " + name + " is not a number");
            }
            catch (IllegalArgumentException e) {
                throw new XDException(Errors.PARAM_VALUE_FORMAT,"query parameter " + name + " is malformed");
            }
            if (depth != null           && depth < 0 )                      throw new XDException(Errors.PARAM_OUT_OF_RANGE,"Invalid 'depth' parameter value");
            if (descendantDepth != null && descendantDepth < 0)             throw new XDException(Errors.PARAM_OUT_OF_RANGE,"Invalid 'descendantDepth' parameter value");
            if (skip != null            && skip < -1 )                      throw new XDException(Errors.PARAM_OUT_OF_RANGE,"Invalid 'skip' parameter value");
            if (max_results != null     && max_results < 0 )
                throw new XDException(Errors.PARAM_OUT_OF_RANGE,"Invalid 'max-result' parameter value");
            if (priority != null        && (priority < 1 || priority > 16)) throw new XDException(Errors.PARAM_OUT_OF_RANGE,"Invalid 'priority' parameter value");
            if (!(alt.equals("json")||alt.equals("xml")||alt.equals("plain")||alt.equals("media"))) throw new XDException(Errors.PARAM_OUT_OF_RANGE,"Invalid 'alt' value");
            if (!alt.equals("plain")) locale = null; // locale is only for 'alt=plain', the spec says: "If provided for other representations or other base types, it shall be ignored."
        }
    }

    private void parseMetadataString(String metadata) throws XDException {
        // the default is the "value related" metadata,
        // but if ?metadata is specified, we start with nothing but the required stuff like $self, $next, etc.
        metadataFilter = new StringSet(Rules.alwaysMetadata);
        metadataFilterIncludesExtensions = false;
        metadataFilterIncludesDefinitions = false;
        StringTokenizer tokens = new StringTokenizer(metadata,",");
        while (tokens.hasMoreTokens()) {
            String name = tokens.nextToken();
            if      (name.equals("")) continue;
            if      (name.equals("all")||name.equals("cat-all"))  {
                metadataFilter.add(Rules.allMetadata);
                metadataFilter.remove(Meta.HISTORY); // "all" still doesn't include history!
                metadataFilterIncludesExtensions = true;
            }
            else if (name.equals("defs"))            metadataFilterIncludesDefinitions = true;
            else if (name.equals("cat-types"))       metadataFilter.add(Rules.typeMetadata);
            else if (name.equals("cat-tags"))        metadataFilter.add(Rules.tagMetadata);
            else if (name.equals("cat-links"))       metadataFilter.add(Rules.linkMetadata);
            else if (name.equals("cat-ui"))          metadataFilter.add(Rules.uiMetadata);
            else if (name.equals("cat-doc"))         metadataFilter.add(Rules.docMetadata);
            else if (name.equals("cat-data"))        metadataFilter.add(Rules.dataMetadata);
            else if (name.equals("cat-auth"))        metadataFilter.add(Rules.authMetadata);
            else if (name.equals("cat-change"))      metadataFilter.add(Rules.changeMetadata);
            else if (name.equals("cat-extensions"))  metadataFilterIncludesExtensions = true;
            else if (name.equals("cat-value"))       metadataFilter.add(Rules.valueMetadata);
            else if (Rules.allMetadata.containsComponent("$"+name)) metadataFilter.add("$"+name);
            else if (name.startsWith("-") && Rules.allMetadata.containsComponent("$"+name.substring(1))) metadataFilter.remove("$"+name.substring(1)); // Nonstandard, but handy!!
            else throw new XDException(Errors.PARAM_OUT_OF_RANGE,"The 'metadata' query parameter does not support \""+name+"\"");
        }
    }

    // This code is for IF PATHS ARE ALLOWED in select query parameter
    // private List<String[]> parseSelectString(String select) throws XDException {
    //    List<String[]> list = new ArrayList<String[]>();
    //    String[] paths = select.split(";");
    //    for (String path : paths) list.add(path.split("/"));
    //    return list;
    //}

    // This code is for IF PATHS ARE *NOT* ALLOWED in select query parameter
    private String[] parseSelectString(String select) throws XDException {
        return select.split(";");
    }


    private String getToken(Parser parser) throws XDException {  // will get "xxx" from "xxx("  or "(" from "(xxx"
        parser.skipWhitespace();
        String token = parser.consumeUntil(" \t()");
        parser.skipWhitespace();
        if (token.length()==0 && (parser.peekNextNonwhite()=='(' || parser.peekNextNonwhite()==')')) token = String.valueOf(parser.consume());
        return token;
    }

    private String[] parseFilterString(String filter) throws XDException {
        // evil: ((b or(not( f(a)) and c)))
        // must become tokens: '(', '(', 'b', 'or', '(', 'not', '(', 'f(a)', ')', ')', 'and', 'c', ')', ')', ')',
        List<String> tokens;
        Parser parser = new Parser();
        tokens = new ArrayList<String>();
        parser.begin(new StringReader(filter),null,"filter string",0);
        String previousToken = "";
        while (parser.peekNextNonwhite() != 0) {
            String token;
            token = getToken(parser);
            // if you see a word or a right paren, throw it on the stack.
            // if you see a left paren, look at the last word on the stack.
            //     if it's a left paren or and/or/not, then just throw the new left paren on the stack
            //     else, it's a function name, so grab it, add the new left paren, parse up to the the right paren
            //           ...concatenate that all together and replace the last item on the stack
            if (token.equals("(")) {
                if (previousToken.length()==0 || previousToken.equals("(") || previousToken.equals("and") || previousToken.equals("or") || previousToken.equals("not")) {
                    tokens.add("(");  // it's not a function call, just a paren
                    previousToken = "(";
                }
                else {  // it is a function call...
                    // we've already seen the function name, now reassemble it with its args and trailing param
                    String arguments = getToken(parser);
                    String endParen  = getToken(parser);
                    if (endParen == null || !endParen.equals(")")) throw new XDException(Errors.PARAM_SYNTAX,"Missing right paren for function '"+previousToken+"' in filter expression");
                    // do not URL.unencode the arguments. the function processor will do that.
                    tokens.set(tokens.size()-1,previousToken+"("+arguments+")"); // replace function name with entire function invocation
                    previousToken = ")";
                }
            }
            else {
                try { tokens.add(URLDecoder.decode(token, "UTF-8")); // everything that is not a function argument gets unencoded
                } catch (Exception e) { throw new XDException(Errors.PARAM_SYNTAX,"bad URL encoding for '"+token+"' in filter expression"); }
                previousToken = token;
            }
        }
        return tokens.toArray(new String[tokens.size()]);
    }



}
