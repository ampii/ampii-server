// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDException;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Static methods and constants related to the HTTP protocol
 */
public class HTTP {

    public static final String HTTP_200_OK           = "200 OK";
    public static final String HTTP_201_CREATED      = "201 Created";
    public static final String HTTP_204_NO_CONTENT   = "204 No Content";
    public static final String HTTP_301_REDIRECT     = "301 Moved Permanently";
    public static final String HTTP_302_FOUND        = "302 Found";
    public static final String HTTP_400_BADREQUEST   = "400 Bad Request";
    public static final String HTTP_401_UNAUTHORIZED = "401 Unauthorized";
    public static final String HTTP_403_FORBIDDEN    = "403 Forbidden";
    public static final String HTTP_404_NOTFOUND     = "404 Not Found";
    public static final String HTTP_405_BADMETHOD    = "405 Method Not Allowed";
    public static final String HTTP_405_TIMEOUT      = "408 Request Timeout";
    public static final String HTTP_412_PRECONDITION = "412 Precondition Failed";
    public static final String HTTP_415_MEDIATYPE    = "415 Unsupported Media Type";
    public static final String HTTP_500_INTERNALERROR= "500 Internal Server Error";

    public static Map<String,String> parseParameters(String www_form_urlencoded) throws XDException {
        Map<String,String> results = new HashMap<>();
        String[] tokens = www_form_urlencoded.split("&");
        for (String parameter : tokens) {
            try {
                int equalsIndex = parameter.indexOf('=');
                if ( equalsIndex != -1 ) {
                    String name  = parameter.substring(0, equalsIndex);
                    String value = parameter.substring(equalsIndex + 1);
                    name  = URLDecoder.decode(name,"UTF-8").trim();
                    value = URLDecoder.decode(value,"UTF-8");
                    results.put(name,value);
                }
                else results.put(parameter.trim(),"");
            }
            catch (Exception e) {
                throw new XDException(Errors.PARAM_SYNTAX, "Bad percent encoding in \""+parameter+"\"");
            }
        }
        return results;
    }

    public static String encodeParameters(Map<String,String> parameters) throws XDException {
        StringBuilder results = new StringBuilder();
        boolean first = true;
        for (Map.Entry parameter : parameters.entrySet()) {
            if (!first) results.append("&");
            //try {
                //String name = URLEncoder.encode((String) parameter.getKey(), "UTF-8");
                //String value = URLEncoder.encode((String) parameter.getValue(), "UTF-8");
                String name = (String)parameter.getKey();
                String value = (String)parameter.getValue();
                results.append(name).append("=").append(value);
            //} catch(UnsupportedEncodingException e){} // stupid API, this never happens for UTF-8
            first = false;
        }
        return results.toString();
    }


    public static String[] parseBasicAuthorization(String authorization) throws XDException {
        if (!authorization.startsWith("Basic ")) throw new XDException(Errors.PARAM_VALUE_FORMAT, "'Authorization' header is not 'Basic'");
        byte bytes[] = DatatypeConverter.parseBase64Binary(authorization.substring(6));
        authorization = new String(bytes, StandardCharsets.UTF_8);
        int i = authorization.indexOf(":");
        if (i == -1) throw new XDException(Errors.PARAM_VALUE_FORMAT, "'Authorization' header value '"+authorization+"' is not in the format 'user:pass'");
        return new String[]{ authorization.substring(0,i),authorization.substring(i+1)};
    }

    public static String generateBasicAuthorization(String user, String pass) {
        return "Basic "+DatatypeConverter.printBase64Binary((user+":"+pass).getBytes(StandardCharsets.UTF_8));
    }

    public static String x_www_url_form_encoded(String... namedValues) {  // for making Content-Type x-www-url-form-encoded
        // namedValues is an even-number-sized array of alternating name and value entries
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < namedValues.length; i += 2) {
            try { result.append(namedValues[i]).append("=").append(URLEncoder.encode(namedValues[i + 1],"UTF-8")); } catch (UnsupportedEncodingException e){}
            if (i + 2 < namedValues.length) result.append("&");
        }
        return result.toString();
    }


}
