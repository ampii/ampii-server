// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

import org.ampii.xd.application.Application;
import org.ampii.xd.application.hooks.HTTPHooks;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.database.Session;
import org.ampii.xd.data.Context;
import org.ampii.xd.resolver.Eval;
import org.ampii.xd.resolver.Path;
import org.ampii.xd.security.Authorizer;

/**
 * Handles the HTTP DELETE method - called from {@link Worker}.
 *
 * @author drobin
 */
public class DeleteHandler {

    public static Response handle(Request request) throws XDException {
        // first check for special resolver
        Response response = HTTPHooks.hook(request);
        if (response != null) return response;
        if (Path.isDataPath(request.path)) return deleteData(request);
        else throw new XDException(Errors.NOT_WRITABLE,"Can't DELETE files at '"+request.path+"'. Were you looking for BACnet/WS data? The prefix for data on this server is '"+Application.dataPrefix+"'.") ;
    }

    private static Response deleteData(Request request) throws XDException {
        Rules.validateDeleteDataRequestOptions(request);
        try {
            Context context = new Context(request);
            context.setAuthorizer(new Authorizer(request));
            Session session = Session.makeWriteSession("DeleteHandler",context);
            try {
                Data target = Eval.eval(session.getRoot(), request.path, Eval.FOR_DELETE);
                if (!target.hasParent()) throw new XDException(Errors.NOT_WRITABLE,"Can't DELETE parentless data") ;
                target.getParent().delete(target.getName());
                session.commit();
            }
            finally { if (session!=null) session.discard(); }
            return new TextResponse(HTTP.HTTP_204_NO_CONTENT, ""); // and send the positive response
        }
        catch (XDException e) { throw e; }
        catch (Throwable t)   { throw new XDError("Unhandled exception or error",t); }
    }

}
