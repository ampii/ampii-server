// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application.hooks;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.XDException;
import org.ampii.xd.security.AuthorizationServer;
import org.ampii.xd.server.Request;
import org.ampii.xd.server.Response;
import org.ampii.xd.server.Server;
import org.ampii.xd.ui.Playground;

/**
 * This is a way to add special URI patterns that are not "data" or "files" that are handled elsewhere.
 * <p>
 * If you want to add a custom URI pattern handler without changing the core AMPII files, create a {@link HTTPHooks.External}
 * interface and inject it with {@link HTTPHooks#registerExternal}.
 *
 * @author daverobin
 */
public class HTTPHooks {

    public interface External {
        Response hook(Request request) throws XDException; //return non-null if handled
    }

    private static External external;

    public static void registerExternal(External hooks) { external = hooks; }

    public static Response hook(Request request) throws XDException {
        if (external != null) {
            Response response = external.hook(request);
            if (request != null) return response;
        }
        switch (request.method) {
            case "GET":
                if (request.path.equals("/.well-known/ashrae"))                      return Server.generateWellknownAshraeResponse();
                if (request.path.equals(Application.dataPrefix +"/.auth/int/token")) return AuthorizationServer.get(request);
            case "POST":
                if (request.path.equals("/ui/rpc"))                                  return Playground.rpc(request);
                if (request.path.equals(Application.dataPrefix +"/.auth/int/token")) return AuthorizationServer.post(request);
        }
        return null;
    }

}
