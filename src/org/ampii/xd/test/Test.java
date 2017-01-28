// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.test;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.LocalizedStrings;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.AnyData;
import org.ampii.xd.data.basetypes.CollectionData;
import org.ampii.xd.data.basetypes.StringData;
import org.ampii.xd.database.Session;
import org.ampii.xd.definitions.DefinitionCollector;
import org.ampii.xd.definitions.Definitions;
import org.ampii.xd.definitions.Instances;
import org.ampii.xd.data.Context;
import org.ampii.xd.marshallers.*;
import org.ampii.xd.resolver.Eval;
import org.ampii.xd.resolver.Path;
import org.ampii.xd.server.Server;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * The base class for all tests - provides all the storage and methods for the tests to do their jobs.
 * <p>
 * These methods are the basic building blocks of tests.  Almost all of them will throw a TestException to end the test
 * when something goes wrong. Normally, tests will just be a series of these methods.  Hopefully, only rarely should you
 * have to resort to "custom" code interspersed with these. One typical case for custom code would be to remember/recall
 * variables, or to add conditional execution.
 * <p>
 * You can end a test early (e.g. you want to skip all or part) by simply returning from execute().  This will not count
 * as a failure.
 * <p>
 * You can indicate a failure by throwing a TestException or by using the convenience method fail() that will do it for you.
 * <p>
 * You *can* also *allow* an XDException to be thrown by your custom code, but that's not as polite as throwing a TestException.
 * <p><pre>{@code
 *     public void step(String description)    // optionally used to marks Test progress with a "test step" description
 *
 *     public void uri(String uri)             // sets the entire uri: overwrites scheme,host,port,path,query (not including alt)
 *     public void scheme(String scheme)       // sets scheme to "http" (default) or "https" (will also set appropriate port if host is Application.hostName)
 *     public void host(String host)           // defaults to Application.hostName (which defaults to "localhost" for testing)
 *     public void port(int port)              // set automatically for Application.hostName, needs to be explicitly set for external hosts
 *     public void path(String path)           // set by serverData() to point to local data, otherwise needs to be explicitly set
 *     public void pathAdd(String extra)       // adds string to end of path
 *     public void pathRemove(String extra)    // removes string from end of path
 *     public void query(String query)         // query expression without leading "?", e.g. "metadata=all&depth=1", or blank for none
 *     public void alt(String format)          // one of "json","xml","plain","media","none","default" - default is set by Application.testFormat
 *     public void contentType(String text)    // sets or expects ContentType header; will be set by a call to alt(...)
 *     public void requestHeader(String name, String value) // sets a request header; passing a null value will remove a previously set header
 *     public void requestText(String text)    // sets entire request body for PUT or POST - mutually exclusive with clientData()
 *
 *     public void definition(String toParse)  // adds a definition, with no <Definitions> wrapper, for this test; call once for each def to be added
 *
 *     public void serverData(String toParse)  // makes local target data in server and sets host,port,path to match location of data on server
 *     public void serverData(Data target)     // makes local target data in server and sets host,port,path to match location of data on server
 *
 *     public void clientData(String toParse)  // sets data for the client side: a destination for GET or a source for PUT/POST (mutually exclusive with requestText())
 *     public void clientData(Data data)       // sets data for the client side: a destination for GET or a source for PUT/POST (mutually exclusive with requestText())
 *     public void clientData()                // clears the client data (a new UndefinedData will be created if needed)
 *     public void clientDataPars(String pars) // controls the generation of clientData for PUT and POST, e.g., "metadata=cat-ui"
 *
 *     public void get()                       // GETs the data at scheme://host:port/path and sets responseCode and responseText
 *     public void put()                       // PUTs the clientData to scheme://host:port/path and sets responseCode and responseText
 *     public void post()                      // POSTs the clientData to scheme://host:port/path and sets responseCode and responseText
 *     public void delete()                    // DELETEs the resource at scheme://host:port/path and sets responseCode and responseText
 *
 *     public void expectResponseHeaderPresent(String name)             // expects response header to be present
 *     public void expectResponseHeaderAbsent(String name)              // expects response header to be absent
 *     public void expectResponseHeaderValue(String name, String value) // expects response header to be present with given value
 *
 *     public void expectResponseSuccessCode()                          // expects the response status code to be 200-299
 *     public void expectResponseFailureCode()                          // expects an error status code of some kind
 *     public void expectResponseCode(int code)                         // expects a specific response status code
 *     public void expectErrorNumber(int number)                        // expects responseText to start with "? <number>", i.e. Errors.XXXX
 *
 *     public void expectResponseText()                                 // expects a non-zero length responseText (http body)
 *     public void expectResponseText(String body)                      // expects entire responseText to exactly match given string
 *     public void expectResponseTextContains(String fragment)          // expects responseText to contain given string
 *     public void expectResponseTextStartsWith(String fragment)        // expects responseText to start with given string
 *
 *     public void allowResponseDefinitions()                           // allows the response to contain definitions contexts
 *
 *     public void expectResponseData()                                   // expects to be able to parse responseText into responseData, based on 'alt'
 *     public void expectResponseDataItemPresent(String path)             // expects responseData item at path to exist
 *     public void expectResponseDataItemAbsent(String path)              // expects responseData item at path to be absent
 *     public void expectResponseDataItemValue(String path, String value) // expects responseData item at path to be present and have given avlue
 *
 *     public void expectClientData()                                   // expects previously declared clientData to be updatable by responseData
 *     public void expectClientData(String toParse)                     // expects updated clientData to exactly match the data in the given json/xml
 *     public void expectClientData(Data data)                          // expects updated clientData to exactly match the given data
 *     public void expectClientDataItemPresent(String path)             // expects item at path to be present in the updated clientData
 *     public void expectClientDataItemAbsent(String path)              // expects item at path to be absent in the updated clientData
 *     public void expectClientDataItemValue(String path, String value) // expects item at path to be present and have given value in the updated clientData
 *
 *     public void expectServerData(String toParse)                     // expects serverData to be exactly equal to the data in the given json/xml
 *     public void expectServerData(Data expectedData)                  // expects serverData to be exactly equal to given data
 *     public void expectServerDataItemPresent(String path)             // expects serverData item at path to be present
 *     public void expectServerDataItemAbsent(String path)              // expects serverData item at path to be absent
 *     public void expectServerDataItemValue(String path, String value) // expects serverData item at path to be present and have given value
 *
 *     public void delay(int timeout)                                    // just waits 'timeout' ms
 *
 *     public void fail(String reason)                                   // ends the test by throwing TestException()
 *     public void fail(String reason, Exception e)                      // ends the test by throwing TestException()
 * }</pre><p>
 * In addition to the above basic building blocks, custom code can use the following public accessors.
 * Unless they have a declared 'defaultValue', all the getXxxx methods will throw a TestException if the desired thing is not available
 * <p><pre>{@code
 *     public String getResponseHeader(String name, String defaultValue)
 *     public Data   getResponseData()
 *     public Data   getClientData()
 *     public Data   getServerData()
 * }</pre><p>
 *
 * @author daverobin
 */
