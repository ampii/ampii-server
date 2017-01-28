// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.common;

import org.ampii.xd.server.Request;
import org.ampii.xd.server.Response;
import org.ampii.xd.application.Application;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.*;


/**
 * The all-static Log object provides logging services for all AMPII classes. It is initialized by the {@link Application}
 * class and the log file locations and names can be configured with command line arguments.
 *
 * @author daverobin
 */
public class Log {

    private static Level        logLevelGeneral = Level.FINE;   //or Level.INFO for less;
    private static Level        logLevelHttp    = Level.INFO;
    private static Level        logLevelConsole = Level.INFO;  // console should always be set to "info"

    private static Logger       generalLogger;
    private static Logger       httpLogger;
    private static Logger       consoleLogger;

    private static PrintWriter  logFileGeneralWriter; // console logs go here too
    private static PrintWriter  logFileHttpWriter;

    public static Logger getGeneralLogger()                 { return generalLogger; }
    public static Logger setGeneralLogger(Logger newLogger) { Logger old = generalLogger; generalLogger = newLogger; return old; }

    public static Logger getHTTPLogger()                    { return httpLogger;    }
    public static Logger setHTTPLogger(Logger newLogger)    { Logger old = httpLogger;    httpLogger = newLogger;    return old; }

    public static Logger getConsoleLogger()                 { return consoleLogger; }
    public static Logger setConsoleLogger(Logger newLogger) { Logger old = consoleLogger; consoleLogger = newLogger; return old; }

    public static Level  getGeneralLevel()                  { checkInit(); return generalLogger.getLevel(); }
    public static void   setGeneralLevel(Level level)       { checkInit(); generalLogger.setLevel(level); }

    public static Level  getHTTPLevel()                     { checkInit(); return httpLogger.getLevel(); }
    public static void   setHttpLevel(Level level)          { checkInit(); httpLogger.setLevel(level); }

    // general
    public static void  logSevere(String message)           { checkInit(); generalLogger.severe("SEVERE: " + message); }
    public static void  logWarning(String message)          { checkInit(); generalLogger.warning("WARNING: " + message); }
    public static void  logInfo(String message)             { checkInit(); generalLogger.info(message); }
    public static void  logFine(String message)             { checkInit(); generalLogger.fine(message); }

    // http
    public static void  logHttpSevere(String message)       { checkInit(); httpLogger.severe("SEVERE: " + message); }
    public static void  logHttpWarning(String message)      { checkInit(); httpLogger.warning("WARNING: " + message); }
    public static void  logHttpInfo(String message)         { checkInit(); httpLogger.info(message); }
    public static void  logHttpFine(String message)         { checkInit(); httpLogger.fine(message); }

    // console
    public static void  logConsole(String message)          { checkInit(); consoleLogger.info(message); }


    public static void logDeniedRequest(Request request) {
        httpLogger.info("BLOCKED "+request.peerAddress.getHostAddress()+" "+request.firstLine);
    }

    public static void logRequest(Request request) {
        if (!httpLogger.isLoggable(Level.FINE)) {
            httpLogger.info(request.peerAddress.getHostAddress()+" "+request.firstLine);
        }
        else {
            StringBuilder builder = new StringBuilder();
            builder.append("\nvvvvvvvvvv\n");
            builder.append(request.firstLine);
            builder.append("\n");
            for (Map.Entry entry : request.header.entrySet()) {
                builder.append(entry.getKey() + ": " + entry.getValue() + "\n");
            }
            builder.append("\n");
            if (request.body != null) {
                String alt = request.getParameter("alt", "json");
                if (alt.equals("xml") || alt.equals("json") || alt.equals("plain")) {
                    try { builder.append(request.getBodyAsString()); }
                    catch (XDException e) { builder.append("[" + request.body.length + " bytes of invalid UTF-8]"); }
                }
                else  {
                    builder.append( "["+ request.body.length + " bytes of data]" );
                }
                builder.append("\n");
            }
            builder.append("^^^^^^^^^^\n");
            httpLogger.fine(request.peerAddress.getHostAddress()+" "+request.firstLine+builder.toString());
        }
    }

