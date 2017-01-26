// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Log;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Rules;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * A worker thread that listens for incoming HTTP connections and dispatches to GetHandler, PutHandler, etc.
 *
 * @author drobin
 */
public class Worker implements Runnable
{
    private Socket  socket;

    public Worker(Socket s) {
        socket = s;
    }

    public void run() {

        try  {
            InputStream is = socket.getInputStream();
            // Even though we want to read *chars* to process the header, we don't turn the InputStream into a Reader
            // because we need it later as a raw byte stream to get the body using content-length (which is in bytes).
            // So we first have to read the bytes up to the \r\n\r\n, then wrap that with a Reader to read the header as
            // lines of text, then switch back to reading raw bytes to get the body, then wrap that in a Reader to get
            // the body in characters.  Not fun, but it works.
            ByteArrayOutputStream rawHeader = new ByteArrayOutputStream();
            int state= 0;  // the state machine for looking for the "\r\n\r\n" that terminates the header
            int aByte = is.read();
            while (aByte != -1) {
                rawHeader.write(aByte);
                if (state == 0 && aByte == '\r' )      state = 1;
                else if (state == 1 && aByte == '\n' ) state = 2;
                else if (state == 2 && aByte == '\r' ) state = 3;
                else if (state == 3 && aByte == '\n' ) break;
                else state = 0;
                aByte = is.read();
            }
            if (state != 3) return; // connection terminated before reaching end of header (actually happens in the wild)
            // now convert the raw bytes into characters so we can read the header
            ByteArrayInputStream bis = new ByteArrayInputStream(rawHeader.toByteArray());
            BufferedReader reader = new BufferedReader(new InputStreamReader(bis, "UTF-8"));
            // create a Request to hold everything and dive in
            Request request = new Request();
            consumeHeader(reader, request);
            // that was easy;  now check for a body
            String contentLength = request.header.get("content-length");
            if (contentLength != null) {
                try {
                    int size = Integer.parseInt(contentLength);
                    // we need to use raw bytes to get the content-length correct
                    request.body = new byte[size];
                    int receivedBytes = 0;
                    while (receivedBytes < size) {  // read it as efficiently as possible (might be all at once, but not always)
                        int read = is.read(request.body, receivedBytes, size-receivedBytes);
                        //int read = is.read(buffer, receivedBytes, 1);
                        if (read == -1) break;
                        receivedBytes += read;
                    }
                }
                catch (NumberFormatException ex) { errorAbort(HTTP.HTTP_400_BADREQUEST, "Bad Request: content-length is not a number!"); }
            }
            request.isTLS = socket instanceof SSLSocket;
            request.peerAddress = socket.getInetAddress();
            // check white list, if provided
            String peerAddressAsString = request.peerAddress.getHostAddress();
            if (Application.whitelist != null && !Application.whitelist.isEmpty() && !Application.whitelist.contains(peerAddressAsString)) {
                Log.logDeniedRequest(request);
            }
            else {
                Log.logRequest(request);
                try {
                    // before dispatch, do a few global things...
                    Rules.validateGlobalRequestOptions(request);
                    // now, the all-important dispatch based on HTTP method:
                    switch (request.method) {
                        case "GET":
                            sendResponse(GetHandler.handle(request));
                            break;
                        case "PUT":
                            sendResponse(PutHandler.handle(request));
                            break;
                        case "POST":
                            sendResponse(PostHandler.handle(request));
                            break;
                        case "DELETE":
                            sendResponse(DeleteHandler.handle(request));
                            break;
                        default:
                            sendError(HTTP.HTTP_500_INTERNALERROR, "Unsupported HTTP method: " + request.method);
                            break;
                    }
                }
                catch (XDException e)  { sendResponse(new ErrorResponse(e,request)); }
                catch (XDError e)      { sendResponse(new ErrorResponse(e,request)); }
            }
            is.close();
        }
        catch (IOException e)  { sendError(HTTP.HTTP_500_INTERNALERROR, "Internal Error, Worker.run: IOException: "+e.getLocalizedMessage());  }
        catch (InterruptedException e) { } // errorAbort will throw an InterruptedException and we'll just exit the thread
    }