public class Test {

    protected TestEnvironment env;
    protected int             testNumber;                      // set upon Test creation
    protected int             stepNumber;                      // set upon step() call
    protected String          testDescription;                 // set upon Test creation
    protected String          stepDescription;                 // updated during test execution to annotate progress for error messages
    protected String          serverDataPath;                  // serverData() creates temporary target in Application.hostName and makes host/port/path to point to it
    protected Data            serverData;                      // serverData() creates temporary target in Application.hostName and makes host/port/path to point to it
    protected String          scheme = "http";                 // use scheme() to set "http" or "https"
    protected String          host   = Application.hostName;   // use host() for setting up external access
    protected int             port   = Application.tcpPort;    // setting scheme to "https" with host==Application.hostName will change this to Application.serverTLSPort
    protected String          path   = "/";                    // absolute paths on host==Application.hostName will get Application.dataPrefix prepended
    protected String          alt    = "none";                 // default is set by environment. use alt() to set "xml"/"json"/"plain", (or "default" to reset)
    protected String          contentType = "";                // sets or expects ContentType header; will be set by a call to contentType or alt(...)
    protected String          query  = "";                     // query string without leading '?', default is set by environment during initialize()
    protected String          requestText;                     // entire body of outgoing PUT of POST (mutually exclusive with clientData)
    protected Data            clientData;                      // data that is to be the destination of a GET or the source for a PUT/POST (mutually exclusive with requestText)
    protected String          clientDataPars;                  // formatted as query pars, e.g., "metadata=xxx", this controls the generation of clientData into xml/json for put/post
    protected int             responseCode;
    protected String          responseText;
    protected Data            responseData;
    protected Map<String,List<String>> requestHeaders;
    protected Map<String,List<String>> responseHeaders;
    protected List<String>    testDefinitionNames;             // temporary definitions that were declared in tests (to be removed after test completion)
    protected List<String>    testFileNames;                   // temporary files that were created by tests (to be removed after test completion)
    protected DataList        responseDefinitions;             // definitions collected from response data; allowed (made non-null) by allowResponseDefinitions()
    protected DataList        responseTagDefinitions;          // tag definitions collected from response data; allowed (made non-null) by allowResponseTagDefinitions()

    static int                 testCount;                      // used to dynamically generate the testNumber

    public Test(String description) {
        this.testDescription = description;
    }

    // called behind the scenes by the Tester before it calls execute()
    public void initialize(TestEnvironment env)  {
        this.env = env;
        alt(env.defaultFormat);
        testNumber = ++testCount;
    }

