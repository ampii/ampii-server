// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.security;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.marshallers.JSONGenerator;
import org.ampii.xd.server.*;

/**
 * This is the "Internal Authorization Server" function.
 * <p>
 * It provides only the required "Resource Owner Password" and "Client Credentials" grant types, listening at /.auth/int/token
 * <p>
 * It supports POST with body parameters (preferred) and GET with URI parameters (NOT recommended because secrets could get logged)
 *
 * @author drobin
 */
public class AuthorizationServer {

    public static Response post(Request request) throws XDException {
        if (!request.getHeader("Content-Type","").startsWith("application/x-www-form-urlencoded")) throw new XDException(Errors.PARAM_SYNTAX, "Content-Type is not 'application/x-www-form-urlencoded'");
        request.parseBodyParameters();
        return get(request);
    }

    public static Response get(Request request) throws XDException {
        // TODO: implement protection against brute force password attack (rate-limiting, etc.)
        if (!request.path.endsWith("/.auth/int/token")) throw new XDError("Authorization server called with bad path"); // this is an internal error, this should have been caught earlier
        if (!request.isTLS && !Application.allowUnsecuredAuth) throw new XDException(Errors.AUTH_INVALID, "OAuth request is not secured with TLS!");
        boolean enabled = AuthManager.getSettings().int__enable;
        if (!enabled) throw new XDException(Errors.NOT_AUTHENTICATED, "Internal Authorization Server is not enabled");
        String scope = request.getParameter("scope", "");
        String grantType = request.getRequiredParameter("grant_type");
        String authorization = request.getHeader("authorization", null);
        if (grantType.equals("password")) {
            // client credentials are OPTIONALLY given through HTTP "Basic" authentication in the header
            if (authorization != null) authenticateClient(authorization);
            // username and password are REQUIRED
            String user = request.getRequiredParameter("username");
            String pass = request.getRequiredParameter("password");
            String authuser = AuthManager.getSettings().int__user;
            String authpass = AuthManager.getSettings().int__pass;
            if (!Application.useLaxDefaultUserPass) if(authpass.equals(".") || authuser.equals(".")) throw new XDException(Errors.NOT_AUTHENTICATED, "Internal username and password are not configured yet");
            if (!user.equals(authuser) || !pass.equals(authpass)) throw new XDException(Errors.NOT_AUTHENTICATED, "Incorrect username and/or password");
        }
        else if (grantType.equals("client_credentials")) {
            // The client credentials are given through HTTP "Basic" authentication in the header
            if (authorization == null) throw new XDException(Errors.MISSING_PARAMETER, "'Authorization' header not provided");
            authenticateClient(authorization);
        }
        else throw new XDException(Errors.PARAM_OUT_OF_RANGE, "Specified 'grant_type' of '"+grantType+"' not supported");
        String body = JSONGenerator.generatePairs("access_token",makeAccessToken(scope),"token_type","Bearer","expires_in",AuthManager.getSettings().int__config__token_dur);
        Response response = new JSONResponse(HTTP.HTTP_200_OK, body.toString());
        response.addHeader("Cache-Control","no-store");
        response.addHeader("Pragma","no-cache");
        return response;
    }

    public static void authenticateClient(String authorization) throws XDException {
        String[] userpass = HTTP.parseBasicAuthorization(authorization);
        String authid     = AuthManager.getSettings().int__id;
        String authsecret = AuthManager.getSettings().int__secret;
        if (!Application.useLaxDefaultUserPass) if(authid.equals(".") || authsecret.equals(".")) throw new XDException(Errors.NOT_AUTHENTICATED, "Internal client id and secret are not configured yet");
        if (!userpass[0].equals(authid) || !userpass[1].equals(authsecret)) throw new XDException(Errors.NOT_AUTHENTICATED, "Incorrect client id and/or secret");
    }

    public static String makeAccessToken(String scopes) throws XDException {
        Token token = new Token();
        token.setScopes(scopes);
        return token.encode();
    }

}
