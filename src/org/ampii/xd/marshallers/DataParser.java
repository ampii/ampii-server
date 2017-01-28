// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.marshallers;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.definitions.DefinitionCollector;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * General routines for parsing XML/JSON text into Data items.
 * <p>
 * These all have two formats, one with a declared DefinitionsCollector and one without. The ones without a collector
 * will throw exceptions if definitions are encountered.
 *
 * @author daverobin
 */
public class DataParser {

    public static Data parse(File file) throws XDException {
        return parse(file,definitionRejector);
    }

    public static Data parse(File file, DefinitionCollector definitionCollector) throws XDException {  // will guess format based on file extension
        try {
            Reader reader = new FileReader(file);
            return parse(reader, makeURL(file), guessFormat(file), 0, definitionCollector);
        }
        catch (FileNotFoundException e) { throw new XDException(Errors.CANNOT_FOLLOW,"Can't open file '"+file+"'"); }
    }

    public static Data parse(URL url) throws XDException {
        return parse(url,definitionRejector);
    }

    public static Data parse(URL url, DefinitionCollector definitionCollector) throws XDException {  // will guess format based on file extension
        try {
            Reader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            return parse(reader, url, guessFormat(url), 0, definitionCollector);
        }
        catch (IOException e) { throw new XDException(Errors.PARAM_VALUE_FORMAT,"Can't make input stream out of '"+url+"'"); }
    }

    public static Data parse(String string) throws XDException {
        return parse(string,definitionRejector);
    }

    public static Data parse(String string, DefinitionCollector definitionCollector) throws XDException {    // will guess format based on string contents
        return parse(new StringReader(string), null, guessFormat(string), 0, definitionCollector);
    }

    public static Data parse(String string, String format) throws XDException {
        return parse(string,format,definitionRejector);
    }

    public static Data parse(String string, String format, DefinitionCollector definitionCollector) throws XDException {
        return parse(new StringReader(string), null, format, 0, definitionCollector);
    }

    public static Data parse(String string, int options) throws XDException {
        return parse(string,options,definitionRejector);
    }

    public static Data parse(String string, int options, DefinitionCollector definitionCollector) throws XDException {  // will guess format based on string contents
        return parse(new StringReader(string), null, guessFormat(string), options, definitionCollector);
    }

    public static Data parse(Reader reader, String format) throws XDException {
        return parse(reader,format,definitionRejector);
    }

    public static Data parse(Reader reader, String format, DefinitionCollector definitionCollector) throws XDException {
        return parse(reader, null, format, 0, definitionCollector);
    }

    public static Data parse(Reader reader, URL source, String format, int options) throws XDException {
        return parse(reader,source,format,options,definitionRejector);
    }

    public static Data parse(Reader reader, URL source, String format, int options, DefinitionCollector definitionCollector) throws XDException { // source can be null
        if      (format.equals("xml"))  return new XMLParser().parse(reader,source,null,options,definitionCollector);
        else if (format.equals("json")) return new JSONParser().parse(reader,source,null,options,definitionCollector);
        else                            return new PlainParser().parse(reader,null,null,0,null);
    }

    ////////////////////////////////


    private static DefinitionCollector definitionRejector = new DefinitionCollector() {
        @Override public void addDefinition(Data definition) throws XDException {
            throw new XDException(Errors.CANNOT_CREATE,"Definitions not allowed/expected in this context");
        }
        @Override public void addTagDefinition(Data definition) throws XDException {
            throw new XDException(Errors.CANNOT_CREATE,"Tag definitions not allowed/expected in this context");
        }
    };

    private static URL makeURL(File file) throws XDException {
        try { return new URL("file:///"+file.getAbsolutePath()); }
        catch (MalformedURLException e) { throw new XDException(Errors.VALUE_FORMAT, "Can't make URL out of File "+file.getName()); }
    }

    private static String guessFormat(URL url) throws XDException{
        if      (url.toString().toLowerCase().endsWith("xml"))  return "xml";
        else if (url.toString().toLowerCase().endsWith("json")) return "json";
        else                                                    return "plain";
    }

    private static String guessFormat(File file) throws XDException{
        if      (file.getName().toLowerCase().endsWith("xml"))  return "xml";
        else if (file.getName().toLowerCase().endsWith("json")) return "json";
        else                                                    return "plain";
    }

    private static String guessFormat(String s) {
        for (int i = 0; i < s.length(); i++) { // find first non-whitespace character and guess based on '<', '{', or other
            char c = s.charAt(i);
            if (!( c==' ' || c=='\t' || c=='\r' || c=='\n' || c=='\f')) return c == '<'? "xml" : c == '{'? "json" : "plain";
        }
        return "plain"; // empty or all whitespace is plain
    }
}