    // called behind the scenes by the Tester after execute() returns or throws exception
    public void close() throws XDException {
        if (testDefinitionNames != null) { // clean up any temporary definitions created by this test
            for (String name : testDefinitionNames) Definitions.removeDefinition(name);
        }
        if (testFileNames != null) { // clean up any temporary files created by this test
            for (String name : testFileNames) new File(Application.webroot+ getServerTestFilePath()+"/"+name).delete();
        }
        Session.atomicPut("Test.close", getServerTestDataPath(), new CollectionData("")); // this will delete all children in server data path
    }

    public void execute() throws TestException,XDException { // this method is overridden by anonymous subclasses (the actual tests)
        fail("can't call execute() directly on Test class");
    }

    public void step(String description) { // optionally marks test progress with a "test step" description
        this.stepNumber++;
        this.stepDescription = description;
        env.stepTest(this);  // callback to environment for each step
    }

    public void uri(String uri) throws TestException {  // sets scheme,host,port,path,query
        try {
            URI u  = new URI(uri);
            String parsedScheme = u.getScheme();
            String parsedHost   = u.getHost();
            int    parsedPort   = u.getPort();
            String parsedPath   = u.getRawPath();
            String parsedQuery  = u.getRawQuery();
            scheme = parsedScheme != null? parsedScheme : "http";
            host   = parsedHost   != null? parsedHost   : "localhost";
            port   = parsedPort   != -1  ? parsedPort   : scheme.equals("https")?443:80;
            path   = parsedPath   != null? parsedPath   : "/";
            query  = parsedQuery  != null? parsedQuery  : "";
        }
        catch (URISyntaxException e) { fail("uri(" + uri + "): URI syntax error: " + e.getLocalizedMessage()); }
    }

    public void scheme(String scheme)  {  // sets scheme to "http" (default) or "https" (also sets port if host is Application.hostName)
        this.scheme = scheme;   // also set the port to match the scheme if the host is still Application.hostName
        if (host.equals("localhost") || host.equals(Application.hostName)) port = scheme.equals("http")? Application.tcpPort : Application.tlsPort;
    }

    public void host(String host) {  // defaults to "localhost"
        this.host = host;
    }

    public void port(int port) {   // set automatically for "localhost", needs to be explicitly set for external hosts
        this.port = port;
    }

    public void path(String path) {  // set by serverData() to point to local data, otherwise needs to be explicitly set
        this.path = (host.equals("localhost")||host.equals(Application.hostName))? Application.dataPrefix + path : path;
    }

    public void pathReset() {  // resets to serverData() location if there is any, else to data root
        path = serverDataPath !=null? serverDataPath : Application.dataPrefix;
    }

    public void pathResetAndAdd(String extra) {  // resets to serverData() location if there is any, else to data root then adds more
        pathReset();  pathAdd(extra);
    }

    public void pathAdd(String extra) {  // adds string to end of path (no automatic "/")
        path += extra;
    }

    public void pathRemove(String extra) throws TestException {  // removes string from end of path
        if (!path.endsWith(extra)) fail("pathRemove() called with text that doesn't exist");
        path = path.substring(0,path.length()-extra.length());
    }

    public void query(String query)    {  // set the query parameters (without leading "?") e.g. "metadata=all&depth=1", or empty to disable
        this.query = query;
    }

    public void alt(String alt)  {   // one of "json","xml","plain","none","media","default" - default is set by Application.testFormat
        if (alt.equals("default")) alt = Application.testFormat;
        this.alt = alt;
        switch (alt) {
            case "json":  contentType = "application/json"; break;
            case "xml":   contentType = "application/xml";  break;
            case "plain": contentType = "text/plain";       break;
        }
    }

    public void requestHeader(String name, String value) {  // sets a request header; passing a null value will remove a previously set header
        if (value == null) {
            if (requestHeaders != null) requestHeaders.remove(name);
        }
        else {
            if (requestHeaders == null) requestHeaders = new HashMap<String,List<String>>();
            List<String> list = requestHeaders.get(name);
            if (list == null) list = new ArrayList<String>();
            list.add(value);
            requestHeaders.put(name,list);
        }
    }

    public void requestText(String text)    { // sets entire request body - mutually exclusive with clientData()
        requestText = text;
        clientData  = null; // mutually exclusive
    }

    public void definition(String toParse) throws TestException { // adds a definition, with no <Definitions> wrapper, for this test; call once for each def to be added
        try { parseDefinition(toParse);}
        catch (XDException e) { fail("definition(" + limitedText(toParse) + ")", e);}
    }

    public void serverData(String toParse) throws TestException {  // makes local target data in server and sets host,port,path to match location of data on server
        try { serverData(parseServerInstance(toParse)); }
        catch (XDException e) { fail("serverData(\"" + limitedText(toParse) + "\")", e);}
    }

    public void serverData(Data target) throws TestException {  // makes local target data in server and sets host,port,path to match location of data on server
        try {
            serverDataPath = Session.atomicPost("Test.serverData", getServerTestDataPath(), target);
            serverData = null; // serverData() sets the data in the database but does not set member serverData yet, that is done by expectServerData()
            host = Application.hostName;    // defaults to "localhost" for testing;
            path = serverDataPath; // will include {prefix}
            port = scheme.equals("http")? Application.tcpPort : Application.tlsPort;
        } catch (XDException e) { fail("serverData(data)", e);}
    }

