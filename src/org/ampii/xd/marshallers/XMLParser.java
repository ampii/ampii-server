// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.marshallers;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import org.ampii.xd.common.*;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.CollectionData;
import org.ampii.xd.data.basetypes.ParsedData;
import org.ampii.xd.database.DataStore;
import org.ampii.xd.definitions.DefinitionCollector;

import javax.xml.bind.DatatypeConverter;

/**
 * Parses XML into {@link ParsedData} items.
 * <p>
 * It's a lot yuckier than the XML generator because of the need to handle ugly things like {@code <Extensions>} and things
 * that have two forms like {@code displayName/<DisplayName>} and {@code value/<Value>}.
 */
public class XMLParser extends Parser {

    /**
     * Generally, you should use one of the many specific {@link DataParser}.parse(...) methods rather than this ugly workhorse.
     * The sourceURL and/or sourceName params can be null if n/a
     */
    public Data parse(Reader reader, URL sourceURL, String sourceName, int options, DefinitionCollector definitionCollector) throws XDException {
        try {
            begin(reader, sourceURL, sourceName, options);
            if (!hasOption(OPTION_NO_HEADER)) consumeXMLHeader();
            Data result = consumeDataElement(definitionCollector, hasOption(OPTION_IS_DEFINITION));
            finish();
            return result;
        }
        catch (XDException e)  {
            e.add("Parsing \"" + this.sourceName + "\" at line " + line + " column " + column);
            if (hasMark()) e.add("Starting from line " + markedLine + " column " + markedColumn);
            throw e;
        }
        catch (Error e)        {
            e.printStackTrace();
            throw new XDError("Parser Error: Parsing \"" + this.sourceName + "\" at line " + line + " column " + column+": "+e.getClass()+":"+e.getLocalizedMessage());
        }
    }

    ////////////////////////////////////////////////

    private CollectionData definitionCollector;

    private Data consumeDataElement(DefinitionCollector definitionCollector, boolean isDefinition) throws XDException {
        mark();
        String tag = consumeElementStart();
        return consumeRestOfDataElement(tag,definitionCollector,isDefinition);
    }

