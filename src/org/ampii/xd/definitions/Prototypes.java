// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.definitions;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.CollectionData;
import org.ampii.xd.data.basetypes.ListData;
import org.ampii.xd.database.DataStore;
import org.ampii.xd.resolver.Path;

/**
 * Makes prototypes of defined types, from which instances are made.
 * <p>
 * A prototype is a kind of "rolled up" definition.
 * <p>
 * This is a <b>very useful</b> layer to have because the inheritance chain for things like $namedValues can get pretty
 * harry otherwise. Several coding attempts were made for "life without prototypes" and all failed, buried in complexity.
 *
 * @author drobin
 */
public class Prototypes {

    public static Data    getSystemPrototypes()  {
        try { return DataStore.getSystemRootIHopeYouKnowWhatYouAreDoing().getOrCreate("..prototypes",null,Base.COLLECTION); }
        catch (XDException e) { throw new XDError("getSystemPrototypes() failed!"); }
    }

    public static Data getPrototypeFor(Base base) {
        return Builtins.getPrototypeOfBase(base); // builtins are also valid prototypes
    }

    public static String getPathFor(Data proto) {
        return Path.toRelativePath(getSystemPrototypes(), proto);
    }

    public static Data getPrototypeFor(String typeName) throws XDException {
        return getPrototypeFor_recurse(typeName, new Depth());
    }
    public static Data findPrototypeFor(String typeName)  {
        try { return getPrototypeFor_recurse(typeName, new Depth());}
        catch (XDException e) { return null; }
    }

    public static void removePrototype(String typeName) {
        getSystemPrototypes().removeLocal(typeName);
    }

    ///////////////////////////////////////////////////////

    private static class Depth { public int depth = 0; }

    private static Data getPrototypeFor_recurse(String type, Depth depth) throws XDException {
        if (++depth.depth > Application.maxDefinitionDepth) throw new XDException(Errors.INCONSISTENT_VALUES,"Circular definition encountered for "+type);
        // the 'type' argument is allowed to be in the form "toplevel/sublevel/..."
        // if that is the case, then we strip off the top level name and find that by itself, then we descend down
        // through that prototype following the path we are given to find the corresponding descendant
        if (type.contains("/")) {
            Data found = null;
            for (String segment : type.split("/")) {
                if (found == null) found = getPrototypeFor_recurse(segment, depth); // found is null for top level, so that's where we start
                else { // for all subsequent levels, descend through the prototype
                    Data sub = found.find(segment);
                    if (sub == null) {
                        // since prototypes don't copy all of *their* prototype's metadata, we have to look up the prototype chain for the metadata.
                        for (Data proto = found.getPrototype(); !proto.isBuiltin(); proto = proto.getPrototype()) {
                            sub = proto.find(segment);
                            if (sub != null) break;
                        }
                    }
                    if (sub != null) found = sub;
                }
                // if that was unsuccessful, complain
                if (found == null) throw new XDException(Errors.INVALID_DATATYPE,"When resolving type name '"+type+"', '"+segment+"' was not found");
            }
            depth.depth--;
            return found;
        }
        else {  // we were not given a slash in the name, so this must be looking for a top level name.
            // if there already is one, return that
            Data proto = getSystemPrototypes().find(type);
            if (proto != null) { depth.depth--; return proto; }
            // if it's a builtin, then use that
            proto = Builtins.findPrototypeOfBase(type);
            if (proto != null) { depth.depth--; return proto; }
            // else look up the definition by name, and complain if missing
            Data definition = Definitions.getDefinitions().find(type);
            if (definition == null)
                throw new XDException(Errors.INCONSISTENT_VALUES, "Missing definition for '"+type+"'");
            // found the definition, so make a prototype for it
            proto = makePrototypeFromDefinition(definition, depth);
            getSystemPrototypes().addLocal(proto);
            proto.setIsImmutable(true);
            proto.setIsPrototype(true);
            depth.depth--;
            return proto;
        }
    }