    public void serverFile(String name, String contents) throws TestException {  // makes a temporary local target file in server
        try {
            if (testFileNames == null) testFileNames = new ArrayList<>();
            testFileNames.add(name);  // these file names will be deleted by close()
            File dir = new File(Application.webroot + getServerTestFilePath());
            if (!dir.exists()) { fail("Directory '" + Application.webroot + getServerTestFilePath() + "' for creating test files does not already exist... something is wrong... don't want to write files in the wrong place... aborting test is the only safe thing to do."); }
            FileWriter writer = new FileWriter(new File(Application.webroot + getServerTestFilePath() + "/" + name));
            writer.write(contents);
            writer.close();
        }
        catch (IOException e) { fail("serverFile(\""+name+"\",...)", e); }
    }

    public void clientData(String toParse) throws TestException { // sets data for the client side: a destination for GET or a source for PUT/POST (mutually exclusive with requestText())
        try { clientData(parseClientInstance(toParse)); }
        catch (XDException e) { fail("clientData(" + limitedText(toParse) + ")", e);}
    }

    public void clientData(Data data)   {  // sets data for the client side: a destination for GET or a source for PUT/POST (mutually exclusive with requestText())
        clientData = data;
        requestText = null; // mutually exclusive with requestText()
    }

    public void clientDataPars(String clientDataPars)  { // controls the generation of clientData for PUT and POST, e.g., "metadata=cat-ui"
        this.clientDataPars = clientDataPars;
    }

    public void get()  throws TestException {    // GETs the data at scheme://host:port/path and sets responseCode and responseText
        try { doHttp("GET"); }
        catch (TestException e) { fail("get(): " + e.getLocalizedMessage());}
    }

    public void put() throws TestException  {    // PUTs the clientData to scheme://host:port/path and sets responseCode and responseText
        try { doHttp("PUT"); }
        catch (TestException e) { fail("put(): " + e.getLocalizedMessage());}
    }

    public void post() throws TestException  {   // POSTs the clientData to scheme://host:port/path and sets responseCode and responseText
        try { doHttp("POST"); }
        catch (TestException e) { fail("post(): " + e.getLocalizedMessage());}
    }
    public void delete() throws TestException {   // DELETEs the resource at scheme://host:port/path and sets responseCode and responseText
        try { doHttp("DELETE"); }
        catch (TestException e) { fail("delete(): " + e.getLocalizedMessage());}
    }

    public void expectResponseHeaderPresent(String name) throws TestException {
        try {
            if (responseHeaders == null)             throw new XDException(Errors.TEST_FAILURE,"Expected header "+name+" not found in response");
            List<String> values = responseHeaders.get(name);
            if (values == null || values.size()==0)  throw new XDException(Errors.TEST_FAILURE,"Expected header "+name+" not found in response");
        } catch (XDException e) { fail("expectResponseHeaderPresent(" + name + ")", e); }
    }

    public void expectResponseHeaderAbsent(String name)  throws TestException {
        try {
            if (responseHeaders != null) {
                List<String> values = responseHeaders.get(name);
                if (values != null && values.size()!=0)  throw new XDException(Errors.TEST_FAILURE,"Expected header "+name+" to be absent, but was found");
            }
        } catch (XDException e) { fail("expectResponseHeaderAbsent(" + name + ")", e); }
    }

    public void expectResponseHeaderValue(String name, String value) throws TestException {
        try {
            if (responseHeaders == null)             throw new XDException(Errors.TEST_FAILURE,"Expected header "+name+" not found in response");
            List<String> values = responseHeaders.get(name);
            if (values == null || values.size()==0)  throw new XDException(Errors.TEST_FAILURE,"Expected header "+name+" not found in response");
            if (!values.get(0).equals(value))        throw new XDException(Errors.TEST_FAILURE,"Expected header "+name+" has value of '"+values.get(0)+"' not expected value of '"+value+"'");
        } catch (XDException e) { fail("expectResponseHeaderValue(" + name + "," + value + ")", e); }
    }

    public void expectStatusCode(int code) throws TestException {   // expects a specific response status code
        try {if (responseCode != code) throw new XDException(Errors.TEST_FAILURE,"Response code "+responseCode+" was not expected value of "+code);}
        catch (XDException e) { fail("expectStatusCode(" + code + ")", e);}
    }

    public void expectSuccessCode() throws TestException {   // expects the response status code to be 200-299
        try { if (responseCode < 200 || responseCode > 299) throw new XDException(Errors.TEST_FAILURE,"Response code "+responseCode+" was not in expected success code range",responseText); }
        catch (XDException e) { fail("expectSuccessCode()", e);}
    }