    private Data consumeRestOfDataElement(String tag, DefinitionCollector definitionCollector, boolean isDefinition) throws XDException {
        String     childTag;
        String     grandChildTag;
        Base base;
        mark();

        Data info =  makeParsedData("anonymous");

        // <CSML> is not a real base like <String>, so we have to handle is explicitly. <CSML> becomes a Collection.
        // And we name it with a reserved name so the consumer will know to unwrap the Collection's members
        if (tag.equals("CSML")) {
            base = Base.COLLECTION;
            info.setName(".csml");
        }
        else {
            base = Base.fromString(tag);
            if(base == Base.INVALID) throw complaint("Element <" + tag + "> not allowed here");
        }
        info.setBase(base);
        info.setIsDefinition(isDefinition);

        // consume attributes like 'name', 'value', and 'displayName', as appropriate for the base
        consumeAttributes(info);

        // now process all child XML elements. These can be either metadata like <DisplayName> or data children like <String...>
        while ((childTag = getChildTag(tag)) != null) {
            topmost = false; // if we're doing children, we're obviously not the topmost element
            if (childTag.equals("Includes")) {
                while ((grandChildTag = getChildTag(childTag)) != null) {
                    // only Links are allowed...
                    if (!grandChildTag.equals("Link")) throw complaint("Unexpected <" + grandChildTag + "> under <Includes>");
                    // TODO Figure out what to do about definitions when using "Includes":
                    // SPEC_PROBLEM What is "Includes" allowed to include?  e.g., can it include *defined* members of a sequence/object/composition,
                    // or is it defined by the standard to *only* include members of a "Collection of Any"?
                    Data link = consumeRestOfDataElement(grandChildTag,definitionCollector, isDefinition);
                    String file = link.stringValue(); // the file name is in the value of the Link
                    if (file == null) throw complaint("The Link is missing a 'value'");
                    if (sourceURL == null && !(file.startsWith("http")||file.startsWith("HTTP")||file.startsWith("https")||file.startsWith("HTTPS"))) throw complaint("Can't use relatives links for <Includes> in data that is not from a file or URL");
                    URL url;
                    try { url = new URL(sourceURL,file); }
                    catch (MalformedURLException e) { throw complaint("The 'file' \"" + file + "\" could not be combined with context \"" + sourceURL + "\"");}
                    Data incoming = DataParser.parse(url, definitionCollector);
                    // if we got back a wrapper, unwrap the children, else just add the single data item
                    if (incoming.getName().equals(".csml")) {
                        for (Data child : incoming.getChildren()) info.addLocal(child);// TODO confirm that we ignore metadata of a csml wrapper
                    }
                    else info.addLocal(incoming);
                }
            }
            else if (childTag.equals("Definitions")) {   // there can be multiple "<Definitions> sections, so they have to go in a collection
                while ((grandChildTag = getChildTag(childTag)) != null) definitionCollector.addDefinition(consumeRestOfDataElement(grandChildTag, definitionCollector, true));
            }
            else if (childTag.equals("TagDefinitions")) {   // there can be multiple "<TagDefinitions> sections, so they have to go in a collection
                while ((grandChildTag = getChildTag(childTag)) != null) definitionCollector.addTagDefinition(consumeRestOfDataElement(grandChildTag, definitionCollector, true));
            }
            else if (childTag.equals("Value")) {
                if (base == Base.STRING) {  // this is localizable so we need to handle the default locale and other separately
                    LocalizedString locstr = consumeRestOfElementWithLocalizableBodyText(childTag, false);// these have *optional* locale
                    if (locstr.isDefaultLocale()) info.setValue(locstr.getValue());
                    else info.setValue(locstr);
                }
                else if (base == Base.OCTETSTRING || base == Base.RAW || base == Base.BITSTRING) {
                    info.setValue(consumeRestOfElementWithBase64BodyText(childTag));
                }
                else throw complaint("Base type " + base + " cannot have <Value> child");
            }
            else if (childTag.equals("Extensions")) {
                while ((grandChildTag = getChildTag(childTag)) != null) {
                    Data extension = consumeRestOfDataElement(grandChildTag, definitionCollector, isDefinition);
                    String extensionName = extension.getName();
                    // first see if this is a standard metadata name by turning DisplayName into $displayName and looking up
                    String metaName = "$"+extensionName;
                    if (Rules.isStandardMetadata(metaName)) {
                        // we "merge" rather then "replace" because the metadata value might already exist as attribute
                        // e.g., we already got a displayName attribute and now have a DisplayName extension
                        info.getOrCreate(metaName, null, Base.fromString(grandChildTag)).put(extension);
                    }
                    else { // If it's not a standard name, then it's a proprietary extension, and we just add the whole thing as is
                        extensionName = "$"+extensionName;
                        extension.setName(extensionName);
                        info.addLocal(extension);
                    }
                }
            }
            else if (childTag.equals("NamedValues") || childTag.equals("Choices") || childTag.equals("ValueTags") ||
                childTag.equals("NamedBits") || childTag.equals("Links") || childTag.equals("PriorityArray") || childTag.equals("Failures")) {
                Data meta = info.getOrCreate("$"+lowerize(childTag));
                while ((grandChildTag = getChildTag(childTag)) != null) meta.addLocal(consumeRestOfDataElement(grandChildTag, definitionCollector, isDefinition));
            }
            else if (childTag.equals("MemberTypeDefinition")) {
                Data meta = info.getOrCreate(Meta.MEMBERTYPEDEFINITION); // this is awkwardly a List of 1 item (for some lost historical reason)
                while ((grandChildTag = getChildTag(childTag)) != null) meta.post(consumeRestOfDataElement(grandChildTag, definitionCollector, isDefinition));
                if (meta.getCount()!=1) throw complaint("There must be one-and-only-one child of <MemberTypeDefinition>");
            }
            else if (childTag.equals("Documentation")) {
                Data meta = info.getOrCreate("$" + lowerize(childTag));
                LocalizedString locstr = consumeRestOfElementWithLocalizableBodyText(childTag, false);// these have *optional* locale
                meta.setValue(locstr);
            }
            else if (childTag.equals("Error") || childTag.equals("Units") ||
                    childTag.equals("WritableWhen")  || childTag.equals("RequiredWhen")) {
                Data meta = info.getOrCreate("$" + lowerize(childTag)+"Text");  // Error -> $errorText, Units -> $unitsText, etc.
                LocalizedString locstr = consumeRestOfElementWithLocalizableBodyText(childTag, false);// these have *optional* locale
                meta.setValue(locstr);
            }
            else if (childTag.equals("Description")  || childTag.equals("DisplayName") || childTag.equals("DisplayNameForWriting")) {
                Data meta = info.getOrCreate("$" + lowerize(childTag));
                LocalizedString locstr = consumeRestOfElementWithLocalizableBodyText(childTag, true);// these have *required* locale attribute
                meta.setValue(locstr);
            }
            else {  // none of the special elements above, so it better be a data child like <String>, <List>, etc.
                Data child = consumeRestOfDataElement(childTag,definitionCollector, isDefinition); // must be a real data child like <String>
                child.setName(Rules.getNextAvailableChildName(info,child.getName()));
                info.addLocal(child); // must be a real data child like <String>
            }
        }
        return info;
    }


