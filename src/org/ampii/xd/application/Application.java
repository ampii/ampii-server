// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application;

import org.ampii.xd.client.Client;
import org.ampii.xd.common.Log;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.database.DataStore;
import org.ampii.xd.bacnet.BACnetManager;
import org.ampii.xd.managers.ClientManager;
import org.ampii.xd.managers.InfoManager;
import org.ampii.xd.managers.MultiManager;
import org.ampii.xd.managers.SubsManager;
import org.ampii.xd.security.AuthManager;
import org.ampii.xd.server.Server;
import org.ampii.xd.test.Tester;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/**
 * This is where it all starts.
 * <p>
 * The Application class if full of static stuff; there is no attempt here to allow multiple instances in one jvm.
 * But there is still a modicum of self control in this code: only {@link Application} and {@link Tester} classes are allowed to
 * call {@link Log#logConsole}, i.e. System.out
 * <p>
 * The configuration settings are all public so if this main() is not used, they can be manipulated by some
 * other main() before this startup() is called.  If this main() is used, then most of these settings can be controlled
 * with command line arguments.
 * <p>
 *
 * @author drobin
 */
public class Application {

    public static String  version = "0.42"; // an automatic build number gets appended to this at runtime
    public static String  hostName = "localhost"; // change for real deployments so the server knows how to refer to itself
    public static String  baseDir = ".";         // defaults to current working directory
    public static String  logDir = "./logs";
    public static String  logFileGeneral = "ampii-log.txt";
    public static String  logFileHttp = "ampii-log-http.txt";
    public static boolean useDatedLogFiles = false;        // true will append date to log file name
    public static String  configFile = "resources/config/config-with-examples.xml";
    public static String  authFile = "resources/config/auth-with-tls.xml";
    public static String  deviceFile = "resources/config/bacnet-device-object.xml"; // if this contains the default values for the system BACnet Device object
    public static String  webroot = "resources/webroot";
    public static String  dataPrefix = "/bws";       // data prefix, i.e. "/bws/.info"; /.well-known/ashrae is fabricated to match
    public static String  filePrefix = "";           // file prefix (only used if dataPrefix is empty)
    public static String  wordsFile = "resources/webroot/ui/words.txt";  // for generating random strings!  see UI.populateWithRandomValues()
    public static int     tcpPort = 8080;
    public static int     tlsPort = 4443;
    public static Locale  locale = Locale.US;
    public static Level   logLevelGeneral = Level.FINE;
    public static Level   logLevelHttp = Level.FINE;
    public static String  whitelist = "";   // no limiting if empty
    public static int     maxDefinitionDepth = 50;   // stack-protecting max depth to prevent run away circular definitions
    public static int     maxPopulateDepth = 51;   // resource-protecting max depth for populate() to create
    public static int     maxPopulateCount = 1001; // resource-protecting max count of data items for populate() to create
    public static int     multiWatchInterval = 1000; // millis
    public static int     clientWatchInterval = 5000; // millis
    public static int     subsWatchInterval = 1000; // millis
    public static int     sessionTimeout = 3600;  // seconds
    public static int     acquireDatabaseTimeout = 10000; // millies  // no operation should take more than 10 seconds, or a "severe error" will be logged!
    public static int     thisDeviceInstance = 657780; // the instance of ".this" BACnet device (65 77 80 in ASCII is "AMP" :-)
    public static UUID    deviceUUID = null;   // will be based off hostname if not set from command line
    public static boolean doTests = false;
    public static String  testDefinitionFile = "resources/tests/tests.xml"; // test parameters and list of tests to run
    public static String  testDataPath = "test-data";                 // location of Collection to hold data created for/by tests
    public static String  testFilePath = "test-files";                // location of directory to hold files created for/by tests
    public static String  testFormat = "json";                      // "json" or "xml"; sets the default format for test
    public static boolean testStopOnFailure = true;                        // aborts test list on first failure
    public static boolean useEllipticTokenKey = true;
    public static boolean quitOnKeypress = true;
    public static boolean trustAllCertificates = true;

