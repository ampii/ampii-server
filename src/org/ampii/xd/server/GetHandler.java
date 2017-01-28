// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

import org.ampii.xd.application.Application;
import org.ampii.xd.application.hooks.HTTPHooks;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.Rules;
import org.ampii.xd.database.Session;
import org.ampii.xd.data.Context;
import org.ampii.xd.marshallers.JSONGenerator;
import org.ampii.xd.marshallers.PlainGenerator;
import org.ampii.xd.marshallers.XMLGenerator;
import org.ampii.xd.resolver.Eval;
import org.ampii.xd.resolver.Path;
import org.ampii.xd.security.Authorizer;
import java.io.StringWriter;

/**
 * Handles the HTTP GET method - called from {@link Worker}.
 *
 * @author daverobin
 */
public class GetHandler {

    public static Response handle(Request request) throws XDException {
        // first check for special resolver, like "/.well-known/ashrae" and OAuth token endpoint
        Response response = HTTPHooks.hook(request);
        if (response != null) return response;
        if (Path.isDataPath(request.path)) return getData(request);
        else return new FileResponse(Application.baseDir+"/"+Application.webroot+"/",request.path);
    }

    private static Response getData(Request request) throws XDException {
        Rules.validateGetDataRequestOptions(request);
        try {
            Context context    = new Context(request);
            context.setAuthorizer(new Authorizer(request));
            Session session = Session.makeReadSession("GetHandler",context);
            try {
                Data target = Eval.eval(session.getRoot(), request.path, Eval.FOR_GET);
                if (!target.hasParent()) target.setContext(context); // if this evaluated to parentless fabrication, we need to give it it's context
                context.setTarget(target); // now tell the context that this is the target of the URI - this means that this is the "top" for serialization and filtering
                if (target.getName().equals(Application.rootName)) context.setDepth(1); // it has become clear that actually returning the root without depth limit is a bad idea!
                return getDataResponse(target);
            }
            finally { if (session!=null) session.discard(); }
        }
        catch (XDException e) { throw e; }
        catch (Throwable t)   { throw new XDError("Unhandled exception or error",t); }
    }

    public static Response getDataResponse(Data data) throws XDException {
        StringWriter writer = new StringWriter();
        switch (data.getContext().getAlt()) {
            case "plain":
                if (!data.canHaveValue()) throw new XDException(Errors.NOT_REPRESENTABLE,"The specified value is not available as plaintext");
                PlainGenerator plain = new PlainGenerator();
                plain.generate(writer, data);
                return new TextResponse(HTTP.HTTP_200_OK, writer.toString());
            case "json":
                JSONGenerator json = new JSONGenerator();
                json.generate(writer, data);
                return new JSONResponse(HTTP.HTTP_200_OK, writer.toString());
            case "xml":
                XMLGenerator xml = new XMLGenerator();
                xml.generate(writer, data);
                return new XMLResponse(HTTP.HTTP_200_OK, writer.toString());
            case "media":
                if (data.getBase() != Base.STRING && data.getBase() != Base.OCTETSTRING) throw new XDException(Errors.NOT_REPRESENTABLE,"Invalid base type for alt=media");
                String mediaType = data.stringValueOf(Meta.MEDIATYPE,"");
                if (mediaType.isEmpty()) throw new XDException(Errors.NOT_REPRESENTABLE,"There is no $mediaType for this data item");
                return new MediaResponse(HTTP.HTTP_200_OK, mediaType, data.getValue());
            default:
                throw new XDException(Errors.NOT_REPRESENTABLE,"The specified 'alt' format is not recognized");
        }
    }

}

