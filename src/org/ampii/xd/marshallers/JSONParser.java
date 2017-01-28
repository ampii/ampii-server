// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.marshallers;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.LocalizedString;
import org.ampii.xd.common.LocalizedStrings;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.definitions.DefinitionCollector;
import org.ampii.xd.data.basetypes.ParsedData;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Parses JSON text into {@link ParsedData} items.
 *
 * @author daverobin
 */
public class JSONParser extends Parser {

    /**
     * Generally, you should use one of the many specific {@link DataParser}.parse(...) methods rather than this ugly workhorse.
     * The sourceURL and/or sourceName params can be null if n/a
     */
    public Data parse(Reader reader, URL sourceURL, String sourceName, int options, DefinitionCollector definitionCollector) throws XDException {
        begin(reader, sourceURL, sourceName, options);
        Data result = consumeObject("anonymous", definitionCollector, hasOption(OPTION_IS_DEFINITION));
        finish();
        return result;
    }

    ////////////////////////////////////////


    private Data consumeObject(String name, DefinitionCollector definitionCollector, boolean isDefinition) throws XDException {
        Data info = makeParsedData(name);
        info.setIsDefinition(isDefinition);
        String order = null;
        expect('{');
        if (peekNextNonwhite()!='}') for(;;) {
            String memberName = hasOption(OPTION_ALLOW_UNQUOTED_NAMES)? consumeOptionallyQuoted() : consumeQuoted();
            expect(':');
            if (memberName.equals("$value")) {
                info.setValue(consumePrimitiveValue());
            }
            else if (memberName.equals("$name")) {
                info.setName(consumePrimitiveValue());
            }
            else if (memberName.equals("$base")) {
                info.setBase(Base.fromString(consumePrimitiveValue()));
            }
            else if (memberName.equals("$$definitions")) {
                for (Data def : consumeObject(memberName,definitionCollector,true).getChildren()) definitionCollector.addDefinition(def);
            }
            else if (memberName.equals("$$tagDefinitions")) {
                for (Data def : consumeObject(memberName,definitionCollector,true).getChildren()) definitionCollector.addTagDefinition(def);
            }
            else if (memberName.equals("$$includes")) {
                for (Data link : consumeObject(memberName,definitionCollector,false).getChildren()) {
                    // only Links are allowed...
                    String file = link.stringValue(); // the file name is in the value of the Link
                    if (file == null) throw complaint("The include Link is missing a 'value'");
                    if (sourceURL == null && !(file.startsWith("http")||file.startsWith("HTTP")||file.startsWith("https")||file.startsWith("HTTPS"))) throw complaint("Can't use relatives links for $$includes in data that is not from a file or URL");
                    URL url;
                    try { url = new URL(sourceURL,file); }
                    catch (MalformedURLException e) { throw complaint("The 'file' \"" + file + "\" could not be combined with context \"" + sourceURL + "\"");}
                    Data incoming = DataParser.parse(url,definitionCollector);
                    // if we got back a wrapper, unwrap the children, else just add the single data item
                    if (incoming.getName().equals(".csml")) {
                        for (Data child : incoming.getChildren()) info.addLocal(child);
                    }
                    else info.addLocal(incoming);
                }
            }
            else if (memberName.equals("$$defaultLocale")) {
                defaultLocale = consumePrimitiveValue();
            }
            else if (memberName.equals("$$org.ampii.comment")) { // consume comments here
                if (peekNextNonwhite() == '{') consumeObject(memberName,definitionCollector,isDefinition);
                else                           consumePrimitiveValue();
            }
            else if (memberName.equals("$$order")) {  // order will be processed at end of object
                order = consumePrimitiveValue();
            }
            else if (memberName.contains("$$")) {          // it's a localized metadata value
                int index = memberName.indexOf("$$");
                String locale = memberName.substring(index + 2); // get the locale from after the "$$"
                memberName = memberName.substring(0, index);      // get the metadata name from before the "$$"
                if (memberName.equals("$value")) {
                    info.setValue(new LocalizedString(locale, consumePrimitiveValue()));
                }
                else {
                    Data meta = info.getOrCreate(memberName);        // this format only works for standard metadata because otherwise we wouldn't know the base
                    meta.setValue(new LocalizedString(locale, consumePrimitiveValue()));
                }
            }
            else {
                Data member = consumeMember(memberName,definitionCollector,isDefinition);
                if (!Rules.isMetadata(member)) member.setName(Rules.getNextAvailableChildName(info,member.getName()));
                info.addLocal(member);
            }
            if (peekNextNonwhite() != ',' ) break;
            expect(',');
        }
        expect('}');
        if (order != null) sort(info, order); // sort children by $$order
        // a little clean up at the end...
        if (info.getName().equals(".csml")) { // if it's named ".csml", then it must be of type Collection
            if (info.getBase() == Base.POLY) info.setBase(Base.COLLECTION);
            else if (info.getBase() != Base.COLLECTION) throw complaint(Errors.INCONSISTENT_VALUES,"Data named '.csml' must be of base type Collection");
        }
        return info;
    }

    /**
     * consumes either an object, an array, or a primitive
     */
    private Data consumeMember(String memberName, DefinitionCollector definitionCollector, boolean isDefinition) throws XDException {
        if      (peekNextNonwhite() == '[') return consumeArray(memberName, definitionCollector, isDefinition);
        else if (peekNextNonwhite() == '{') return consumeObject(memberName,definitionCollector,isDefinition);
        else                                return consumePrimitive(memberName,isDefinition);
    }