    public static final String rootName = "..root"; // there is no standard name for the root node, so we use this internally. externally, it's known as "/{prefix}"

    // below are behavior modifications for special cases (may alter strict spec compliance)
    public static boolean useLaxDefaultUserPass = true;  // true will allow user/pass of "." to be granted a token (SPEC_PROBLEM)
    public static boolean allowUnsecuredAuth = false; // true will allow authorization requests without TLS (testing only!)


    public static void main(String[] args) {
        version = version + "." + generateBuildNumber();
        Log.logConsole("AMPII Simulator/Tester - version " + version);

        for (int i = 0; i < args.length; i++) {
            try {
                switch (args[i]) {
                    case "--hostName":
                        hostName = args[++i];
                        break;
                    case "--baseDir":
                        baseDir = args[++i];
                        break;
                    case "--configFile":
                        configFile = args[++i];
                        break;
                    case "--authFile":
                        authFile = args[++i];
                        break;
                    case "--deviceFile":
                        deviceFile = args[++i];
                        break;
                    case "--logDir":
                        logDir = args[++i];
                        break;
                    case "--logFileGeneral":
                        logFileGeneral = args[++i];
                        break;
                    case "--logFileHttp":
                        logFileHttp = args[++i];
                        break;
                    case "--tcpPort":
                        tcpPort = Integer.parseInt(args[++i]);
                        break;
                    case "--tlsPort":
                        tlsPort = Integer.parseInt(args[++i]);
                        break;
                    case "--logLevelHttp":
                        logLevelHttp = Level.parse(args[++i]);
                        break;
                    case "--locale":
                        locale = Locale.forLanguageTag(args[++i]);
                        break;
                    case "--generalLogLevel":
                        logLevelGeneral = Level.parse(args[++i]);
                        break;
                    case "--datedLogs":
                        useDatedLogFiles = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--whitelist":
                        whitelist = args[++i];
                        break;
                    case "--maxDefinitionDepth":
                        maxDefinitionDepth = Integer.parseInt(args[++i]);
                        break;
                    case "--useLaxDefaultUserPass":
                        useLaxDefaultUserPass = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--allowUnsecuredAuth":
                        allowUnsecuredAuth = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--thisDeviceInstance":
                        thisDeviceInstance = Integer.parseInt(args[++i]);
                        break;
                    case "--doTests":
                        doTests = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--wordsFile":
                        wordsFile = args[++i];
                        break;
                    case "--testDefinitionFile":
                        testDefinitionFile = args[++i];
                        break;
                    case "--testFormat":
                        testFormat = args[++i];
                        break;
                    case "--testDataPath":
                        testDataPath = args[++i];
                        break;
                    case "--testStopOnFailure":
                        testStopOnFailure = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--acquireDatabaseTimeout":
                        acquireDatabaseTimeout = Integer.parseInt(args[++i]);
                        break;
                    case "--deviceUUID":
                        deviceUUID = UUID.fromString(args[++i]);
                        break;
                    case "--useEllipticTokenKey":
                        useEllipticTokenKey = Boolean.parseBoolean(args[++i]);
                        break;
                    default:
                        fatalExit("Unrecognized command line argument: " + args[i], null);
                }
            } catch (Throwable e) {
                fatalExit("Command line argument problem with '" + args[i] + "'", e);
            }
        }

        try {
            startup();
        } catch (Throwable e) {
            fatalExit("Server failed to start", e);
        }

        if (doTests) {
            Tester.doTests(testFormat);
        }
        else if (quitOnKeypress) {
            Log.logConsole("--->Press Enter to quit<---");  // give a simple interactive way to quit
            try {System.in.read();} catch (Exception e) {} // hit enter key to terminate this blocking call
        }
        else {
            Log.logConsole("Server is running.");
            try { Thread.sleep(Long.MAX_VALUE); } catch (Exception e) {} // this thread sleeps forever
        }
        shutdown();
    }

