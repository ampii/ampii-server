// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.marshallers;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.LocalizedString;
import org.ampii.xd.common.LocalizedStrings;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.definitions.Definitions;
import org.ampii.xd.data.Context;
import org.ampii.xd.resolver.Path;
import javax.xml.bind.DatatypeConverter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates XML from {@link Data} items.
 * <p>
 * It's a lot yuckier than the JSON generator because of the need to handle ugly things like {@code<Extensions>} and things
 * that have two forms like {@code displayName/<DisplayName>} and {@code value/<Value>}.
 *
 * @author drobin
 */
public class XMLGenerator extends Generator {

    static String CSML_NAMESPACE = "http://bacnet.org/csml/1.2";
    List<String> definitionNames = new ArrayList<String>();
    String defaultLocale;

    public void generate(Writer writer, Data data) throws XDException {
        Context context = data.getContext();
        this.defaultLocale = context.getLocale();
        begin(writer, context);
        emit("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        emitData(data, data.getName(), false, false);
        finish();
    }

    //////////////////////////////////////////////

    private void emitData(Data data, String name, boolean doingMetadata, boolean doingExtensions) throws XDException {
        String tag = (context.cur_depth==0 && data.getName().startsWith(".csml"))? "CSML" : Base.toString(data.getBase());  // data named ".csml" turns into <CSML>, everything else is based on base type
        emitStartOfElement(tag);
        context.cur_depth++;
        // <CSML...> element doesn't have a name and neither should "internal" names at top level, otherwise, emit name
        if (!(tag.equals("CSML") || (context.cur_depth==1 && data.getName().startsWith("..")))) emitXMLAttribute("name", name);
        if (data.canHaveValue() && context.canIncludeValue() && !doingExtensions) {
            if (!context.getAuthorizer().checkRead(data)) {
                emitXMLAttribute("error", Integer.toString(Errors.NOT_READABLE));
            }
            else if (!hasLargeValue(data) && !data.getContextualizedMetadata().contains(Meta.ERROR)) {
                if (data.getValue() instanceof LocalizedStrings) {
                    emitXMLAttribute("value",((LocalizedStrings)data.getValue()).get(context.getLocale()));
                }
                else if (data.hasValue()) {
                    Base base = data.getBase();
                    if (base == Base.DATE || base == Base.DATETIME || base == Base.TIME) {
                        if (!data.booleanValueOf(Meta.UNSPECIFIEDVALUE, false))
                            emitXMLAttribute("value", data.stringValue());
                    }
                    else emitXMLAttribute("value", data.stringValue());
                }
            }
        }
        // We have to decide when to output $type (and possibly $extends based on whether an Any is from a defined Any
        // member of a Sequence/Object/Composition).  So, getEffectiveType() is the magic sauce here because it will
        // return blank for children of collections that don't have a $memberType or $memberTypeDefinition (truly
        // "no definition" items), and non-blank for an Any child of a Sequence/Object/Composition definition
        // (i.e., a "defined Any"), which is the special case where we output its $type as "$extends" at the top level
        // because we also have to output the 'effective type' as "$type"
        // We always output type info, at any level, if we're 'replaceable', i.e., 'from Any' or 'from nothing', i.e. the
        // client *can't* know our type. Even if the client has read us before and thinks it doesn't want type info, our
        // type could have changed, so we always have to return it!
        // If the client *does* want type info, we will always include it at the top level. But at lower levels, we will
        // only include it where it's not already implied by something above (e.g., we're replaceable)
        // Finally, we will always output type info, at all levels, if we're a definition.
        if (data.isFromAny() || data.isFromNothing() || context.canIncludeType() || data.isDefinition()) {
            Data localType = data.find(Meta.TYPE);
            if (context.cur_depth==1) { // at the top level, we have to output "effective type" as $type
                String effectiveType = data.getEffectiveType();
                if (effectiveType.isEmpty() || data.isDefinition()) { // if there is no effective type, or we're a definition, then we can just output $type as is.
                    if (localType != null) emitXMLAttribute("type", localType.stringValue());
                }
                else { // else we have to output effective type as $type, and a local $type turns into $extends
                    emitXMLAttribute("type", effectiveType);
                    // if the local type is different from the effective type, then we output it as $extends
                    if (localType != null && !localType.stringValue().equals(effectiveType))  emitXMLAttribute("extends",localType.stringValue());
                }
            }
            else {  // we're not at the top level, so we don't worry about "effective type", just output $type normally if it is not implied
                if (localType != null && (data.isFromAny() || data.isFromNothing()) ) emitXMLAttribute("type", localType.stringValue());
            }
        }
        emitRestOfData(tag, data, doingMetadata, doingExtensions);
        context.cur_depth--;
    }

    // come here after opening tag, and 'name' and 'value' if appropriate, have already been written
    private void emitRestOfData(String tag, Data data, boolean doingMetadata, boolean doingExtensions) throws XDException {

        DataList metadata = data.getContextualizedMetadata();
        DataList children = data.getContextualizedChildren();

        boolean hasXMLChildren = children.size()!=0 || hasLargeValue(data) || data.getValue() instanceof LocalizedStrings || context.canIncludeDefinitions(); // also set later by other factors

        for (Data meta : metadata) {
            if (meta.getName().equals(Meta.TYPE)) continue; // we've already dealt with type above.
            DataList metaMetadata = context.makeContextualizedMetadata(meta);
            if (!Rules.isStandardMetadata(meta.getName()) || metaMetadata.size()!=0 || meta.getValue() instanceof LocalizedStrings || metadataAlwaysUsesElementForm(meta.getName())) {
                hasXMLChildren = true; // no write yet; just set flag to come back and get this later
            }
            if (Rules.isStandardMetadata(meta.getName()) && meta.canHaveValue() && !metadataAlwaysUsesElementForm(meta.getName())) {
                emitXMLAttribute(meta.getName().substring(1), meta.stringValue()); // nothing fancy, then, just a plain little XML attribute value
            }
        }

        if (data.canHaveChildren()) {
            if (children.truncated || !context.canDescend()) { emitXMLAttribute("truncated", "true");  }
            if (children.partial)                            { emitXMLAttribute("partial", "true");    }
            if (children.next != null)                       { emitXMLAttribute("next", children.next); }
        }

        if (indentLevel == 1) { // emit these things only at the top level
            emitXMLAttribute("xmlns", CSML_NAMESPACE);
            emitXMLAttribute("defaultLocale", defaultLocale);
            if (context.hasRequest() && !data.getName().equals(".csml")) { // we don't do any of this for the special case of a top level ".csml"/<CSML>, wrapper
                if (context.canInclude(Meta.SELF)) emitXMLAttribute("self", Path.toPath(data));
                for (String metaName : Rules.inheritsFromParents.getComponents()) {
                    if (data.find(metaName) == null) { // if found locally, never mind - they will get emitted elsewhere in this method
                        if (context.canInclude(metaName) && Rules.parentallyInheritedIsDifferentFromDefault(data, metaName)) {
                            Data effective = data.findEffective(metaName);
                            if (effective != null)
                                emitXMLAttribute(metaName.substring(1), effective.stringValue()); // *should* be non-null if differentFromDefault returned true
                        }
                    }
                }
            }
        }
        if (!hasXMLChildren)  {   // if it doesn't have XML children, all we have to do is close the element
            emitEndOfElementClosed();
        }
        else {  // it does have XML children of some kind
            emitEndOfElementOpen();

            if (context.canIncludeDefinitions()) {
                emitDefinitionsFor(data);
            }
            if (hasLargeValue(data) && context.getAuthorizer().checkRead(data) && !metadata.contains(Meta.ERROR)) { // "large" values use the <Value> form - see StringData and OctetStringData
                emitStartOfElement("Value");
                emitEndOfElementOpenForBodyText();
                emit(escape(getLargeValueToString(data)));
                emitEntireEndTagNoIndent("Value");
            }
            if (data.getValue() instanceof LocalizedStrings && context.getAuthorizer().checkRead(data) && !metadata.contains(Meta.ERROR)) { // "localized" values also use the <Value> form
                // if we were given a specific locale, then we only output the one string (handled as attribute earlier), else we output them all here
                if (!context.hasLocale()) { // we were not given a specific locale, so...
                    for (LocalizedString locstr : (LocalizedStrings)data.getValue()) {   // output all but the system default locale
                        if (!locstr.isDefaultLocale()) { // default locale is handled earlier as attribute
                            emitStartOfElement("Value");
                            emitXMLAttribute("locale", locstr.getLocale());
                            emitEndOfElementOpenForBodyText();
                            emit(locstr.getValue());
                            emitEntireEndTagNoIndent("Value");
                        }
                    }
                }
            }
            boolean extensionNeeded = false;  // if this flag gets marked true on our first pass through the metadata, then we need <Extensions>
            for (Data meta : metadata) {
                // check special metadata name to serialize as a <Definitions> or <TagDefinitions> section
                String metaName = meta.getName();
                if      (metaName.startsWith(Meta.AMPII_DEFINITIONS))     { emitDefinitions(meta.getChildren());   continue; }
                else if (metaName.startsWith(Meta.AMPII_TAG_DEFINITIONS)) { emitTagDefinitions(meta.getChildren());continue; }
                if (Rules.isExtendedMetadata(metaName)) { extensionNeeded = true;  continue;  } // we'll serialize proprietary metadata later
                if (context.makeContextualizedMetadata(meta).size() != 0) extensionNeeded = true; // if the metadata has metadata set flag and deal with those later
                String attTag;
                if ((attTag = elementTagForConstructedMetadata(metaName)) != null) { // is this a constructed thing like <NamedValues>?
                    emitEntireStartTag(attTag);
                    for (Data child : meta.getChildren()) {
                        emitData(child, child.getName(), doingMetadata, false);
                    }
                    emitEntireEndTag(attTag);
                } else if ((attTag = elementTagForLocalizableMetadata(metaName)) != null) { // is it a localizable string like <Description>?
                    // some things like <Documentation> and <Units> always use element form even for the default locale
                    if (metadataAlwaysUsesElementForm(metaName)) {
                        emitStartOfElement(attTag);
                        emitEndOfElementOpenForBodyText();
                        emit(meta.stringValue());
                        emitEntireEndTagNoIndent(attTag);
                    }
                    // others, like <DisplayName> only use element form for non-default 'locale'
                    if (meta.getValue() instanceof LocalizedStrings) {
                        for (LocalizedString locstr : (LocalizedStrings) meta.getValue()) {   // output all but the system default locale
                            if (!locstr.isDefaultLocale()) { // default locale is handled earlier as attribute
                                emitStartOfElement(attTag);
                                emitXMLAttribute("locale", locstr.getLocale());
                                emitEndOfElementOpenForBodyText();
                                emit(locstr.getValue());
                                emitEntireEndTagNoIndent(attTag);
                            }
                        }
                    }
                }
            }
            if (extensionNeeded) {
                emitStartOfElement("Extensions");
                emitEndOfElementOpen();
                for (Data meta : metadata) {
                    if (Rules.isStandardMetadata(meta.getName())) {
                        if (context.makeContextualizedMetadata(meta).size()!=0) {
                            String metaName = meta.getName().substring(1); // strip off the leading "$"
                            emitData(meta, metaName, true, true);
                        }
                    }
                    else {
                        emitData(meta, meta.getName().substring(1), true, false);
                    }
                }
                emitEntireEndTag("Extensions");
            }
            // now finally, output the data's children (if we're not doing children of <Extensions>)
            if (children.size()!=0 && (doingMetadata || context.inDefinitionContext() || context.canDescend()) && !doingExtensions)  {  // (metadata are unaffected by depth)
                for (Data child : children)
                    emitData(child, child.getName(), doingMetadata, false);
            }
            emitEntireEndTag(tag);
        }
    }

    private void emitDefinitionsFor(Data data) throws XDException {
        Data definition = Definitions.getDefinitionContaining(data);
        if (definition != null ) {
            if (!definitionNames.contains(definition.getName())) { // omit if already serialized once
                definitionNames.add(definition.getName());
                context.enterDefinitionContext();
                emitStartOfElement("Definitions");
                emitEndOfElementOpen();
                context.enterDefinitionContext();
                emitData(definition, definition.getName(), false, false);
                context.exitDefinitionContext();
                emitEntireEndTag("Definitions");
                context.exitDefinitionContext();
            }
        }
    }

    private void emitDefinitions(DataList defs)    throws XDException { _emitDefs("Definitions", defs);    }

    private void emitTagDefinitions(DataList defs) throws XDException { _emitDefs("TagDefinitions", defs); }

    private void _emitDefs(String tagName, DataList defs) throws XDException {
        context.enterDefinitionContext();
        emitStartOfElement(tagName);
        emitEndOfElementOpen();
        context.enterDefinitionContext();
        for (Data def: defs) emitData(def, def.getName(), false, false);
        context.exitDefinitionContext();
        emitEntireEndTag(tagName);
        context.exitDefinitionContext();
    }

    /////////////////// "large" value support ///////////////////////

    private boolean hasLargeValue(Data data) throws XDException {
        if (!(data.getBase()==Base.STRING || data.getBase()==Base.BITSTRING || data.getBase()==Base.OCTETSTRING || data.getBase()==Base.RAW)) return false;
        Object value = data.getValue();
        return value instanceof String && ((String)value).length() > 32 ||
               value instanceof byte[] && ((byte[])value).length   > 32 ;
    }

    public String   getLargeValueToString(Data data)  throws XDException {
        Object value = data.getValue();
        if (value instanceof byte[]) return DatatypeConverter.printBase64Binary((byte[])value);
        return value == null? "" : data.stringValue();
    }

    ////////////////////////////////////////


    private void emitStartOfElement(String tag) {
        emitWithIndent("<" + tag);
        indentLevel++;
    }

    private void emitEntireStartTag(String tag) {
        emitWithIndent("<" + tag + ">\n");
        indentLevel++;
    }

    private void emitEntireEndTag(String tag) {
        indentLevel--;
        emitWithIndent("</" + tag + ">\n");
    }
    private void emitEntireEndTagNoIndent(String tag) {
        indentLevel--;
        emit("</" + tag + ">\n");
    }

    private void emitEndOfElementClosed() {
        emit("/>\n");
        indentLevel--;
    }
    private void emitEndOfElementOpen() {
        emit(">\n");
    }
    private void emitEndOfElementOpenForBodyText() {
        emit(">");
    }

    private void emitXMLAttribute(String name, String value) {
        emit(" " + name + "=\"" + escape(value) + "\"");
    }

    public static String elementTagForConstructedMetadata(String name)
    {
        if      (name.equals(Meta.NAMEDVALUES))           return "NamedValues";
        else if (name.equals(Meta.CHOICES))               return "Choices";
        else if (name.equals(Meta.MEMBERTYPEDEFINITION))  return "MemberTypeDefinition";
        else if (name.equals(Meta.LINKS))                 return "Links";
        else if (name.equals(Meta.NAMEDBITS))             return "NamedBits";
        else if (name.equals(Meta.PRIORITYARRAY))         return "PriorityArray";
        else if (name.equals(Meta.FAILURES))              return "Failures";
        return null;
    }

    public static String elementTagForLocalizableMetadata(String name)
    {
        if      (name.equals(Meta.ERRORTEXT))             return "Error";
        else if (name.equals(Meta.UNITSTEXT))             return "Units";
        else if (name.equals(Meta.DOCUMENTATION))         return "Documentation";
        else if (name.equals(Meta.DESCRIPTION))           return "Description";
        else if (name.equals(Meta.DISPLAYNAME))           return "DisplayName";
        else if (name.equals(Meta.DISPLAYNAMEFORWRITING)) return "DisplayNameForWriting";
        else if (name.equals(Meta.WRITABLEWHENTEXT))      return "WritableWhen";
        else if (name.equals(Meta.REQUIREDWHENTEXT))      return "RequiredWhen";
        else if (name.equals(Meta.COMMENT))               return "Comment";
        return null;
    }
    public static boolean metadataAlwaysUsesElementForm(String name)
    {
        return  name.equals(Meta.ERRORTEXT)            ||
                name.equals(Meta.UNITSTEXT)            ||
                name.equals(Meta.DOCUMENTATION)        ||
                name.equals(Meta.NAMEDVALUES)          ||
                name.equals(Meta.CHOICES)              ||
                name.equals(Meta.MEMBERTYPEDEFINITION) ||
                name.equals(Meta.WRITABLEWHENTEXT)     ||
                name.equals(Meta.REQUIREDWHENTEXT)     ||
                name.equals(Meta.LINKS)                ||
                name.equals(Meta.NAMEDBITS)            ||
                name.equals(Meta.PRIORITYARRAY)        ||
                name.equals(Meta.FAILURES);
    }

    private String escape(String in) {
        StringBuilder out = new StringBuilder(in.length());
        for (char c : in.toCharArray()) {
            if      (c == '\"' ) out.append("&quot;");
            else if (c == '\'' ) out.append("&apos;");
            else if (c == '<' )  out.append("&lt;");
            else if (c == '>' )  out.append("&gt;");
            else if (c == '&' )  out.append("&amp;");
            else out.append(c);
        }
        return out.toString();
    }

}