// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.client;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.Log;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Context;
import org.ampii.xd.data.Data;
import org.ampii.xd.marshallers.*;
import org.ampii.xd.server.HTTP;
import org.ampii.xd.server.Request;
import org.ampii.xd.server.Server;
import org.ampii.xd.test.TestException;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Home for some basic client-side helpers.
 *
 * @author drobin
 */
public class Client {

    public static String makeSimpleNextPointer(Context context) throws XDException {
        // no cursors or anything fancy, this just advances the given skip by the given max-results
        Request request   = context.getRequest();
        if (request == null) throw new XDError("Can't create a $next pointer without a valid Request Context");
        if (!context.hasMaxResults()) throw new XDError("Can't make 'simple' next pointer without max-results");
        int skip = context.getSkip();  // we forgive skip being absent, though, because this could be the first time
        int newMaxResults = context.getMaxResults();
        int newSkip       = skip + newMaxResults;
        if (newMaxResults == 0) newMaxResults = 1; // fix evil tester or dumb client that asked for max-results=0
        Map<String, String> newPars = new HashMap<>(request.parameters); // copy current parameters to make new set with skip and max-results
        newPars.put("skip", String.valueOf(newSkip));
        newPars.put("max-results", String.valueOf(newMaxResults));
        return (request.isTLS ? Server.getHttpsBaseServerURI() : Server.getHttpBaseServerURI()) + request.path + "?" + HTTP.encodeParameters(newPars);
    }

