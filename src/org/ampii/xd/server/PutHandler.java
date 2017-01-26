// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

import org.ampii.xd.application.Application;
import org.ampii.xd.application.hooks.HTTPHooks;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Rules;
import org.ampii.xd.database.Session;
import org.ampii.xd.data.Context;
import org.ampii.xd.resolver.Eval;
import org.ampii.xd.resolver.Path;
import org.ampii.xd.security.Authorizer;

/**
 * Handles the HTTP PUT method - called from {@link Worker}.
 *
 * @author drobin
 */
public class PutHandler {

    public static Response handle(Request request) throws XDException {
        Response response = HTTPHooks.hook(request);
        if (response != null) return response;
        if (Path.isDataPath(request.path)) return putData(request);
        else throw new XDException(Errors.NOT_WRITABLE, "Can't PUT files at '" + request.path + "'. Were you looking for BACnet/WS data? The prefix for data on this server is '" + Application.dataPrefix + "'.");
    }

    private static Response putData(Request request) throws XDException {
        Rules.validatePutDataRequestOptions(request);
        try {
            Data data = request.parseBody();
            Context context = new Context(request);
            context.setAuthorizer(new Authorizer(request));
            Session session = Session.makeWriteSession("PutHandler",context);
            try {
                Data target = Eval.eval(session.getRoot(), request.path, Eval.FOR_PUT);
                context.setTarget(target);
                context.getAuthorizer().requireWrite(target);  // we found or created our target for the put(), but is it writable?
                target.put(data);                         // finally...the actual put()! (well, the *attempted* put(), anyway - it could still throw)
                session.commit();                     // let any application-specific logic know we just wrote the data (hook to back-ends)
            }
            finally { session.discard(); }
            return new TextResponse(HTTP.HTTP_204_NO_CONTENT, "");
        }
        catch (XDException e) { throw e; }
        catch (Throwable t)   { throw new XDError("Unhandled exception or error",t); }
    }


}