    private  void consumeHeader(BufferedReader reader, Request request) throws InterruptedException {
        try {
            String line = reader.readLine();
            if (line == null) { Log.logWarning("what the? blank request?"); throw new InterruptedException(); } // this happens sometimes!
            request.firstLine = line; // remember this for Log.logRequest() and other reasons nosey reasons
            StringTokenizer tokens = new StringTokenizer(line); // split first line into: VERB pathAndQuery version
            request.method = tokens.nextToken();                // the first token is the verb
            parsePathAndQuery(tokens.nextToken(),request);      // further split second token into path and query parameters
            line = reader.readLine();                           // now crank through the remaining header lines
            while (line != null && line.trim().length()>0) {    // ...till blank line found
                int colonIndex = line.indexOf( ':' );           // we just ignore malformed headers with no ':'
                if ( colonIndex != -1 ) request.header.put( line.substring(0,colonIndex).trim().toLowerCase(), line.substring(colonIndex+1).trim());
                line = reader.readLine();                       // now on the the next...
            }
        }
        catch (IOException e) { errorAbort(HTTP.HTTP_500_INTERNALERROR, "Internal Error, Worker.consumeHeader: IOException: " + e.getMessage()); }
        catch (NoSuchElementException e) { errorAbort(HTTP.HTTP_400_BADREQUEST, "Bad HTTP Request Format"); } // if nextToken() fails, first line is hosed
    }

    private void parsePathAndQuery(String pathAndQuery, Request request) throws InterruptedException {
        int queryStart = pathAndQuery.indexOf('?');
        if ( queryStart != -1 ) {
            // first split into to path and query components
            String queryString = pathAndQuery.substring(queryStart+1);
            request.path = unencode(pathAndQuery.substring(0,queryStart));
            try { request.parameters = HTTP.parseParameters(queryString); } catch (XDException e) { errorAbort(HTTP.HTTP_400_BADREQUEST, e.getMessage());  }
        }
        else request.path = unencode(pathAndQuery); // no query part, so path is whole thing
    }

    private String unencode(String string) throws InterruptedException {
        try { return URLDecoder.decode(string,"UTF-8"); }
        catch( Exception e )  { errorAbort(HTTP.HTTP_400_BADREQUEST, "Bad Request: Bad percent encoding"); }
        return ""; //this will never happen, but compiler doesn't know errorAbort always throws an exception
    }

    // sends an error response and then throws exception
    private void errorAbort(String status, String msg) throws InterruptedException  {
        sendResponse(new Response(status, "text/plain", msg));
        throw new InterruptedException();
    }

    // just send an error response but does not throw exception.  use as alternative to sendResponse
    private void sendError(String status, String msg)  {
        sendResponse(new Response(status, "text/plain", msg));
    }

    private void sendResponse(Response response)  {
        try {
            // first let the response do any prep work it wants.
            response.prepareToSend();
            OutputStream os = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(os);
            // first build header
            StringBuilder builder = new StringBuilder();
            builder.append("HTTP/1.1 " + response.status + " \r\n");
            if (response.contentType != null) builder.append("Content-Type: " + response.contentType + "\r\n");
            // crank through all the headers...
            for (String name : response.header.keySet()) builder.append(name + ": " + response.header.get(name) + "\r\n");
            // convert string data to raw octets to get proper Content-Length!
            byte[] bodyBytes = null;
            if (response.body instanceof String) bodyBytes = ((String)response.body).getBytes("UTF-8");
            if (response.body instanceof byte[]) bodyBytes = (byte[])response.body;
            if (bodyBytes != null) builder.append("Content-Length: " + bodyBytes.length + "\r\n");
            // end header with blank line and append the body
            builder.append("\r\n");
            String header = builder.toString();
            Log.logResponse(response, header);
            os.write(header.getBytes("UTF-8"));
            if (bodyBytes != null) os.write(bodyBytes);
            os.flush();
            os.close();
        }
        catch ( UnsupportedEncodingException e ) { } // not gonna happen
        catch( IOException ioe ) { try { socket.close(); } catch( Throwable t ) {} }
    }
}