// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Log;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.*;

/**
 * Static methods to manage the HTTP server and its listening ports.
 *
 * @author drobin
 */
public class Server
{
    private static ServerSocket tcpSocket;
    private static Throwable    tcpSocketFailure;
    private static boolean      tcpShuttingDown;

    private static ServerSocket tlsSocket;
    private static Throwable    tlsSocketFailure;
    private static boolean      tlsShuttingDown;

    private static String       httpBaseServerURI  = "http://"  + Application.hostName + ":" + Application.tcpPort;
    private static String       httpsBaseServerURI = "https://" + Application.hostName + ":" + Application.tlsPort;
    private static String       httpBaseDataURI    = "http://"  + Application.hostName + ":" + Application.tcpPort + Application.dataPrefix;
    private static String       httpsBaseDataURI   = "https://" + Application.hostName + ":" + Application.tlsPort + Application.dataPrefix;

    public static String getHttpBaseServerURI()  { return httpBaseServerURI; }

    public static String getHttpsBaseServerURI() { return httpsBaseServerURI; }

    public static String getHttpBaseDataURI()    { return httpBaseDataURI;  } // no trailing slash e.g., http://hostname:8080, http://hostname:8080/bws

    public static String getHttpsBaseDataURI()   { return httpsBaseDataURI; } // no trailing slash e.g., http://hostname:4443, http://hostname:4443/bws

    public static boolean isOurData(String uri) {
        return  uri.equals(Application.dataPrefix) ||          // "/prefix"
                uri.startsWith(Application.dataPrefix+"/") ||  // "/prefix/...", or just "/..." if no prefix
                uri.equals(httpBaseDataURI) ||                 // "http://host:port/prefix",  or "http://host:port" if no prefix
                uri.startsWith(httpsBaseDataURI+"/") ||        // "http://host:port/prefix/...",  or "http://host:port/..." if no prefix
                uri.equals(httpsBaseDataURI) ||                // "https://host:port/prefix",  or "https://host:port" if no prefix
                uri.startsWith(httpsBaseDataURI+"/");          // "https://host:port/prefix/...",  or "https://host:port/..." if no prefix
    }

    public static String getDataPath(String uri) {  // blank means none
        if (uri.isEmpty()) return "";
        if (uri.equals(Application.dataPrefix))         return "/";                                            // "/prefix" -> "/"
        if (uri.startsWith(Application.dataPrefix + "/")) return uri.substring(Application.dataPrefix.length()); // "/prefix/...", or just "/..." if no prefix -> "/..."
        if (uri.equals(httpBaseDataURI))                return "/";                                            // "http://host:port/prefix",  or "http://host:port" if no prefix
        if (uri.startsWith(httpBaseDataURI + "/"))        return uri.substring(httpBaseDataURI.length());        // "http://host:port/prefix/...",  or "http://host:port/..." if no prefix
        if (uri.equals(httpsBaseDataURI))               return "/";                                            // "https://host:port/prefix",  or "https://host:port" if no prefix
        if (uri.startsWith(httpsBaseDataURI + "/"))       return uri.substring(httpsBaseDataURI.length());       // "https://host:port/prefix/...",  or "https://host:port/..." if no prefix
        if (hasScheme(uri)) return "";  // if it has a scheme other than http: or https:, then it's bogus
        else return uri;  // else it's a relative path
    }

    private static boolean  hasScheme(String s) {  // does it start with ascii alpha followed by colon?
        int colonPosition = s.indexOf(':');
        if (colonPosition == -1 || colonPosition == 0) return false;  // must have a colon and at least one character before it
        for (int i = 0; i < colonPosition; i++) {
            char c = s.charAt(i);
            if (!(c > 'a' && c < 'z' || c > 'A' && c < 'Z')) return false; // if we hit a non-ascii-alpha before the first colon, we don't have a scheme
        }
        return true; // yes, we found "{some-alpha}:"
    }

    public static Response generateWellknownAshraeResponse() {
        // .well-known/ashrae content is dynamically created with our data prefix
        Response response = new Response();
        response.contentType = "text/plain";
        response.status = HTTP.HTTP_200_OK;
        response.body = "Link: <"+Application.dataPrefix +">; rel=\"http://bacnet.org/csml/rel#server-root\"";
        return response;
    }

