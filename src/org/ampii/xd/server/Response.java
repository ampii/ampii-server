// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

import org.ampii.xd.database.DataStore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * The base class for an object containing the information that will be an HTTP response - see JSONResponse, ErrorResponse, etc.
 *
 * @author drobin
 */
public class Response
{
    public String                 status;
    public String                 contentType;
    public Object                 body;    // String or byte[]
    public HashMap<String,String> header;
    public boolean                fromFile; // for logging suppression

    public Response() {
        header = new HashMap<String,String>();
    }

    public Response(String status, String mimeType, Object body) {
        this();
        this.status      = status;
        this.contentType = mimeType;
        this.body        = body;
    }

    //  subclasses can uses this to finish creating body, and any other actions, before sending
    public void prepareToSend()
    {
        if (status == null) status = HTTP.HTTP_500_INTERNALERROR;   // oops
        if (header.get("Date") == null) header.put("Date",getCurrentDateString());
    }

    public void addHeader(String name, String value) {
        header.put(name, value);
    }

    private static SimpleDateFormat dateFormatter;
    private String getCurrentDateString() {
        if (dateFormatter == null) {
            dateFormatter = new java.text.SimpleDateFormat( "E, d MMM yyyy HH:mm:ss 'GMT'", DataStore.getDatabaseLocale());
            dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        return dateFormatter.format(new Date());
    }




}