    public void expectFailureCode() throws TestException {   // expects an error status code of some kind
        try { if (responseCode >=200 && responseCode <= 299) throw new XDException(Errors.TEST_FAILURE,"Response code "+responseCode+" was not in expected error code range"); }
        catch (XDException e) { fail("expectFailureCode()", e);}
    }

    public void expectResponseText() throws TestException {
        if (responseText == null || responseText.isEmpty())  fail("expectResponseText()");
    }

    public void expectResponseText(String body) throws TestException {
        expectResponseText();
        if (!responseText.equals(body)) fail("expectResponseText(" + limitedText(body) + "): found '" + limitedText(responseText) + "'");
    }

    public void expectResponseTextContains(String fragment) throws TestException{
        expectResponseText();
        if (!responseText.contains(fragment)) fail("expectResponseTextContains(" + limitedText(fragment) + ")");
    }

    public void expectResponseTextStartsWith(String fragment) throws TestException {
        expectResponseText();
        if (!responseText.startsWith(fragment)) fail("expectResponseTextStartsWith(" + limitedText(fragment) + ")");
    }

    public void expectErrorNumber(int number) throws TestException {
        expectResponseTextStartsWith("? " + number);
    }

    public void allowResponseDefinitions() {   // if you don't call this first, any definitions in the response data will be an error
        responseDefinitions =  new DataList();
    }
    public void allowResponseTagDefinitions() {   // if you don't call this first, any tag definitions in the response data will be an error
        responseTagDefinitions = new DataList();
    }
    public DataList getResponseDefinitions() {
        return responseDefinitions != null? responseDefinitions :  new DataList();
    }
    public DataList getTagResponseDefinitions() {
        return responseTagDefinitions != null? responseTagDefinitions :  new DataList();
    }

    private DefinitionCollector responseDefinitionCollector = new DefinitionCollector() {
        @Override public void addDefinition(Data definition) throws XDException {
            if (responseDefinitions == null) throw new XDException(Errors.CANNOT_CREATE,"Definitions not expected in response data");
            else responseDefinitions.add(definition);
        }
        @Override public void addTagDefinition(Data definition) throws XDException {
            if (responseTagDefinitions == null) throw new XDException(Errors.CANNOT_CREATE,"TAg definitions not expected in response data");
            else responseTagDefinitions.add(definition);
        }
    };

    public void expectResponseData() throws TestException {   // expects to be able to parse responseText into responseData, based on 'alt'
        expectResponseText();
        expectSuccessCode();
        try { if (responseData == null) responseData = DataParser.parse(responseText, alt, responseDefinitionCollector); }
        catch (XDException e) { fail("expectResponseData()", e); }
    }

    public void expectResponseDataItemPresent(String path) throws TestException{   // expects responseData item at path to exist
        expectResponseData();
        try { Eval.eval(responseData, path, Eval.FOR_GET); }
        catch (XDException e) { fail("expectResponseDataItemPresent(" + path + ")"); }
    }

    public void expectResponseDataItemAbsent(String path) throws TestException {    // expects responseData item at path to be absent
        expectResponseData();
        try {
            Data data = Eval.eval(responseData, path, 0);  // 0 means raw eval (not the default FOR_GET which will do fabricated stuff )
            if (data != null) fail("expectResponseDataItemAbsent(" + path + ")");
        } catch (XDException e) {} // that's OK, we want it to be absent
    }

    public void expectResponseDataItemValue(String path, String value) throws TestException { // expects responseData item at path to be present and have given value
        expectResponseData();
        Data data = Eval.eval(responseData, path, (Data)null);
        if (data == null) fail("expectResponseDataItemValue(" + path + "," + value + "): data item does not exist");
        if (!data.stringValue("<none>").equals(value))  fail("expectResponseDataItemValue(" + path + "," + value + "): data item has value '" + data.stringValue("<none>") + "'");
    }

    public void expectClientData() throws TestException { // expects that clientData can be updated by a valid responseData
        expectResponseData();
        try { // be lenient - if clientData() was not called, set clientData to an Any
            if (clientData == null) clientData = alt.equals("plain")? new StringData("..clientData") : new AnyData("..clientData");
            clientData = clientData.put(responseData,Data.PUT_OPTION_USE_CLIENT_RULES);
        } catch (XDException e) { fail("expectClientData()", e);}
    }

    public void expectClientData(String toParse) throws TestException {  // expects updated clientData to exactly match the given xml/json
        try { expectClientData(parseClientInstance(toParse));}
        catch (XDException e) { fail("expectClientData(" + limitedText(toParse) + ")", e);}
    }

    public void expectClientData(Data expected) throws TestException { // expects clientData to exactly match the given data
        expectClientData();
        compare(Path.toPath(clientData), expected, clientData);
    }

    public void expectClientDataItemPresent(String path) throws TestException { // expects item at path to be present in updated clientData
        expectClientData();
        try { Eval.eval(clientData, path,Eval.FOR_GET); }
        catch (XDException e) { fail("expectClientDataItemPresent(" + path + ")", e); }
    }