    private String getChildTag(String expectedEndTag) throws XDException {
        skipWhitespace();
        if (peekNext() == '>') {   // an "open" end tag, thus inviting children
            expectNext('>');
            return getChildTag(expectedEndTag); // recurse to get possible children
        }
        else if (peekNext() == '<') {   // this is either the start of a child or the start of an end tag
            expectNext('<');
            if (peekNext() == '!') { skipRestOfComment(); return getChildTag(expectedEndTag); } // skip comment and try again
            if (peekNext() == '/') {   // start of end tag?
                expectRestOfEndTag(expectedEndTag);
                return null;
            }
            else return consumeUntil(" \t/>");  // else it's the start of a child so return the child tag
        }
        else if (peekNext() == '/') {   // a "close" end tag, so there are no children
            expectNext('/');
            expectNext('>');
            return null;
        }
        else {
            throw complaint("I'm just plain confused. Why is there a '" + peekNext() + "' here?");
        }
    }

    private String lowerize(String name) { return Character.toLowerCase(name.charAt(0)) + name.substring(1); }

    private LocalizedString consumeRestOfElementWithLocalizableBodyText(String expectedEndTag, boolean localeRequired) throws XDException {
        mark();
        String locale = defaultLocale;
        if (hasMoreAttributes()) {
            if (!consumeAttributeName().equals("locale")) throw complaint("Only 'locale' attribute is allowed in a <"+expectedEndTag+">");
            locale = consumeAttributeValue();
        }
        else if (localeRequired) throw complaint("The <" + expectedEndTag + "> element requires a 'locale' attribute");
        if (locale.equals(DataStore.getDatabaseLocaleString())) locale = "";
        String value = consumeBodyText(expectedEndTag);
        return new LocalizedString(locale,value);
    }

    private String consumeRestOfElementWithBase64BodyText(String expectedEndTag) throws XDException {
        if (hasMoreAttributes())  throw complaint("No attributes are allowed for <Value> under <"+expectedEndTag+">");
        try { return DatatypeConverter.printHexBinary(DatatypeConverter.parseBase64Binary(consumeBodyText(expectedEndTag))); } // yuck, converts base64 to hex
        catch (IllegalArgumentException e) { throw complaint("Invalid base64 format data"); }

    }

    private void consumeAttributes(Data info) throws XDException {
        boolean xmlnsSeen = false;
        while (hasMoreAttributes()) {
            String attrName  = consumeAttributeName();
            String attrValue = consumeAttributeValue();
            if (attrName.equals("name")) {
                info.setName(attrValue);
            }
            else if (attrName.equals("value")) {
                info.setValue(attrValue);
            }
            else if (attrName.equals("xmlns")) {
                if (!topmost) throw complaint("Can't use 'xmlns' on non-top-level element");
                if (!attrValue.equals(XMLGenerator.CSML_NAMESPACE)) throw complaint("Wrong default namespace: xmlns:"+attrValue);
                xmlnsSeen = true;
            }
            else if (attrName.equals("defaultLocale")) {
                if (!topmost) throw complaint("Can't use 'defaultLocale' on non-top-level element");
                if (!attrValue.equals(DataStore.getDatabaseLocaleString())) Log.logInfo("Warning: Parsing XML in " + sourceURL + " with defaultLocal '" + attrValue + "' different from system defaultLocal '" + DataStore.getDatabaseLocaleString() + "'");
                defaultLocale = attrValue;
            }
            else {  // it's a misc attribute name... validity will be caught later
                Data meta = makeParsedData("$"+attrName);
                meta.setValue(attrValue);
                info.addLocal(meta);
            }
        }
        if (topmost && !xmlnsSeen && !hasOption(OPTION_NO_NAMESPACE)) throw complaint("Default namespace not specified (no 'xmlns' found)");
    }