    private static Data makePrototypeFromDefinition(Data definition, Depth depth) throws XDException
    {
        if (++depth.depth > Application.maxDefinitionDepth) throw new XDException(Errors.INCONSISTENT_VALUES,definition,"Circular definition encountered");
        String defName = definition.getName();
        Data result;
        Data                     extensionOf = definition.find(Meta.EXTENDS);
        if (extensionOf == null) extensionOf = definition.find(Meta.TYPE);
        if (extensionOf == null) extensionOf = definition.find(Meta.OVERLAYS);
        if (extensionOf != null ) { // we are extending something,
            // so we first create an instance of the thing we're extending to be the starting point for our prototype
            // DO NOT refactor the following two lines into Instances.makeInstance(extensionOf.stringValue(),defName);
            // we're in a recursion and we need to properly check 'depth' to prevent runaway circular definitions.
            Data prototypePrototype = getPrototypeFor_recurse(extensionOf.stringValue(), depth); // DO NOT refactor (see above);
            result = Instances.makeInstance(prototypePrototype,defName);                         // DO NOT refactor (see above);
        }
        else { // we are not extending anything, so start with a fresh empty base type
            result = DataFactory.make(definition.getBase(), defName);
        }
        result.setIsPrototype(true);
        // now here's the tricky bit, we update the members of the new "instance" with our (the given definition's) stuff.
        // This will make a merged prototype for the given definition.
        // The "instance" we made above has a copy of all the children and none of the metadata of the extended definition.
        // So we will look at all the given metadata and make each one a local metadata in our new prototype, merging in the
        // extended metadata using special rules as needed.
        for (Data givenMetaDef : definition.getMetadata()) {
            String metaName = givenMetaDef.getName();
            switch (metaName) {
                case Meta.TYPE:
                case Meta.EXTENDS:
                case Meta.OVERLAYS:
                    // $type, $extends, and $overlays are only used to construct this prototype. they are not stored here.
                    // An instance made from this will have its 'prototype' member variable pointing to this, so if it
                    // wants to know its "effective type" it can just use that pointer to find this prototype and follow
                    // up the parents to get its effect type name.
                    // Any's can have their own local $type metadata so they can have *two* types. One from this prototype
                    // via the 'prototype' member variable and the other from the type declared by their local $type metadata
                    break;
                case Meta.MEMBERTYPEDEFINITION:
                    // Inline definitions can use $extends, so we have to make a real flattened prototype out of them also.
                    Data memberDef = givenMetaDef.get("1"); // $memberTypeDefinition is a List, so the actual definition is a child named "1"
                    Data memberProto = makePrototypeFromDefinition(memberDef, depth);
                    Data wrapper = new ListData(Meta.MEMBERTYPEDEFINITION); // recreate the $memberTypeDefinition wrapper for the new memberProto
                    wrapper.addLocal(memberProto);    // the prototype is also named "1", at ".../$memberTypeDefinition/1" because that's what the spec requires for the effective type
                    result.addLocal(wrapper);
                    break;
                case Meta.NAMEDBITS:
                case Meta.NAMEDVALUES:
                    // if this prototype was made *from* another (it extended something), then we need to start with the
                    // named bits/values from *that* thing and merge in what we've been given here to make a flat list of
                    // named values/bits.
                    // (we combine these two cases into one set of code here because there is so much similarity)
                    boolean doingBits = metaName.equals(Meta.NAMEDBITS); // shortcut for later use
                    Data extended =  result.findEffective(metaName);     // findEffective will get from prototype if we extended something
                    Data combined = new CollectionData(metaName);        // this is the new $namedXxxx
                    int highest = -1;
                    // this loop does three things: copies extended children into results, looks for highest assigned value, and assigns unassigned value
                    if (extended != null) for (Data extendedThing : extended.getChildren()) {
                        Data newThing = Instances.makeInstance(extendedThing); // make copy of the extended thing for our new prototype
                        // if value is unassigned, assign next available number
                        if (doingBits) if (newThing.effectiveIntValueOf(Meta.BIT, -1) == -1) newThing.set(Meta.BIT,++highest);
                        else           if (!newThing.hasValue())                             newThing.setValue(++highest);
                        // now look for highest assigned value
                        int value = doingBits? newThing.effectiveIntValueOf(Meta.BIT, -1) : newThing.intValue();
                        if (value > highest) highest = value;
                        combined.addLocal(newThing);
                    }
                    // now that we've copied all extended named things, let's add (or overlay) the new ones
                    for (Data givenThingDef : givenMetaDef.getChildren()) {
                        // first check if the given name already existed in the extended definition? (we just made a copy of it above)
                        Data existingThing = combined.find(givenThingDef.getName());
                        if (existingThing != null) {
                            // yes it already existed, so we're overlaying new information
                            // sanity check: you can't change the *number* of the thing your are overlaying
                            int givenValue    = doingBits? givenThingDef.effectiveIntValueOf(Meta.BIT, -1)    : givenThingDef.intValue();
                            int existingValue = doingBits? existingThing.effectiveIntValueOf(Meta.BIT, -1) : existingThing.intValue();
                            if (givenValue != existingValue) throw new XDException(Errors.INCONSISTENT_VALUES,givenMetaDef,"Can't change value of extended item");
                            existingThing.put(givenThingDef); // put() will merge new overlaid metadata from given to existing
                        }
                        else {
                            Data newThing = makePrototypeFromDefinition(givenThingDef, depth); // make copy of the given thing for our new prototype
                            // if value is unassigned, assign next available number
                            if (doingBits) if (newThing.effectiveIntValueOf(Meta.BIT, -1) == -1) newThing.set(Meta.BIT,++highest);
                            else           if (!newThing.hasValue())                      newThing.setValue(++highest);
                            combined.addLocal(newThing);
                        }
                    }
                    result.addLocal(combined);  // finally, add the newly created $namedXxxx to our result
                    break;
                case Meta.CHOICES:
                    Data resultMeta = result.findEffective(Meta.CHOICES); // we use findEffective() here to get inherited $choices
                    if (resultMeta != null) {         // if found, update it
                        // this is considerably easier than the $namedXxxx stuff above.  We need to just add new or update existing children
                        // of $choices. However, if we just call put() here, it will *replace* all the members of $choices because that's the
                        // default behavior for a Collection.  So we add an option to put to modify that behavior to do a merge.
                        resultMeta.put(givenMetaDef,Data.PUT_OPTION_GIVEN_PARTIAL);
                    }
                    else {  // else make one
                        resultMeta = makePrototypeFromDefinition(givenMetaDef, depth);
                        result.addLocal(resultMeta);
                    }
                    break;
                default:
                    // end of special cases, just update or add new metadata
                    resultMeta = result.find(givenMetaDef.getName());
                    if (resultMeta != null) {      // if found, update it
                        resultMeta.put(givenMetaDef);
                    }
                    else {  // else make one
                        resultMeta = makePrototypeFromDefinition(givenMetaDef, depth);
                        result.addLocal(resultMeta);
                    }
                    break;
            }
        }
        // the rules for children in definitions are based on base type and are slightly different from a simple put()
        switch (definition.getBase()) {
            case LIST:
            case SEQUENCEOF:
            case ARRAY:
            case COLLECTION:
            case UNKNOWN:
                result.removeLocalChildren(); // goodbye extended children! you're being replaced.
                boolean givenChildren = false;
                for (Data givenChildDef : definition.getChildren()) {
                    result.addLocal(makePrototypeFromDefinition(givenChildDef, depth));
                    givenChildren = true;
                }
                if (givenChildren) result.removeLocal(Meta.OPTIONAL); // if there are children in this definition, then this is no longer be optional
                // TODO: confirm that an extended optional item is no longer optional if it's been given defined children
                break;
            case OBJECT:
            case SEQUENCE:
            case COMPOSITION:
            case CHOICE:
                for (Data givenChildDef : definition.getChildren()) {  // just add to end of existing (assumes $partial in definitions)
                    result.addLocal(makePrototypeFromDefinition(givenChildDef, depth));
                }
                break;
            // we don't have a case for "POLY" here because that's not ever really in definitions
        }
        // finally, deal with value
        if (definition.canHaveValue() && definition.hasValue()) {
            result.setValue(definition.getValue());
            result.removeLocal(Meta.OPTIONAL); // if there was a value defined here, then this is no longer optional
        }
        depth.depth--;
        return result;
    }


}
