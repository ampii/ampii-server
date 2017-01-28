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
import java.util.UUID;

/**
 * Handles the HTTP POST method - called from {@link Worker}.
 *
 * @author daverobin
 */
public class PostHandler {

    public static Response handle(Request request)throws XDException {
        Response response = HTTPHooks.hook(request);
        if (response != null) return response;
        if (Path.isDataPath(request.path)) return postData(request);
        else throw new XDException(Errors.NOT_WRITABLE,"Can't POST files at '"+request.path+"'. Were you looking for BACnet/WS data? The prefix for data on this server is '"+Application.dataPrefix+"'.") ;
    }

    private static Response postData(Request request) throws XDException {
        Rules.validatePostDataRequestOptions(request);
        try {
            Data data = request.parseBody(); // will throw early exception if there is a problem with the given data
            Context context = new Context(request);
            context.setAuthorizer(new Authorizer(request));
            Session session = Session.makeWriteSession("PostHandler", context);
            try {
                Data target = Eval.eval(session.getRoot(), request.path, Eval.FOR_POST);
                context.setTarget(target);
                String name = data.getName();
                if (name == null || name.isEmpty()) name = UUID.randomUUID().toString();    // if no given name, just make something up
                data.setName(Path.makeLegalPathName(name)); // we don't just accept what the client wanted!
                data = target.post(data);                   // do it! this will possibly call bindings to process RPC-style POSTs
                // if the returned data is marked "ephemeral", then this POST is not actually persisting/creating anything.
                // so we just return the processed data to the client
                if (data.getName().equals("..ephemeral")) {
                    return GetHandler.getDataResponse(data);
                }
                else {   // otherwise data was actually created and needs to be committed
                    session.commit();
                    // and we only return the location of the newly created data
                    Response response = new TextResponse(HTTP.HTTP_201_CREATED,"");
                    response.addHeader("Location", Server.getHttpBaseServerURI() + Path.toPath(data));
                    return response;
                }
            }
            finally  { if (session!=null) session.discard(); }
        }
        catch (XDException e) { throw e; }
        catch (Throwable t)   { throw new XDError("Unhandled exception or error",t); }
    }
}