    public static Throwable getTcpServerFailure()  { return tcpSocketFailure; }
    public static Throwable getTlsServerFailure()  { return tlsSocketFailure; }

    public static void startTcp() {
        tcpShuttingDown = false; // duh
        if (tcpSocket != null) { // is the server already running?
            try { tcpSocket.close(); } catch (IOException e) {}  // this will cause thread to restart with new socket
        }
        else {
            Thread thread = new Thread(()->{
                    int respawnDelay = 1;
                    for (;;) {
                        try {
                            Log.logFine("http server getting socket ");
                            if (tcpSocket != null) tcpSocket.close();
                            tcpSocket = new ServerSocket(Application.tcpPort);
                            Log.logInfo("http server worker listening on port "+ Application.tcpPort);
                            for (;;) {
                                Socket accepted = tcpSocket.accept();
                                Worker worker = new Worker(accepted);
                                Thread workerThread = new Thread(worker);
                                workerThread.setDaemon(true);
                                workerThread.start();
                            }
                        }
                        catch (Throwable e) {
                            if (tcpShuttingDown) {
                                Log.logInfo("http server worker thread shutting down" );
                                break;
                            }
                            else {
                                tcpSocketFailure = e;
                                if (respawnDelay<600) respawnDelay = respawnDelay*2;  // double delay every time, up to ten minutes
                                Log.logWarning("http server worker thread died (respawn in "+respawnDelay+"s) " + e.getMessage());
                                try {
                                    Thread.sleep(respawnDelay*1000);
                                }
                                catch(InterruptedException ie) {
                                    Log.logSevere("http server respawn broken - exiting thread! " + ie.getMessage());
                                    break;
                                }
                            }
                        }
                    }
                }
            );
            thread.setDaemon(true);
            thread.start();
        }
    }

    public static void stopTcp() {
        tcpShuttingDown = true;
        if (tcpSocket != null) {
            try { tcpSocket.close(); } catch (IOException e) {} // this will cause thread to notice the shutdown
        }
    }

    public static void startTls()  {
        tlsShuttingDown = false;
        if (tlsSocket != null) {   // is the TLS server already running?
            try { tlsSocket.close(); } catch (IOException e) {}  // this will cause thread to restart with new socket
        }
        else {
            Thread tlsthread = new Thread( new Runnable() {
                public void run()  {
                    int respawnDelay = 1;
                    for (;;) {
                        try {
                            Log.logFine("https server getting socket for port "+ Application.tlsPort);
                            SSLServerSocketFactory sssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                            if (tlsSocket != null) tlsSocket.close();
                            tlsSocket = sssf.createServerSocket(Application.tlsPort);
                            Log.logInfo("https server worker listening on port "+ Application.tlsPort);
                            for (;;) {
                                SSLSocket accepted = (SSLSocket) tlsSocket.accept();
                                Worker worker = new Worker(accepted);
                                Thread thread = new Thread(worker);
                                thread.setDaemon(true);
                                thread.start();
                            }
                        }
                        catch (Throwable e) {
                            if (tlsShuttingDown) {
                                Log.logInfo("https server worker thread shutting down" );
                                break;
                            }
                            else {
                                tlsSocketFailure = e;
                                if (respawnDelay<600) respawnDelay = respawnDelay*2;  // double delay every time, up to 10 minutes
                                Log.logWarning("https server worker thread died (respawn in "+respawnDelay+"s) " + e.getMessage());
                                try {
                                    Thread.sleep(respawnDelay*1000);
                                }
                                catch(InterruptedException ie) {
                                    Log.logSevere("https server respawn broken - exiting thread! " + ie.getMessage());
                                    break;
                                }
                            }
                        }
                    }
                }
            });
            tlsthread.setDaemon(true);
            tlsthread.start();
        }
    }

    public static void stopTls()  {
        tlsShuttingDown = true;
        if (tlsSocket != null) {
            try { tlsSocket.close(); } catch (Exception e) { }// this will cause thread to notice the shutdown
        }
    }

}