    public static void logResponse(Response response, String header) {
        String status = String.valueOf(response.status);
        String bodySummary = null;
        if (response.body instanceof String) {
            if (response.fromFile) {
                bodySummary =  "["+ ((String)response.body).length() + " chars of file text]";
            }
            else {
                // get "first line" (or reasonable portion thereof)
                int firstLineEnd = ((String)response.body).indexOf('\n');
                int bodyLength = ((String)response.body).length();
                if (firstLineEnd == -1) firstLineEnd = bodyLength > 40 ? 40 : bodyLength;
                bodySummary = ((String)response.body).substring(0, firstLineEnd);
            }
        }
        else if (response.body instanceof byte[]) {
            bodySummary = "["+ ((byte[])response.body).length + " bytes of binary data]";
        }
        if (!httpLogger.isLoggable(Level.FINE)) {
            httpLogger.info(status+" "+bodySummary);
        }
        else  {
            StringBuilder builder = new StringBuilder();
            builder.append("\nvvvvvvvvvv\n");
            builder.append(header);
            if (response.body instanceof byte[] || response.fromFile) builder.append(bodySummary).append("\n");
            else if (response.body instanceof String ) { builder.append(response.body); if (!((String)response.body).endsWith("\n")) builder.append("\n"); }
            else builder.append("[unknown body type!]\n");
            builder.append("^^^^^^^^^^\n");
            httpLogger.fine(status + " " + bodySummary + builder.toString());
        }

    }

    public static void initialize(boolean useDatedLogFiles, String logDir, String logFileGeneral, Level logLevelGeneral, String logFileHttp, Level logLevelHttp) {
        if (useDatedLogFiles) {
            logFileGeneral = makeDatedFileName(logFileGeneral);
            logFileHttp    = makeDatedFileName(logFileHttp);
        }
        setLogFileGeneral(logDir + File.separatorChar + logFileGeneral);
        setGeneralLevel(logLevelGeneral);
        setLogFileHttp(logDir + File.separatorChar + logFileHttp);
        setHttpLevel(logLevelHttp);
    }

    public static void setLogFileGeneral(String fileName) {
        try { logFileGeneralWriter = new PrintWriter(new FileWriter(new File(fileName)));}
        catch (IOException e) { Log.logSevere("Can't open log file "+fileName);}
    }
    public static void setLogFileHttp(String fileName) {
        try { logFileHttpWriter = new PrintWriter(new FileWriter(new File(fileName))); }
        catch (IOException e) { Log.logSevere("Can't open log file "+fileName);}
    }

    private static void checkInit() {
        if (generalLogger == null) {
            generalLogger = Logger.getLogger("GEN");
            generalLogger.addHandler(new BetterConsoleHandler("GEN"));
            generalLogger.setUseParentHandlers(false);
            generalLogger.setLevel(logLevelGeneral);
        }
        if (consoleLogger == null) {
            consoleLogger = Logger.getLogger("CON");
            consoleLogger.addHandler(new BetterConsoleHandler("CON"));
            consoleLogger.setUseParentHandlers(false);
            consoleLogger.setLevel(logLevelConsole);
        }
        if (httpLogger == null) {
            httpLogger = Logger.getLogger("HTTP");
            httpLogger.addHandler(new BetterConsoleHandler("HTTP"));
            httpLogger.setUseParentHandlers(false);
            httpLogger.setLevel(logLevelHttp);
        }
    }



    //////////////////////////////////

    static private class BetterConsoleHandler extends Handler {
        String name="";
        public BetterConsoleHandler(String name) { this.name = name; }

        public void publish(LogRecord record) {
            if (!isLoggable(record)) return;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (name.equals("HTTP")) {
                if (logFileHttpWriter != null) {
                    logFileHttpWriter.println(name+" "+format.format(new Date())+" " +record.getMessage());
                    logFileHttpWriter.flush();
                }
            }
            else if (name.equals("CON")){
                if (logFileGeneralWriter != null) {
                    logFileGeneralWriter.println(name+"  "+format.format(new Date())+" " +record.getMessage());
                    logFileGeneralWriter.flush();
                }
                System.out.println(record.getMessage()); // also send to System.out with no datestamp
            }
            else if (name.equals("GEN")){
                if (logFileGeneralWriter != null) {
                    logFileGeneralWriter.println(name+"  "+format.format(new Date())+" " +record.getMessage());
                    logFileGeneralWriter.flush();
                }
                else System.out.println("(log file error!): "+name+" "+format.format(new Date())+" " +record.getMessage());
            }
            else {
                System.out.println("(unknown log!): "+name+" "+format.format(new Date())+" " +record.getMessage());
            }
        }
        public void flush() { }
        public void close() throws SecurityException {  }
    }


    private static String makeDatedFileName(String name) {
        int i = name.lastIndexOf('.');
        String extension = (i != -1)? name.substring(i) : "";
        name = name.substring(0, name.length()-extension.length());
        return name +"-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + extension;
    }



}
