// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.security;

import org.ampii.xd.common.Errors;
import org.ampii.xd.data.Data;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.Rules;
import org.ampii.xd.server.Request;
import java.util.*;

/**
 * Used to decide if data operations can be performed with the given authorization.
 * <p>
 * Usually found in a Context in a session. e.g., someData.getContext().getAuthorizer().
 * <p>
 * An empty Authorizer (one not based on an external HTTP request) will allow all operations by default (i.e., "god mode")
 * <p>
 * Code that knows what it is doing can temporarily enter god mode, with enterGodMode(), for certain operations to
 * override the normal permission checks without needing to save/restore the real user's context.
 *
 * @author drobin
 */
public class Authorizer {

    private String[] authorizedScopes = null;
    private boolean  authorizationProvided = false;
    public int       inGodMode;   // counting semaphore used by inGodMode(), enterGodMode(), etc.

    /**
     *  Default constructor is for "god mode" - for internal use, not web requests.
     *  This will authorize all requests, even reading the unreadable and writing the unwritable.
     */
    public Authorizer() {
        enterGodMode();
    }

    public void    enterGodMode() { inGodMode++; }
    public void    exitGodMode()  { inGodMode--; }
    public boolean inGodMode()    { return inGodMode != 0; }


    public Authorizer(Request request) throws XDException {
        // makes a new authorizer from HTTP request
        authorizedScopes = new String[0];
        // the auth token can either be in the Authorization header (preferred) or the "access_token" URI query parameter (frowned upon)
        String authorization = request.getHeader("Authorization");
        if (authorization != null)  {
            // header looks like this: Authorization: Bearer {token}
            StringTokenizer parser = new StringTokenizer(authorization);
            if (!parser.hasMoreElements()) throw new XDException(Errors.PARAM_SYNTAX,"Authorization header is blank");
            if (!parser.nextToken().equals("Bearer")) throw new XDException(Errors.PARAM_OUT_OF_RANGE,"Authorization header is not of type \"Bearer\"");
            if (!parser.hasMoreTokens()) throw new XDException(Errors.PARAM_SYNTAX,"Authorization OAuth access token is blank");
            decodeToken(parser.nextToken());  // throws if bad token: not for us, expired, etc
            authorizationProvided = true;
        }
        else {
            authorization = request.getParameter("access_token");
            if (authorization != null)  {
                if (request.getHeader("Authentication")!= null) throw new XDException(Errors.PARAM_NOT_SUPPORTED,"Can't specify both Authorization header and oauth_token query parameter");
                decodeToken(authorization);    // throws if bad token: not for us, expired, etc
                authorizationProvided = true;
            }
        }
        // else it just creates an authorizer with an empty authorizedScopes list with authorizationProvided false;
    }

    public void    requireWrite(Data data) throws XDException { checkWrite(data,true); }

    public boolean checkWrite(Data data) throws XDException { return checkWrite(data,false); }

    public boolean checkWrite(Data data, boolean required) throws XDException {
        if (authorizedScopes == null || inGodMode()) return true; // null is for internal use only
        if (!data.isWritable()) {
            if (required) throw new XDException(Errors.NOT_WRITABLE,data,"The data is not writable");
            else return false;
        }
        if (!scopesMatch(getWriteScopesFor(data))) {
            if (required) {
                if (authorizationProvided) throw new XDException(Errors.OAUTH_INSUFFICIENT_SCOPE, data, "Insufficient scope provided");
                else throw new XDException(Errors.NOT_AUTHORIZED, data, "Authorization required");
            }
            else return false;
        }
        return true;
    }


    public void    requireRead(Data data) throws XDException { checkRead(data, true); }

    public boolean checkRead(Data data)  { try { return checkRead(data,false);} catch (XDException e) {return false;} } // exception will not happen

    public boolean checkRead(Data data, boolean required) throws XDException {
        if (authorizedScopes == null) return true; // null is for internal use only
        if (!data.isReadable()) {
            if (required) throw new XDException(Errors.NOT_READABLE,data,"The data is not readable");
            else return false;
        }
        if (!scopesMatch(getReadScopesFor(data))) {
            if (required) {
                if (authorizationProvided) throw new XDException(Errors.OAUTH_INSUFFICIENT_SCOPE, data, "Insufficient scope provided");
                else throw new XDException(Errors.NOT_AUTHORIZED, data, "Authorization required");
            }
            else return false;
        }
        return true;
    }

    public void    requireVisible(Data data) throws XDException { checkVisible(data, true); }

    public boolean checkVisible(Data data)  { try { return checkVisible(data, false);} catch (XDException e) {return false;} } // exception will not happen

    public boolean checkVisible(Data data, boolean required) throws XDException {
        if (data.isVisible()) return true;
        if (authorizedScopes == null) return true; // null is for internal use only
        if (!data.effectiveStringValueOf(Meta.AUTHREAD, "").isEmpty() && scopesMatch(getReadScopesFor(data))) return true;
        if (required) throw new XDException((Rules.isMetadata(data)?Errors.METADATA_NOT_FOUND:Errors.DATA_NOT_FOUND),data,"Item is not visible");
        else return false;
    }


    ///////////////////////////////////////////

    private boolean scopesMatch(String dataScopes) {
        if (dataScopes == null) return true;            // OK if no required scope.
        if (dataScopes.length() == 0) return true;      // OK if required scope is empty (makes no requirements).
        if (authorizedScopes.length == 0) return false; // if we *do* have requirements but we *don't* have any authorization, then it's a no-go
        // we have some requirements AND we have some authorization...
        boolean allFound = true; // let's be optimistic!   but a single not-found will kill it
        for (String dataScope : dataScopes.split(" ")) {
            boolean oneFound = false;
            for (String authorizedScope : authorizedScopes) if (dataScope.equals(authorizedScope))  { oneFound = true; break; }
            if (!oneFound) { allFound = false; break; }
        }
        return allFound; // all requirements must be present
    }

    private String getWriteScopesFor(Data data) {
        String authWrite = data.effectiveStringValueOf(Meta.AUTHWRITE, null);
        if (authWrite != null) return authWrite;
        if (data.getParent() != null) return getWriteScopesFor(data.getParent());
        return null;
    }
    private String getReadScopesFor(Data data) {
        String authRead = data.effectiveStringValueOf(Meta.AUTHREAD, null);
        if (authRead != null) return authRead;
        if (data.getParent() != null) return getReadScopesFor(data.getParent());
        return null;
    }

    private String getVisibleScopesFor(Data data) {
        String authRead = data.effectiveStringValueOf(Meta.AUTHVISIBLE, null);
        if (authRead != null) return authRead;
        if (data.getParent() != null) return getReadScopesFor(data.getParent());
        return null;
    }

    private void decodeToken(String accessToken) throws XDException {
        Token token = new Token();
        token.decode(accessToken);
        authorizedScopes = token.getScopes().split(" ");
    }

    public String toString() {
        StringBuilder results = new StringBuilder("Auth{");
        if (authorizationProvided && authorizedScopes != null) {
            results.append(" scopes=");
            for (String a : authorizedScopes) results.append("'").append(a).append("';");
        }
        results.append("}");
        return results.toString();
    }
}