    private boolean hasMoreAttributes() throws XDException {
        skipWhitespace();
        return !(peekNext() == '/' || peekNext() == '>');
    }

    private String consumeAttributeName() throws XDException {
        mark();
        skipWhitespace();
        String name = consumeUntil(" \t=</>\r\n\""); // would normally be just " \t=", but this includes common mistake characters also
        if (name.length()==0) throw complaint("Missing attribute name or end of element");
        return name;
    }

    private String consumeAttributeValue() throws XDException {
        mark();
        skipWhitespace();
        expectNext('=');
        skipWhitespace();
        return unescape(consumeAnyQuoted());
    }

    private void expectElementStart() throws XDException {
        for (;;) {
            skipWhitespace();
            expectNext('<');
            if (peekNext() != '!') break;
            else skipRestOfComment();
        }
    }

    private String consumeElementStart() throws XDException {
        expectElementStart();
        return consumeUntil(" \t/>");
    }

    private void skipRestOfComment() throws XDException {
        expectNext('!'); expectNext('-'); expectNext('-');
        int state = 0;
        int startLine   = line;
        int startColumn = column;
        for (;;) {
            char c = consume();
            if (c == 0) throw complaint(Errors.VALUE_FORMAT,"Unterminated comment",startLine,startColumn);
            if (state == 0 && c == '-') state = 1;
            else if (state == 1 && c == '-') state = 2;
            else if (state == 2 && c == '>') break;
            else state = 0;
        }
    }

    private void expectElementEndOpen() throws XDException {
        skipWhitespace();
        expectNext('>');
    }

    private void expectElementEndClosed() throws XDException {
        skipWhitespace();
        expectNext('/');
        expectNext('>');
    }

    private void expectEntireEndTag(String tag) throws XDException {   // skips </Tag>
        skipWhitespace();
        expectNext('<');
        expectRestOfEndTag(tag);
    }
    private void expectRestOfEndTag(String tag) throws XDException {   // skips /Tag>
        expectNext('/');
        skipWhitespace();
        String found = consumeUntil(" \t>");
        if (!found.equals(tag)) throw complaint("Unmatched closing tag: expected \""+tag+"\" but found \""+found+"\"");
        skipWhitespace();
        expectNext('>');
    }

    private String consumeBodyText(String tag) throws XDException {
        String returnValue  = "";
        skipWhitespace();
        if (peekNext() == '/') {
           expectElementEndClosed();  // ended with "/>" so there are no bodyText to process, so we're done.
        }
        else {
            expectElementEndOpen();   // it ends with ">", so we have some body text to gather
            returnValue = consumeUntil("<");
            expectEntireEndTag(tag);
        }
        return unescape(returnValue);
    }

    private void consumeXMLHeader() throws XDException {
        skipWhitespace();
        expectNext('<'); expectNext('?');
        consumeUntil('?');
        expectNext('?'); expectNext('>');
    }

    private String unescape(String in) throws XDException {
        StringBuilder out = new StringBuilder();
        for(int i=0; i<in.length(); i++)  {
            char c = in.charAt(i);
            if (c == '&') {
                int end = in.indexOf(';',i);
                if (end == -1) throw complaint(Errors.PARAM_SYNTAX, "Invalid entity encoding (no ';' after '&')");
                String entity = in.substring(i+1,end);
                switch (entity) {
                    case "lt":   c = '<';    break;
                    case "gt":   c = '>';    break;
                    case "amp":  c = '&';    break;
                    case "apos": c = '\'';   break;
                    case "quot": c = '\"';   break;
                    default: throw complaint(Errors.PARAM_SYNTAX, "Invalid entity '&"+entity+";'");
                }
                i=end;
            }
            out.append(c);
        }
        return out.toString();
    }

}