    public static void startup() throws Exception {

        Log.logConsole("Server is starting on host '" + hostName+"'");
        if (hostName.equals("localhost")) Log.logConsole("WARNING: server does not have a valid host name (using \"localhost\" is FOR TESTING ONLY). For real deployments, set host name with the --hostName command line argument.");
        Log.logConsole("Working Direcory is '" + new File("").getAbsolutePath()+"', log files are in '"+logDir+"'");
        Log.initialize(useDatedLogFiles, logDir, logFileGeneral, logLevelGeneral, logFileHttp, logLevelHttp);
        long start = new GregorianCalendar().getTimeInMillis();
        //
        DataStore.initialize(locale, configFile);
        //
        Server.startTcp();  // starts http on TCP.  http on TLS is started by TLS.activate()
        //
        // optionally populate /.auth with initial data from config file
        if (!authFile.isEmpty()) {
            AuthManager.initializeFromFile(new File(authFile)); // populate the /.auth structure from given file
            // the above might activate TLS if "/.auth/tls-activate" is set to true
        }
        //
        Thread.sleep(200); // delay hack... wait for sockets to bind
        if (Server.getTcpServerFailure() != null) throw new Exception("Http failed to start:"+Server.getTcpServerFailure().getLocalizedMessage());
        if (Server.getTlsServerFailure() != null) throw new Exception("Https failed to start:"+Server.getTlsServerFailure().getLocalizedMessage());
        //
        SubsManager.start();   // start the background task to watch subscriptions
        MultiManager.start();  // start the background task to watch multi records
        ClientManager.start(); // start the background task to watch client records
        BACnetManager.initializeSystemDeviceFromFile(new File(deviceFile));   // set up the ".this" BACnet Device object
        InfoManager.init();    // set "/.info" info
        if (trustAllCertificates) Client.enableTrustAll();
        //
        Log.logConsole("Startup took " + (new GregorianCalendar().getTimeInMillis() - start) + "ms and created " + AbstractData.getTotalCreatedItems() + " initial data items.");
        Log.logConsole("Server is running.");
        AbstractData.resetTotalCreatedItems();
    }

    public static void shutdown() {
        Log.logConsole("Server is shutting down, after creating " + AbstractData.getTotalCreatedItems() + " new data items.");
        // THIS "SHUTDOWN/STOP" STUFF REALLY DOESN'T WORK; NEVER FINISHED, NEVER TESTED.
        // YOU CAN'T REALLY CALL startup() AGAIN SO JUST EXIT AFTER CALLING shutdown() to see the little goodbye message above
        //Server.stopTcp();  // starts http on TCP.  http on TLS is started by TLS.activate()
        //Server.stopTls();  // starts http on TCP.  http on TLS is started by TLS.activate()
        //SubsManager.stop();
        //MultiManager.stop();
    }

    ///////////////////////////////////////////////////////////////////////


    private static void fatalExit(String reason, Throwable t) {
        Log.logConsole(reason);
        if (t!=null) Log.logConsole("Cause: " + t.getLocalizedMessage());
        try {Thread.sleep(200);} catch (InterruptedException ee) {} // system printouts tend to collide so we'll separate them a bit
        if (t != null && !(t instanceof XDException)) t.printStackTrace(); // XDExceptions have nice messages, anything else gets stack trace
        System.exit(0);
    }

    private static long generateBuildNumber() {
        // the "build number" is just the number of minutes since this code was added :-)
        return findNewestFile(new File(baseDir + File.separatorChar + "out").toPath(), 0) / 60000 - 24386442;
    }

    private static long findNewestFile(Path dir, long newest) { // this is used to find the newest class file so we can autogenerate the build date
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            for (Path path : stream) {
                if(path.toFile().isDirectory()) {
                    long modified = findNewestFile(path, newest);
                    if (modified > newest) newest = modified;
                }
                else {
                    long modified = path.toFile().lastModified();
                    if (modified > newest) newest = modified;
                }
            }
        } catch(IOException e) { }
        return newest;
    }

}
