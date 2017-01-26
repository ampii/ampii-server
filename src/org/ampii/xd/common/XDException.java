// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.common;

import org.ampii.xd.data.Data;
import org.ampii.xd.data.basetypes.ParsedData;
import org.ampii.xd.data.Context;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * An Exception that provides an BACnet/WS error number and human-readable reason.
 * <p>
 * Almost everything that can go wrong is the result of external requests, so an XDException is used just about everywhere,
 * and provides the information needed to make a correct HTTP error response to the client along with some hopefully helpful
 * human readable text.
 *
 * @author drobin
 */
public class XDException extends Exception {

    private int          error;
    private Data         target;
    private Data         given;
    private Context      context;
    private String       message;
    private List<Object> extras;

    public XDException(int error, String message, Object... extras)                                { this(error,null,  null,  null,    message, extras); }
    public XDException(int error, Data target, String message, Object... extras)                   { this(error,target,null,  null,    message, extras); }
    public XDException(int error, Data target, Data given, String message, Object... extras)       { this(error,target,given, null,    message, extras); }
    public XDException(int error, Data target, Context context, String message, Object... extras)  { this(error,target,null,  context, message, extras); }

    public XDException(int error, Data target, Data given, Context context, String message, Object... extras) {
        this.error   = error;
        this.message = message;
        this.target  = target;
        this.given   = given;
        this.context = context;
        this.extras  = new ArrayList<Object>();
        for (Object extra: extras) this.extras.add(extra);
    }

    public XDException add(Object... extras) {
        for (Object extra: extras) this.extras.add(extra);
        return this;
    }

    public int    getErrorNumber()  { return error; }

    public String getErrorText()    { return message; }

    public String toString()   { return "XDException:" + error + ";" + Errors.textForErrorNumber(error)+ ";" + message; }

    public String getMessage() { return getLocalizedMessage(); }

    public String getLocalizedMessage() {
        StringBuilder body = new StringBuilder();
        body.append("? ").append(error).append(" ").append(Errors.textForErrorNumber(error)).append("\n");
        if (message != null) body.append("Message:\n   ").append(message).append("\n");
        if (target  != null) body.append("Target:\n   ").append(target.toString(false)).append("\n");
        if (target instanceof ParsedData) body.append("Parsed From: ").append(((ParsedData) target).parsedFrom()).append("\n");
        if (given   != null) body.append("Given:\n   ").append(given.toString(false)).append("\n");
        if (given instanceof ParsedData) body.append("Parsed From: ").append(((ParsedData)given).parsedFrom()).append("\n");
        if (context != null) body.append("Context:\n   ").append(context.toString()).append("\n");
        if (extras.isEmpty())
            body.append("   (no additional info available)\n");
        else
            for (Object item : extras) body.append("More Info:\n"+indent(extraItemToStringLines(item),1)+"\n");
        return body.toString();
    }

    public String extraItemToStringLines(Object item) {
        if      (item instanceof ParsedData) return "Data: " + ((Data)item).toString(false) + "\nParsed From: "+((ParsedData)item).parsedFrom()+"\n";
        else if (item instanceof Data)       return "Data: " + ((Data)item).toString(false) + "\n";
        else if (item instanceof String)     return "Message: " + (String)item + "\n";
        else if (item instanceof Throwable)  return throwableToStringLines((Throwable)item)+ "\n";
        else if (item instanceof Context)    return "Context: "+item.toString() + "\n";
        else                                 return "Other: "+ (item!=null?item.toString():"<null>") + "\n";
    }

    public static String stackToStringLines(Throwable t)  {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
    public static String indent(String s, int level )  {
        if (s == null) s = "<null>";
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<level; i++) sb.append("   ");
        String indention = sb.toString();
        s =  indention + s.replace("\n", "\n"+indention);
        if (s.endsWith("\n"+indention)) s = s.substring(0,s.length()-indention.length()-1);
        return s;
    }

    public static String throwableToStringLines(Throwable t)  {
        return "Thrown: " + t.getClass().getSimpleName() + "\n" +
               "   Cause: \n" + indent(t.getLocalizedMessage(),1) + "\n" +
               "   " +
                "Stack: \n" + indent(stackToStringLines(t),1);
    }


}