    public void expectClientDataItemAbsent(String path) throws TestException { // expects item at path to be absent in updated clientData
        expectClientData();
        Data data = Eval.eval(clientData, path, (Data)null);
        if (data != null) fail("expectClientDataItemAbsent(" + path + ") is present");
    }

    public void expectClientDataItemValue(String path, String value) throws TestException { // expects item at path to be present and have given value in updated clientData
        expectClientData();
        Data data = Eval.eval(clientData, path, (Data)null);
        if (data == null) fail("expectClientDataItemValue(" + path + "," + value + "): data item does not exist");
        if (!data.stringValue("<none>").equals(value)) fail("expectClientDataItemValue(" + path + "," + value + "): data item has value '" + data.stringValue("<none>") + "'");
    }

    public void expectServerData(String toParse) throws TestException {     // expects serverData to be exactly equal to provided xml/json
        try { expectServerData(parseServerInstance(toParse)); }
        catch (XDException e) { fail("expectServerData(" + toParse + ")", e);}
    }

    public void expectServerData() throws TestException {      // expects serverData to have been updated by valid data (not ParsedData)
        if (serverDataPath == null) fail("expectServerData(): serverData was not initialized");
        try { serverData = Session.atomicGetCopy("Test.expectServerData",serverDataPath); } // makes an in-memory copy for later use
        catch (XDException e) { fail("expectServerData() failed to get from database",e); }
    }

    public void expectServerData(Data expectedData) throws TestException { // expects serverData to be exactly equal to provided data
        expectServerData();
        compare(serverDataPath, expectedData, serverData);
    }

    public void expectServerDataItemPresent(String path) throws TestException { // expects serverData item at path to be present
        expectServerData();
        Data data = Eval.eval(serverData, path, (Data)null);
        if (data == null) fail("expectServerDataItemPresent(" + path + "): failed");
    }

    public void expectServerDataItemAbsent(String path) throws TestException { // expects item at path to be absent in updated serverData
        expectServerData();
        Data data = Eval.eval(serverData, path, (Data)null);
        if (data != null) fail("expectServerDataItemPresent(" + path + "): failed");
    }

    public void expectServerDataItemValue(String path, String value) throws TestException { // expects item at path to be present and have given value in updated serverData
        expectServerData();
        Data data = Eval.eval(serverData, path, (Data)null);
        if (data == null) fail("expectServerDataItemValue(" + path + "," + value + "): data item does not exist");
        if (!data.stringValue("<none>").equals(value)) fail("expectServerDataItemValue(" + path + "," + value + "): data item has value '" + data.stringValue("<none>") + "' and expected '"+value+"'");
    }

    public void delay(int timeout) throws TestException { //  will wait for a while with no condition to terminate early
        try { Thread.sleep(timeout); }
        catch (Throwable t) { fail("delay" + timeout + "): " + t.getLocalizedMessage()); }
    }

    public void fail(String reason, XDException cause) throws TestException { fail(reason + ":\n   " + cause.getLocalizedMessage()); }
    public void fail(String reason, Exception cause)   throws TestException { fail(reason + ":\n   " + cause.getLocalizedMessage()); }

    public void fail(String reason) throws TestException {
        throw new TestException(reason);
    }

    //////////////////////////////////////////////////////
    // public helpers for writing custom assertions  //
    //////////////////////////////////////////////////////

    public String getResponseHeader(String name, String defaultValue) throws TestException{
        try { expectSuccessCode(); }
        catch (TestException e) { return defaultValue; }
        if (responseHeaders == null)  return defaultValue;
        List<String> values = responseHeaders.get(name);
        if (values == null || values.size()==0) return defaultValue;
        return values.get(0);
    }

    public Data getResponseData() throws TestException {
        expectResponseData();
        return responseData;
    }

    public Data getClientData() throws TestException {
        expectClientData();
        return clientData;
    }

    public Data getServerData() throws TestException {
        expectServerData();
        return serverData;
    }

    public String limitedText(String text) {  // don't overwhelm error messages
        text = text.replaceAll("\n"," ");
        return (text.length() > 40)? text.substring(0,40)+"..."  : text;
    }

    // TODO someday change these to be variable for off-box tests
    public String getServerTestDataPath()  { return Application.dataPrefix + "/" +Application.testDataPath;  }
    public String getServerDataPrefix()    { return Application.dataPrefix; }
    public String getServerFilePrefix()    { return Application.filePrefix; }
    public String getServerTestFilePath()  { return Application.filePrefix + "/" +Application.testFilePath; }
    public String getServerBaseHttpURI()   { return Server.getHttpBaseServerURI(); }
    public String getServerBaseHttpsURI()  { return Server.getHttpsBaseServerURI(); }


    public void parseDefinition(String toParse) throws XDException {
        Data definition = DataParser.parse(toParse,parsingOptions|Parser.OPTION_IS_DEFINITION);
        // we remember the names of the definitions we create so close() can remove them
        if (testDefinitionNames == null) testDefinitionNames = new ArrayList<>();
        testDefinitionNames.add(definition.getName());
        Definitions.addDefinition(definition);
    }

