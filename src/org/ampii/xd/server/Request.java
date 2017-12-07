// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.basetypes.ChoiceData;
import org.ampii.xd.data.basetypes.OctetStringData;
import org.ampii.xd.data.basetypes.StringData;
import org.ampii.xd.marshallers.DataParser;

import java.io.*;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * An object containing the information from an HTTP request.
 *
 * @author daverobin
 */
public class Request {
    public String                 method;
    public String                 path;
    public Map<String,String>     parameters;
    public Map<String,String>     header; // this is a conscious choice to not support multiple header values with the same name; we don't have a use for it and it complicates the code a lot
    public byte[]                 body;
    public String                 firstLine; // only needed for Log.logRequest()
    public Object                 external; // see ExternalHTTPHooks
    public InetAddress            peerAddress;
    public boolean                isTLS;

    public Request() {
        parameters = new HashMap<String,String>();
        header     = new HashMap<String,String>();
    }

    public boolean hasParameter(String name)  {
        return parameters.get(name) != null;
    }

    public String getParameter(String name)  {
        return parameters.get(name);
    }

    public String getParameter(String name, String defaultValue)  {
        String value = parameters.get(name);
        return (value != null) ? value : defaultValue;
    }

    public String getRequiredParameter(String name) throws XDException {
        String value = parameters.get(name);
        if (value == null) throw new XDException(Errors.MISSING_PARAMETER, "'"+name+"' parameter not provided");
        return value;
    }

    public String getHeader(String name)  {
        return header.get(name.toLowerCase());  // they are put in the hashmap in lowercase
    }

    public String getHeader(String name, String defaultValue)  {
        String value = header.get(name.toLowerCase()); // they are put in the hashmap in lowercase
        return (value != null) ? value : defaultValue;
    }

    public String getRequiredHeader(String name)  throws XDException {
        String value = header.get(name.toLowerCase()); // they are put in the hashmap in lowercase
        if (value == null) throw new XDException(Errors.MISSING_PARAMETER, "'"+name+"' header not provided");
        return value;
    }

    public void parseBodyParameters() throws XDException {
        parameters.putAll(HTTP.parseParameters(getBodyAsString()));
    }

    public String getBodyAsString() throws XDException {
        try { return new String(body, "UTF-8"); }
        catch (UnsupportedEncodingException e) { throw new XDError("UTF-8 unsupported?");}// won't happen, UTF-8 always supported
        catch (Error e) { throw new XDException(Errors.VALUE_FORMAT,"HTTP body is not a valid character string"); }
    }

    public Data parseBody() throws XDException {
        String alt = getParameter("alt", "json");
        if (!(alt.equals("xml")||alt.equals("json")||alt.equals("plain")||alt.equals("media"))) throw new XDException(Errors.NOT_REPRESENTABLE, "The specified 'alt' format is not recognized");
        if (alt.equals("media")) {
            String mediaType = getHeader("Content-Type");
            if (mediaType == null || mediaType.isEmpty()) throw new XDException(Errors.MISSING_PARAMETER, "The 'Content-Type' header is missing or blank for alt=media operation");
            return new OctetStringData(".anonymous",body,new StringData(Meta.MEDIATYPE,mediaType)); // we return octetSTRing for now because we don't know if it's
        }
        else {
            return DataParser.parse(getBodyAsString(),alt);
        }
    }


}