    private Data consumePrimitive(String memberName, boolean isDefinition) throws XDException {
        Data member = makeParsedData(memberName);
        member.setIsDefinition(isDefinition);
        member.setValue(consumePrimitiveValue());
        return member;
    }

    private Data consumeArray(String name, DefinitionCollector definitionCollector, boolean isDefinition) throws XDException {
        Data info = makeParsedData(name);
        info.setIsDefinition(isDefinition);
        expect('[');
        if (peekNextNonwhite()!=']') for(int i=1;;i++) {
            String memberName = String.valueOf(i); // array members get names starting with "1"
            Data member = consumeMember(memberName, definitionCollector, isDefinition);
            info.addLocal(member);
            if (peekNextNonwhite() != ',' ) break;
            expect(',');
        }
        expect(']');
        return info;
    }

    private void sort(Data data, String order) throws XDException {
        DataList children = data.getChildren();
        DataList newChildren = new DataList();  // make a temporary list; we don't really modify till we're done
        String[] names = order.split(";"); // order is a semicolon separated list of child names
        if (names.length != children.size()) throw new XDException(Errors.INCONSISTENT_VALUES, "Number of names in given sort order does not match number of children",this);
        for (String name : names) {  // for all the names in order, if child is found add it to the new list
            boolean found = false;
            for (Data child : children)
                if (child.getName().equals(name)) {
                newChildren.add(child);
                found = true;
                break;
            }
            if (!found) throw new XDException(Errors.INCONSISTENT_VALUES,"Specified sort order contains '" + name + "' that is not currently a child",this);
        }
        // if that succeeded without throwing, then we can actually modify the children
        // (since this is all within a parser, this carefulness is really overkill since this is non-committed data, but, hey, good programming practice)
        for (Data child: children)    data.removeLocal(child);
        for (Data child: newChildren) data.addLocal(child);
    }

    private String consumePrimitiveValue() throws XDException {
        if      (peekNextNonwhite() == '"')                                           return consumeDoubleQuoted();
        else if (peekNextNonwhite() == '\'' && hasOption(OPTION_ALLOW_SINGLE_QUOTES)) return consumeSingleQuoted();
        else                                                                          return consumeUnquoted();
    }

    private String consumeUnquoted() throws XDException {
        return consumeUntil(" \t\r\n,}]:");
    }

    public String consumeQuoted() throws XDException { // can't use base class' consumeQuoted() because of JSON's use of \" to escape quotes
        if (peekNextNonwhite() == '\'' && hasOption(OPTION_ALLOW_SINGLE_QUOTES)) return consumeSingleQuoted();
        else return consumeDoubleQuoted();
    }

    public String consumeDoubleQuoted() throws XDException { // can't use base class' consumeQuoted() because of JSON's use of \" to escape quotes
        skipWhitespace();
        expect('"');
        StringBuilder result = new StringBuilder();
        while (peekNext()!= 0) {
            if (peekNext() == '"') break;
            char c = consume();
            result.append(c);
            if (c == '\\' && peekNext() == '"') result.append(consume()); // consume the quote here so we won't see it at the top of the loop and break
        }
        expect('"');
        return unescape(result.toString());
    }

    public String consumeSingleQuoted() throws XDException { // can't use base class' consumeQuoted() because of JSON's use of \" to escape quotes
        skipWhitespace();
        expect('\'');
        StringBuilder result = new StringBuilder();
        while (peekNext()!= 0) {
            if (peekNext() == '\'') break;
            char c = consume();
            result.append(c);
            if (c == '\\' && peekNext() == '\'') result.append(consume()); // consume the quote here so we won't see it at the top of the loop and break
        }
        expect('\'');
        return unescape(result.toString());
    }

    public String consumeOptionallyQuoted() throws XDException { // for names: allows then to optionally be unquoted (native JavaScript style, not JSON)
        skipWhitespace();
        char c = peekNext();
        if      (c == '"') return consumeDoubleQuoted();
        else if (c == '\'' && hasOption(OPTION_ALLOW_SINGLE_QUOTES)) return consumeSingleQuoted();
        else if (hasOption(OPTION_ALLOW_UNQUOTED_NAMES)) return consumeUnquoted();
        else throw complaint("Expected start of quoted string");
    }

    private String unescape(String in) throws XDException {
        StringBuilder out = new StringBuilder();
        try {
            for(int i=0; i<in.length(); i++)  {
                char c = in.charAt(i);
                if (c == '\\') {
                    i++;
                    switch (in.charAt(i)) {
                        case '\\': c = '\\'; break;
                        case '"':  c = '"';  break;
                        case '/':  c = '/';  break;
                        case 'b':  c = '\b'; break;
                        case 'f':  c = '\f'; break;
                        case 'n':  c = '\n'; break;
                        case 'r':  c = '\r'; break;
                        case 't':  c = '\t'; break;
                        case 'u':  String hex = in.substring(i+1,i+5);
                                   c = (char)Integer.parseInt(hex.toString(),16);
                                   i+=4;
                                   break;
                        default: throw complaint(Errors.PARAM_SYNTAX,"Bad escaping (invalid char '"+c+"' after backslash)");
                    }
                }
                out.append(c);
            }
        }
        catch (NumberFormatException e) { throw complaint("Bad escaping (invalid hex digits after '\\u'"); }
        catch (IndexOutOfBoundsException e) { throw complaint("Bad escaping (ran out of chars after '\\'"); }
        return out.toString();
    }


}