    public Data parseServerInstance(String text) throws XDException {
        return Instances.makeInstance(DataParser.parse(text, parsingOptions)); // clean it up into a proper server-side instance
    }

    public Data parseClientInstance(String text) throws XDException {
        Data parsed = DataParser.parse(text,parsingOptions);
        Data results = Instances.makeInstance(parsed.stringValueOf(Meta.TYPE, null), parsed.getBase(), parsed.getName());
        results.put(parsed,Data.PUT_OPTION_USE_CLIENT_RULES);  // retain client-side things like $partial, $truncated, etc.
        return results;
    }

    ///////////////////////////////////////////////////
    //////////////// private helpers //////////////////
    ///////////////////////////////////////////////////


    private void clearResponse() {
        responseCode = 0;
        responseText = null;
        responseData = null;
    }

    static { //for localhost testing only
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
            new javax.net.ssl.HostnameVerifier(){
                public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                    return hostname.equals("localhost")|| hostname.equals(Application.hostName); // return true (verifies OK) for localhost, false for everything else to use normal mechanisms
                }
            });
    }

    private void doHttp(String method) throws TestException {
        try {
            clearResponse();
            String uri = makeURI();
            HttpURLConnection connection = (HttpURLConnection)new URL(uri).openConnection();
            if (    query.contains("alt=plain") && !alt.equals("plain") ||
                    query.contains("alt=xml") && !alt.equals("xml") ||
                    query.contains("alt=json") && !alt.equals("json"))
                throw new TestException("Query pars contain conflicting alt - use alt() method to set");
            connection.setRequestMethod(method);
            connection.setDoOutput(method.equals("PUT") || method.equals("POST"));
            connection.setInstanceFollowRedirects(false);
            if (!contentType.isEmpty()) {
                connection.setRequestProperty("Content-Type", contentType);
                if (method.equals("GET")) connection.setRequestProperty("Accept", contentType);
            }
            if (requestHeaders != null) {
                for (Map.Entry<String,List<String>> entry : requestHeaders.entrySet()) {
                    List<String> list = entry.getValue();
                    for (String value : list) connection.setRequestProperty(entry.getKey(), value);
                }
            }
            if (method.equals("PUT")|| method.equals("POST")) {
                if (clientData!=null) clientData.setContext(new Context("Test.doHttp()",clientDataPars!=null?clientDataPars:""));
                OutputStream os = connection.getOutputStream();
                PrintWriter  writer = new PrintWriter(os);
                if (requestText != null) writer.print(requestText); // requestText overrides clientData
                else if (alt.equals("json"))  new JSONGenerator().generate(writer,clientData);
                else if (alt.equals("xml"))   new XMLGenerator().generate(writer,clientData);
                else if (alt.equals("plain")) new PlainGenerator().generate(writer,clientData);
                else fail(method+": alt() is not xml/json/plain and no requestText() has been set");
                writer.flush();
                os.flush();
            }
            //
            responseCode = connection.getResponseCode();     // causes the GET/PUT/POST/DELETE to happen
            //
            responseHeaders = connection.getHeaderFields();
            InputStream responseStream =null;
            if (responseCode >=200 && responseCode <= 399) responseStream = connection.getInputStream();
            else                                           responseStream = connection.getErrorStream();
            if (responseStream != null) responseText = new PlainParser().parse(new InputStreamReader(responseStream),null,null,0,null).stringValue(); // a bit of a convoluted way to getText(stream), but saves recreating that code here
            else responseText = null;
            connection.disconnect();
        }
        catch (Exception e ) {
            fail("Exception in HttpURLConnection: " + e +":"+ e.getLocalizedMessage());
        }
    }

    private String makeURI() {
        String result;
        result = scheme + "://" + host;
        if (!(scheme.equals("http") && port==80 || scheme.equals("https") && port==443)) result += ":"+port;
        result += path;
        if (!query.isEmpty()) result += "?" + query;  // append the query, if any
        if (!(alt.equals("none")||alt.equals("json"))) { // then append alt, if needed
            if (query.isEmpty()) result += "?alt=" + alt;
            else                 result += "&alt=" + alt;
        }
        return result;
    }

    private void setRequestHeadersInConnection(HttpURLConnection connection) {
        if (requestHeaders != null) {
            for (Map.Entry<String,List<String>> entry : requestHeaders.entrySet()) {
                List<String> list = entry.getValue();
                for (String value : list) connection.setRequestProperty(entry.getKey(), value);
            }
        }
    }

    private static int parsingOptions = Parser.OPTION_NO_HEADER|Parser.OPTION_NO_NAMESPACE| // XML can be stripped down to just "<String.../>"
                    Parser.OPTION_ALLOW_UNQUOTED_NAMES| Parser.OPTION_ALLOW_SINGLE_QUOTES;  // JSON can be native Javascript format like: "{$value:'a'}"


    private void compare(String path, Data expected, Data received) throws TestException {
        try {
            if (expected.find(Meta.AMPII_MATCH_ANY) != null) return;
            if (expected.getBase() != received.getBase() && received.getBase() != Base.ANY)
                fail(path + ": Not even close: expected " + expected.getBase() + " got " + received.getBase());
            if (expected.hasValue() && !received.hasValue()) fail(path + ": Expected a value and received none");
            if (!expected.hasValue() && received.hasValue()) fail(path + ": Expected no value and received one");
            for (Data expectedMeta : expected.getMetadata()) {
                Data receivedMeta = received.find(expectedMeta.getName());
                String expectedMetaName = expectedMeta.getName();
                if (expectedMetaName.startsWith("$.."))
                    continue; // we never receive anything starting with .., that's only for notations on our side
                if (receivedMeta == null)
                    fail(path + " Expected metadata " + expectedMeta.getName() + " not present in result");
                compare(path + "/" + expectedMeta.getName(), expectedMeta, receivedMeta);
            }
            for (Data receivedMeta : received.getMetadata()) {
                if (receivedMeta.getName().equals(Meta.SELF)) continue; //TODO: check that this is only at the top level
                if (receivedMeta.getName().equals(Meta.TYPE)) { // we don't care about an unexpected $type as long as it's OK
                    if (!receivedMeta.stringValue().equals(expected.getEffectiveType()))
                        fail(path + ": Received $type: '" + receivedMeta.stringValue() + "' does not match effective type of '" + expected.getEffectiveType() + "'");
                    continue;
                }
                if (expected.find(receivedMeta.getName()) == null)
                    fail(path + ": Received unexpected metadata: " + receivedMeta.getName() + " with value '" + receivedMeta.stringValue() + "'");
            }
            // Yuck! comparing unordered lists is a pain because the order (and thus the names of the children) is not guaranteed and can be rearranged by the server at will
            if (expected.getBase() == Base.LIST || expected.getBase() == Base.SEQUENCEOF) {
                for (Data expectedChild : expected.getChildren()) {
                    boolean found = false;
                    StringBuilder mismatchs = new StringBuilder();
                    for (Data receivedChild : received.getChildren()) {
                        try {
                            // the names are not significant in unordered lists, so we do a deep comparison of the contents of each item looking for a match
                            compare(path + "/" + expectedChild.getName(), expectedChild, receivedChild);
                            found = true; // if compare() didn't complain, then we found a match
                        } catch (TestException e) {
                            mismatchs.append("Comparing '" + receivedChild.getName() + "' gives: " + e.getMessage() + "\n");
                        } // remember reason, leave found false
                        if (found) break;
                    }
                    if (!found) {
                        fail(path + ": List or SequenceOf member not found to match expected " + expectedChild.toString() + "\n" + mismatchs.toString());
                    }
                }
                for (Data receivedChild : received.getChildren()) {
                    boolean found = false;
                    for (Data expectedChild : expected.getChildren()) {
                        try {
                            compare(path + "/" + receivedChild.getName(), expectedChild, receivedChild);
                            found = true; // if compare() didn't complain, then we found a match
                        } catch (TestException e) {
                        } // leave found false
                        if (found) break;
                    }
                    if (!found)
                        fail(path + ": Unexpected List or SequenceOf member found in received data " + receivedChild.toString());
                }
            } else { // all other base types the names are useful for comparison (even array's numbered members because they are in order)
                for (Data expectedChild : expected.getChildren()) {
                    Data receivedChild = received.find(expectedChild.getName());
                    if (receivedChild == null)
                        fail(path + " Expected child " + expectedChild.getName() + " not present in result");
                    compare(path + "/" + expectedChild.getName(), expectedChild, receivedChild);
                }
                for (Data receivedChild : received.getChildren()) {
                    if (expected.find(receivedChild.getName()) == null)
                        fail(path + ": Received unexpected child: " + receivedChild.getName());
                }
            }
            // now do value
            if (expected.hasValue()) {
                Object expectedValue = expected.getValue();
                // if value is a set of localized strings, we have some work to do, otherwise, just compare the stringValue() of the two
                if (expectedValue instanceof LocalizedStrings) {
                    Object receivedValue = received.getValue();
                    if (!(receivedValue instanceof LocalizedStrings))
                        fail(path + ": Expected a set of localized values, but received '" + received.stringValue() + "'");
                    if (!(receivedValue.equals(expectedValue)))
                        fail(path + ": Received set of localized strings is not equal to expected set");
                } else if (expectedValue != null && expected.find(Meta.AMPII_MATCH_ANY) == null && !expected.stringValue().equals(received.stringValue()))
                    fail(path + ": Received value: '" + received.stringValue() + "' does not match expected value '" + expected.stringValue() + "'");
            }
            // more?...
        }
        catch (XDException e ) { throw new TestException(e.getLocalizedMessage()); }
    }

}
