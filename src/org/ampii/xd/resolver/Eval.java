// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.resolver;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Rules;
import org.ampii.xd.data.Context;
import org.ampii.xd.functions.Functions;
import org.ampii.xd.security.Authorizer;

/**
 * Workhorse class for evaluating path expressions, including function calls.
 *
 * @author daverobin
 */
public class Eval {

    public static final int FOR_GET    = 1;
    public static final int FOR_PUT    = 2;
    public static final int FOR_POST   = 3;
    public static final int FOR_DELETE = 4;
    public static final int FOR_EXISTS = 5;

    public static Data   eval(Data base,  String path, Data defaultValue) { try { return eval(base, path); } catch(XDException e) { return defaultValue; } }

    public static String eval(Data base,  String path, String defaultValue) { try { return eval(base, path).stringValue(); } catch(XDException e) { return defaultValue; } }

    public static Data   eval(Data base,  String path) throws XDException { return eval(base, path, FOR_GET); }

    public static Data  eval(Data base, String path, int mode) throws XDException {
        Context    context    = base.getContext();
        Authorizer authorizer = context.getAuthorizer();
        if (path.startsWith("/")) base = getRootFor(base);
        Data target = base;
        String[] segments;
        if (path.equals(Application.dataPrefix) || path.startsWith(Application.dataPrefix+"/")) segments = path.substring(Application.dataPrefix.length()).split("/");
        else segments = path.split("/");
        String segment;
        for (int i = 0; i < segments.length; i++) { // can't use a foreach here because we need to look ahead
            segment = segments[i];

            // allow 'foo//bar',  'foo/./bar', and  'foo/../bar'
            if (segment.length() == 0) continue;
            if (segment.equals("."))   continue;
            if (segment.equals(".."))  { if (target.hasParent()) target = target.getParent(); continue; }
            if (segment.equals("...")) { target = getRootFor(target); continue; }  // "..." is alias for the root data prefix, to allow things like ".../.auth/foo" for internal use

            // first we check for aliases, like $history, '.this', '.blt', etc.
            Data found = Alias.resolve(target, segment);
            if (found != null) { authorizer.requireVisible(found); target = found;  continue; }

            // next we give the fabricator a shot at this for GET because it replaces/hides some things, like $type
            // (but we don't allow fabricated things for PUT and DELETE)
            if (mode == FOR_GET) found = Fabricator.fabricate(target, segment);
            if (found != null) { authorizer.requireVisible(found); target = found;  continue; }

            // now try the database
            found = target.find(segment);
            if (found != null) { authorizer.requireVisible(found); target = found;  continue; }

            // is it a function?
            if (segment.contains("(")) {
                if (!segment.endsWith(")")) throw new XDException(Errors.ARG_SYNTAX,"Function invocation does not end with ')'");
                int leftParen = segment.indexOf("(");
                String function  = segment.substring(0,leftParen);
                String argString = segment.substring(leftParen+1, segment.length() - 1);
                found = Functions.invoke(target, function, argString, context);
                target = found;  continue; // invoke throws if bad, so 'found' is always non-null here
            }

            // is it optional metadata or an optional child? To rationalize this as a legitimate RESTful PUT, we pretend that optional children and
            // all possible metadata are valid resources. This is backed up by the fact that the names are fixed, i.e., we know what we are
            // PUTting to, it's just not there at the moment. This is different from POST where we don't actually know the name of the result.
            if (mode == FOR_PUT || mode == FOR_POST) {
                if (Rules.isMetadata(segment)) {
                    if (Rules.notAllowedForPut(segment)) throw new XDException(Errors.ILLEGAL_METADATA,target,"Can't PUT or POST to computed metadata '"+segment+"'");
                    found = target.createMetadata(segment, null, null);
                }
                else {
                    try { found = target.createChild(segment, null, null); }
                    catch (XDException e) { throw new XDException(Errors.CANNOT_CREATE,target,"Can't create child for PUT or POST '"+segment+"'",e); }
                }
                target = found;
                continue;
            }

            // oops.  if we made it this far then there's nothing else to do
            if (Rules.isMetadata(segment)) throw new XDException(Errors.METADATA_NOT_FOUND,target,"Metadata '"+segment+"' not found in path '"+path+"'");
            else                           throw new XDException(Errors.DATA_NOT_FOUND,    target,"Data '"    +segment+"' not found in path '"+path+"'");
        }
        if (mode == FOR_GET) authorizer.requireRead(target); // finally, check if the target is readable
        return target;
    }

    //////////////////////

    private static Data getRootFor(Data data) throws XDException {
        Data found = data;
        for ( ; found.hasParent() ; found = found.getParent());
        return found;
    }


}
