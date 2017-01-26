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
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * This is primarily for Data-to-JSON, but there is a little bonus utility to generate JSON from simple name-value pairs.
 *
 * @author drobin
 */
public class JSONGenerator extends Generator {

    List<String> definitionNames = new ArrayList<String>();
    String defaultLocale;

    public void generate(Writer writer, Data data) throws XDException {
        Context context = data.getContext();
        this.defaultLocale = context.getLocale();
        begin(writer, context);
        emitObject(data, false);
        finish();
    }

    ////////////////////////////////

    private void emitObject(Data data, boolean doingMetadata) throws XDException {
        String name = data.getName();
        context.cur_depth++;
        emitStartOfObject(context.cur_depth == 1 ? null : name);
        setNewline(true);

        DataList metadata = data.getContextualizedMetadata();
        DataList children = data.getContextualizedChildren();

        if (context.cur_depth==1) {
            if (!data.getName().startsWith("..")) emitQuotedPair("$name", data.getName()); // don't emit "internal" names at top level
            emitQuotedPair("$$defaultLocale", defaultLocale);
            if (context.hasRequest() && !data.getName().equals(".csml")) { // we don't do any of this for the special case of a top level ".csml"/<CSML>, wrapper
                if (context.canInclude(Meta.SELF))  emitQuotedPair("$self", Path.toPath(data));
                for (String metaName : Rules.inheritsFromParents.getComponents()) {
                    if (data.find(metaName) == null) { // if found locally, never mind - they will get emitted elsewhere in this method
                        if (context.canInclude(metaName) && Rules.parentallyInheritedIsDifferentFromDefault(data, metaName)) {
                            Data effective = data.findEffective(metaName);
                            if (effective != null)
                                emitQuotedPair(metaName, effective.stringValue()); // *should* be non-null if differentFromDefault returned true
                        }
                    }
                }
            }
        }

        if (context.canIncludeDefinitions()) emitDefinitionsFor(data);

        if (context.canIncludeBase() || data.isFromAny() || data.isFromNothing()) emitQuotedPair("$base", Base.toString(data.getBase()));

        if (data.canHaveChildren()) {
            if (children.truncated || !context.canDescend()) { emitQuotedPair("$truncated", "true");  }
            if (children.partial)                            { emitQuotedPair("$partial", "true");    }
            if (children.next != null)                       { emitQuotedPair("$next",children.next); }
        }

        if (context.canIncludeValue() && data.hasValue()) {
            if (!context.getAuthorizer().checkRead(data)) emitUnquotedPair("$error", Integer.toString(Errors.NOT_READABLE));
            else if (!metadata.contains(Meta.ERROR)) emitValue(data);
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
                    if (localType != null) emitQuotedPair("$type", localType.stringValue());
                }
                else { // else we have to output effective type as $type, and a local $type turns into $extends
                    emitQuotedPair("$type", effectiveType);
                    // if the local type is different from the effective type, then we output it as $extends
                    if (localType != null && !localType.stringValue().equals(effectiveType))  emitQuotedPair("$extends", localType.stringValue());
                }
            }
            else {  // we're not at the top level, so we don't worry about "effective type", just output $type normally, if not implied
                if (localType != null && (data.isFromAny() || data.isFromNothing())) emitQuotedPair("$type", localType.stringValue());
            }
        }

        for (Data meta : metadata) {
            String metaName = meta.getName();
            if (metaName.equals(Meta.TYPE)) continue; // $type is dealt with above
            // check special metadata name to serialize as a "$$definitions":{} section
            if (metaName.startsWith(Meta.AMPII_DEFINITIONS))     { emitDefinitions(meta.getChildren());continue; }
            if (metaName.startsWith(Meta.AMPII_TAG_DEFINITIONS)) { emitTagDefinitions(meta.getChildren());continue; }
            // if this metadata has metadata, or can have children itself, or it is extended metadata, then we have to use object form,
            // else we can use the primitive form
            boolean metaHasMetadata = context.makeContextualizedMetadata(meta).size() != 0;
            if (!metaHasMetadata && !meta.canHaveChildren() && !Rules.isExtendedMetadata(meta.getName())) emitSubValue(meta, context);
            else emitObject(meta,true);
        }

        if (context.canDescend() || doingMetadata) {
            for (Data child : children) {
                // if this child has metadata, or can have children itself, or we've been requested or required to include $base, then we have to use object form,
                // else we can use the primitive form (yay, brevity!)
                boolean childHasMetadata = context.makeContextualizedMetadata(child).size() != 0;
                if (!childHasMetadata && !child.canHaveChildren() && !context.canIncludeBase() && !child.isFromAny() && !child.isFromNothing()) emitSubValue(child, context);
                else emitObject(child,false);
            }
        }
        emitEndOfObject();
        context.cur_depth--;
    }

    private void emitValue(Data data) throws XDException {  // emitting the value of this item; similar but different from emitSubValue(), so can't be combined
        Base base = data.getBase();
        if (base == Base.UNSIGNED || base == Base.INTEGER  ||
                base == Base.REAL     || base == Base.DOUBLE   || base == Base.BOOLEAN ) {
            emitUnquotedPair("$value", data.stringValue());
        }
        else if (base == Base.DATE || base == Base.DATETIME || base == Base.TIME ) {
            if (!data.booleanValueOf(Meta.UNSPECIFIEDVALUE, false)) emitQuotedPair("$value", data.stringValue());
        }
        else {
            if (data.getValue() instanceof LocalizedStrings) {
                for (LocalizedString locstr : (LocalizedStrings)data.getValue()) {
                    if (locstr.isDefaultLocale()) emitQuotedPair("$value", locstr.getValue());
                    else emitQuotedPair("$value" + "$$" + locstr.getLocale(), locstr.getValue());
                }
            }
            else emitQuotedPair("$value", data.stringValue());
        }
    }

    private void emitSubValue(Data data, Context context) throws XDException {
        Base   base  = data.getBase();
        String name  = data.getName();
        Object value = data.getValue();
        if (!context.getAuthorizer().checkRead(data)) {
            emitUnquotedPair(name,"{\"$error\":"+Errors.NOT_READABLE+"}");  // can't show value, so flip to json object format to include $error
        }
        else if (value == null) {
            emitUnquotedPair(name, "{}"); // normally json would use "foo":null here but we don't support null values, so using the json object format works here
        }
        else if (base == Base.UNSIGNED || base == Base.INTEGER  ||
            base == Base.REAL     || base == Base.DOUBLE   || base == Base.BOOLEAN ) {
            emitUnquotedPair(name, data.stringValue());
        }
        else if (value instanceof LocalizedStrings) {  // localized values show up as multiple "primitives"
            for (LocalizedString ls : ((LocalizedStrings) value)) { // e.g., localized values like "displayName$$de-DE":"foo"
                emitQuotedPair(ls.isDefaultLocale() ? name : name + "$$" + ls.getLocale(), ls.getValue());
            }
        }
        else emitQuotedPair(name,data.stringValue()); // no localized values: e.g., output "displayName":"foo"
    }

    private void emitDefinitionsFor(Data data) throws XDException {
        Data definition = Definitions.getDefinitionContaining(data);
        if (definition != null ) {
            if (!definitionNames.contains(definition.getName())) { // omit if already serialized once
                definitionNames.add(definition.getName());
                context.enterDefinitionContext();
                emitStartOfObject("$$definitions");
                emitObject(definition,false);
                emitEndOfObject();
                context.exitDefinitionContext();
            }
        }
    }

    private void emitDefinitions(DataList defs)    throws XDException { _emitDefs("$$definitions", defs);    }

    private void emitTagDefinitions(DataList defs) throws XDException { _emitDefs("$$tagDefinitions", defs); }

    private void _emitDefs(String name, DataList defs) throws XDException {
        context.enterDefinitionContext();
        emitStartOfObject(name);
        for (Data def: defs) emitObject(def,false);
        emitEndOfObject();
        context.exitDefinitionContext();
    }

    private void emitStartOfObject(String name) {
        if (indentLevel !=0) checkFirstAndForceNewline();
        if (name == null) emitWithOptionalIndent("{", indentLevel != 0);
        else emitWithOptionalIndent("\"" + escape(name) + "\":{", indentLevel != 0);
        indentLevel++;
        setNewline(false);
        first = true;
    }

    private void emitEndOfObject() {
        boolean newline = checkNewline();
        indentLevel--;
        if (newline) { emit("\n"); emitWithIndent("}"); }
        else emit("}");
        first = false;
    }
    private void emitUnquotedPair(String name, String value) {
        checkFirst();
        emitWithOptionalIndent("\"" + escape(name) + "\":" + value, checkNewline());
    }

    private void emitQuotedPair(String name, String value) {
        checkFirst();
        emitWithOptionalIndent("\"" + escape(name) + "\":\"" + escape(value) + "\"", checkNewline());
    }

    private void checkFirst() {
        if (!first) emit(",");
        if (checkNewline()) emit("\n");
        else if (!first) emit(" ");
        first = false;
    }
    private void checkFirstAndForceNewline() {
        if (!first) emit(",");
        emit("\n");
        first = false;
    }

    private static String escape(String in) {
        StringBuilder out = new StringBuilder(in.length());
        for (char c : in.toCharArray()) {
            if (c < 0x10 ) out.append("\\u000").append(Integer.toHexString(c));
            else if (c < 0x20 ) out.append("\\u00").append(Integer.toHexString(c));
            else if (c == '\\' || c == '"') out.append('\\').append(c);
            else out.append(c);
        }
        return out.toString();
    }

    /////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////

    // *alternate* little utility method thrown in here, unrelated to Data items, but, hey, it's a "JSON generator" too
    public static String generatePairs(Object... pairs) {
        // generate simple JSON given pairs of objects: name, value, ...repeat
        // only handles primitive strings and numbers at this time
        StringBuilder json = new StringBuilder();
        json.append("{");
        for (int i = 0; i < pairs.length; i+=2) {
            Object name = pairs[i];
            Object value = pairs[i+1];
            json.append("\"" + escape(name.toString()) + "\":"); // "name":
            if (value instanceof String) json.append("\"" + escape((String) value) + "\""); //"name":"value"
            else if (value == null) json.append("null");                                    //"name":null
            else json.append(value.toString());                                             //"name":value
            if (i+2 < pairs.length) json.append(",");
        }
        json.append("}");
        return json.toString();
    }


}
