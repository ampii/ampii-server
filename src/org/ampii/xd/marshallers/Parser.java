// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.marshallers;

import java.net.URL;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.basetypes.ParsedData;
import org.ampii.xd.database.DataStore;
import java.io.IOException;
import java.io.Reader;

// base class for XML and JSON parsers (contains anything common between the two)

public class Parser {

    public static final int OPTION_IS_DEFINITION        = 0x0001; // parsing only definitions; starts in a definition context (without needing <Defintions> or $$definitions wrapper)
    public static final int OPTION_NO_HEADER            = 0x0002; // allows the <?xml...?> to be missing
    public static final int OPTION_NO_NAMESPACE         = 0x0004; // allows the namespace to be missing
    public static final int OPTION_ALLOW_SINGLE_QUOTES  = 0x0008; // allows using single quotes instead of double quotes (for JSON; XML already allows this)
    public static final int OPTION_ALLOW_UNQUOTED_NAMES = 0x0010; // allows unquoted JSON names (native javascript style rather than debatably-wise JSON rules)
    public static final int OPTION_GUESS_BASE           = 0x0020; // allows JSON to guess base type based on text, e.g., for parsing third party data without definitions

    private    Reader  reader;              // setting to 'private' forces use of begin() by subclasses
    protected  URL     sourceURL;
    protected  String  sourceName;
    protected  int     options;             // one of OPTION_XXX, defined here and in subclasses
    protected  int     line;
    protected  int     column;
    protected  int     markedLine;          // markedLine and markedColumn helps error messages to know the start of errorful things
    protected  int     markedColumn;
    protected  char    nextChar;
    protected  String  defaultLocale = DataStore.getDatabaseLocaleString();
    protected  boolean topmost;             // some rules (e.g. namespace declaration) only apply to topmost item

    public void begin(Reader reader, URL sourceURL, String sourceName, int options) {
        this.reader     = reader;
        this.sourceURL  = sourceURL;  // can be null
        this.sourceName = sourceName != null? sourceName : sourceURL != null? sourceURL.toString() : "{noname}";
        this.options    = options;
        line            = 1;
        column          = 1;
        nextChar        = '\0';
        topmost         = true;
    }


    public void finish() { }

    ParsedData makeParsedData(String name) throws XDException { return new ParsedData(name,sourceName,line,column); }

    public boolean hasOption(int option) { return (options&option)!=0; }
    public void    mark()                { markedColumn = column; markedLine = line; }
    public void    clearMark()           { markedColumn = 0;      markedLine = 0; }
    public int[]   getMark()             { return new int[]{markedLine,markedColumn}; }
    public boolean hasMark()             { return markedLine!=0 && !(markedLine==line && markedColumn==column); }


    public XDException complaint(String details)            { return complaint(Errors.VALUE_FORMAT, details,line,column);  }

    public XDException complaint(int error, String details) { return complaint(error, details,line,column);  }

    public XDException complaint(int error, String details, int line, int column) {
        return new XDException(error,"Parsing error at line "+line+" col "+column+" in '"+ sourceURL +"': "+details);
    }

    public void skipWhitespace()  {
        while (" \t\r\n\f".indexOf(peekNext()) != -1) consume();
    }

    public void expect(char expected) throws XDException {
        skipWhitespace();
        expectNext(expected);
    }

    public void expectNext(char expected) throws XDException {
        char found = consume();
        if (found != expected) throw complaint("Expected '"+ escapeCharCStyle(expected)+"' but found '"+ escapeCharCStyle(found)+"'");
    }

    private String escapeCharCStyle(char c) {
        if (c == '\r') return "\\r";
        if (c == '\n') return "\\n";
        if (c == '\t') return "\\t";
        if (c == '\f') return "\\f";
        return Character.toString(c);
    }

    public String consumeUntil(char delimiter) {
        StringBuilder result = new StringBuilder();
        while (peekNext()!= 0 && peekNext() != delimiter) {
            result.append(consume());
        }
        return result.toString();
    }

    public String consumeUntil(String delimiters) {
        StringBuilder result = new StringBuilder();
        while (peekNext()!= 0 && nextIsNotOneOf(delimiters)) {
            result.append(consume());
        }
        return result.toString();
    }

    public String consumeUntilEnd() {
        StringBuilder result = new StringBuilder();
        while (peekNext()!= 0 ) {
            result.append(consume());
        }
        return result.toString();
    }

    public String consumeAnyQuoted() throws XDException {
        skipWhitespace();
        char delimiter;
        if      (peekNext()=='"')  delimiter='"';
        else if (peekNext()=='\'') delimiter='\'';
        else throw complaint("Expected either single or double quote");
        expect(delimiter);
        mark(); // mark start of string
        String result = consumeUntil(delimiter);
        expect(delimiter);
        clearMark();
        return result;
    }

    public String consumeDoubleQuoted() throws XDException {
        skipWhitespace();
        expect('"');
        mark(); // mark start of string
        String result = consumeUntil('"');
        expect('"');
        clearMark();
        return result;
    }

    public boolean nextIsNotOneOf(String delimiters) {
        return (delimiters.indexOf(peekNext()) == -1);
    }

    public char peekNext()  {
        if (nextChar == 0) nextChar = consume();
        return nextChar;
    }

    public char peekNextNonwhite()  {
        skipWhitespace();
        if (nextChar == 0) nextChar = consume();
        return nextChar;
    }

    public char consume()  {
        if (nextChar == 0) {
            try {
                int c = reader.read();
                if (c == -1) return 0;
                if (c == '\n') { line++; column=1; }
                else column++;
                return (char)c;
            } catch (IOException e) { return 0;  }
        }
        else {
            char retval = nextChar;
            nextChar = 0;
            return retval;
        }
    }

}