    public static void doCallback(String url, String body, String contentType) throws XDException { // used by SubsManager
        int responseCode = -1;
        try {
            Log.logFine("Client Callback: callback start");

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("Accept", contentType);

            OutputStream os = connection.getOutputStream();
            PrintWriter  writer = new PrintWriter(os);
            writer.write(body);
            writer.flush();
            os.flush();

            try { responseCode = connection.getResponseCode(); }
            catch (IOException e) {
                InputStreamReader reader = new InputStreamReader(connection.getErrorStream());
                PlainParser parser = new PlainParser();
                String response = parser.parse(reader, null, null, 0, null).stringValue();
                String failure = "Client doCallback() failed with HTTP response: "+responseCode+" "+connection.getResponseMessage()+"\n"+response;
                connection.disconnect();
                Log.logInfo(failure);
                throw new XDException(Errors.CALLBACK_FAILED,failure);
            }
            responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode > 299) {
                InputStreamReader reader = new InputStreamReader(connection.getErrorStream());
                PlainParser parser = new PlainParser();
                String response = parser.parse(reader, null, null, 0, null).stringValue();
                String failure = "Client doCallback() failed with HTTP response: "+responseCode+" "+connection.getResponseMessage()+"\n"+response;
                connection.disconnect();
                Log.logInfo(failure);
                throw new XDException(Errors.CLIENT_ACTION_FAILED,failure,response);
            }

            Log.logFine("Client Callback: client finished with response code "+responseCode);
            connection.disconnect();
        }
        catch (IOException e) {
            Log.logFine("Client Callback: callback failed: "+e.getMessage());
            throw new XDException(Errors.CALLBACK_FAILED,"Callback Failed with IOException:"+e.getMessage());
        }
    }

    public static Data doHttp(String uri, String method, Data data, String dataPars, String requestText, String alt, String contentType,  Map<String,List<String>> requestHeaders) throws XDException {
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(uri).openConnection();
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
                if (data!=null) data.setContext(new Context("Client.doHttp()",dataPars));
                OutputStream os = connection.getOutputStream();
                PrintWriter  writer = new PrintWriter(os);
                if (requestText != null) writer.print(requestText); // requestText overrides data
                else if (alt.equals("json"))  new JSONGenerator().generate(writer,data);
                else if (alt.equals("xml"))   new XMLGenerator().generate(writer,data);
                else if (alt.equals("plain")) new PlainGenerator().generate(writer,data);
                else throw new XDError("Client.doHttp() given bad alt and no requestText has been set");
                writer.flush();
                os.flush();
            }
            //
            int responseCode = connection.getResponseCode();     // causes the GET/PUT/POST/DELETE to happen!
            //
            Map<String,List<String>> responseHeaders = connection.getHeaderFields();
            InputStream responseStream = null;
            String responseText;
            if (responseCode >=200 && responseCode <= 399) responseStream = connection.getInputStream();
            else                                           responseStream = connection.getErrorStream();
            if (responseStream != null) responseText = new PlainParser().parse(new InputStreamReader(responseStream),null,null,0,null).stringValue(); // a bit of a convoluted way to getText(stream), but saves recreating that code here
            else responseText = "";
            connection.disconnect();
            //
            // on failure, throw
            if (responseCode > 400) throw new XDException(Errors.CLIENT_ACTION_FAILED, "Client received HTTP status "+responseCode+" with body:"+responseText);
            // else cross fingers...
            Data parsed = DataParser.parse(responseText);
            return data.put(parsed);
        }
        catch (Exception e ) {
            throw new XDException(Errors.CLIENT_ACTION_FAILED, "Client.doHttp(): " + e + ":" + e.getLocalizedMessage());
        }
    }

    public static void enableTrustAll() throws Exception {
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[] { new TrustAllX509TrustManager() }, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String string,SSLSession ssls) { return true; }
        });
    }

    private static class TrustAllX509TrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0];}
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
    }


    // The following client methods have been commented out because their code is now outdated and there is no usage for
    // them in the current codebase.
    // The Test.java contains client methods but those are dedicated/tailored to the Test environment.
    // But they could be used as guidance for recreating these general purpose client methods in the future.
    /*
    public static void getData(String uri, Data target, String contentType, Data headers) throws XDException {
        int responseCode = -1;
        Data result;
        try {
            Log.logFine("Client: getData at " + uri);

            HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
            connection.setDoOutput(false); // it's input
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("Accept", contentType);

            if (headers != null) for (Data header : headers.getChildren()) connection.setRequestProperty(header.getName(),header.stringValue());

            responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode > 299) {
                InputStreamReader reader = new InputStreamReader(connection.getErrorStream());
                PlainParser parser = new PlainParser();
                String response = parser.parse(reader, null, null, 0, null).stringValue();
                String failure = "Client getData() failed with HTTP response: "+responseCode+" "+connection.getResponseMessage()+"\n"+response;
                connection.disconnect();
                Log.logInfo(failure);
                throw new XDException(Errors.CLIENT_ACTION_FAILED,failure,response);
            }

            Context context = new Context("Client.getData("+uri+")");
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            String alt = contentType.contains("json")? "json" : contentType.contains("xml")? "xml" : "plain";
            target.put(DataParser.parse(reader,alt));
            connection.disconnect();
        }
        catch (IOException e) {
            String failure = "Client getData() failed with IOException: " + e.getMessage();
            Log.logInfo(failure);
            throw new XDException(Errors.CLIENT_ACTION_FAILED, failure);
        }
    }

    public static void putData(String uri, Data data, String contentType, Data headers) throws XDException {
        int responseCode = -1;
        try {
            Log.logFine("Client: put to " + uri);

            HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
            connection.setDoOutput(true); // it's output
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", contentType);
            if (headers != null) for (Data header : headers.getChildren()) connection.setRequestProperty(header.getName(),header.stringValue());

            OutputStream os = connection.getOutputStream();
            PrintWriter  writer = new PrintWriter(os);
            if (contentType.contains("json")) {
                JSONGenerator generator = new JSONGenerator();
                generator.generate(writer,data);
            }
            else if (contentType.contains("xml")) {
                XMLGenerator generator = new XMLGenerator();
                generator.generate(writer,data);
            }
            else  {
                PlainGenerator generator = new PlainGenerator();
                generator.generate(writer,data);
            }
            writer.flush();
            os.flush();

            responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode > 299) {
                InputStreamReader reader = new InputStreamReader(connection.getErrorStream());
                PlainParser parser = new PlainParser();
                String response = parser.parse(reader, null, null, 0, null).stringValue();
                String failure = "Client put() failed with HTTP response: "+responseCode+" "+connection.getResponseMessage()+"\n"+response;
                connection.disconnect();
                Log.logInfo(failure);
                throw new XDException(Errors.CLIENT_ACTION_FAILED,failure);
            }
            connection.disconnect();
        }
        catch (IOException e) {
            String failure = "Client put() failed with exception: " + e.getMessage();
            Log.logInfo(failure);
            throw new XDException(Errors.CLIENT_ACTION_FAILED, failure);
        }
    }

    public static void postData(String uri, Data data, String contentType, Data headers) throws XDException {
        int responseCode = -1;
        try {
            Log.logFine("Client: post to " + uri);

            HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
            connection.setDoOutput(true); // it's output
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", contentType);
            if (headers != null) for (Data header : headers.getChildren()) connection.setRequestProperty(header.getName(),header.stringValue());

            OutputStream os = connection.getOutputStream();
            PrintWriter  writer = new PrintWriter(os);
            if (contentType.contains("json")) {
                JSONGenerator generator = new JSONGenerator();
                generator.generate(writer,data);
            }
            else if (contentType.contains("xml")) {
                XMLGenerator generator = new XMLGenerator();
                generator.generate(writer,data);
            }
            else  {
                PlainGenerator generator = new PlainGenerator();
                generator.generate(writer,data);
            }
            writer.flush();
            os.flush();

            responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode > 299) {
                InputStreamReader reader = new InputStreamReader(connection.getErrorStream());
                PlainParser parser = new PlainParser();
                String response = parser.parse(reader, null, null, 0, null).stringValue();
                String failure = "Client post() failed with HTTP response: "+responseCode+" "+connection.getResponseMessage()+"\n"+response;
                connection.disconnect();
                Log.logInfo(failure);
                throw new XDException(Errors.CLIENT_ACTION_FAILED,failure);
            }
            connection.disconnect();
        }
        catch (IOException e) {
            String failure = "Client post() failed with exception: " + e.getMessage();
            Log.logInfo(failure);
            throw new XDException(Errors.CLIENT_ACTION_FAILED, failure);
        }
    }

    public static void deleteData(String uri, Data headers) throws XDException {
        int responseCode = -1;
        try {
            Log.logFine("Client: deleteData to " + uri);

            HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
            connection.setDoOutput(true); // it's output
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("DELETE");
            if (headers != null) for (Data header : headers.getChildren()) connection.setRequestProperty(header.getName(),header.stringValue());

            responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode > 299) {
                InputStreamReader reader = new InputStreamReader(connection.getErrorStream());
                PlainParser parser = new PlainParser();
                String response = parser.parse(reader,null,null,0,null).stringValue();
                String failure = "Client deleteData() failed with HTTP response: "+responseCode+" "+connection.getResponseMessage()+"\n"+response;
                connection.disconnect();
                Log.logInfo(failure);
                throw new XDException(Errors.CLIENT_ACTION_FAILED,failure);
            }
            connection.disconnect();
        }
        catch (IOException e) {
            String failure = "Client deleteData() failed with exception: " + e.getMessage();
            Log.logInfo(failure);
            throw new XDException(Errors.CLIENT_ACTION_FAILED, failure);
        }
    }
    */


}
