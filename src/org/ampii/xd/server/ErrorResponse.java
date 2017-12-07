// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;

/**
 * An HTTP response containing an error code and a possibly helpful explanation.
 *
 * @author daverobin
 */
public class ErrorResponse extends Response {

    public ErrorResponse(XDError e, Request request) {
        this(new XDException(Errors.INTERNAL_ERROR,"Internal Error", e),request);
    }

    public ErrorResponse(XDException e, Request request) {
        status = Errors.statusLineForErrorNumber(e.getErrorNumber());
        String errorText = request != null? request.getParameter("error-string") : null;
        if (errorText != null) {
            body = errorText;
        }
        else {
            StringBuilder builder = new StringBuilder();
            String errorPrefix = request != null? request.getParameter("error-prefix") : null;
            if (errorPrefix != null) builder.append(errorPrefix + " ");
            builder.append(e.getLocalizedMessage());
            body = builder.toString();
        }
        switch (e.getErrorNumber()) {
            case Errors.OAUTH_INVALID_TOKEN:
                header.put("WWW-Authenticate","Bearer realm=\"bacnet\", error=\"invalid_token\", error_description=\""+e.getErrorText()+"\"");
                break;
            case Errors.OAUTH_INVALID_REQUEST:
                header.put("WWW-Authenticate","Bearer realm=\"bacnet\", error=\"invalid_request\", error_description=\""+e.getErrorText()+"\"");
                break;
            case Errors.INSUFFICIENT_SCOPE:
                header.put("WWW-Authenticate","Bearer realm=\"bacnet\", error=\"insufficient_scope\"");
                break;
            case Errors.NOT_AUTHORIZED:
                header.put("WWW-Authenticate","Bearer realm=\"bacnet\""); // RFC 6750 says not to provide scope info unless the client at least tries to authenticate
                break;
        }
    }
}
