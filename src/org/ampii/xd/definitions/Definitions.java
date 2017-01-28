// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.definitions;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.database.DataStore;

/**
 * This is the manager of the system-wide definitions, from which prototypes, and thus instances, are made.
 * <p>
 * This DOES NOT use sessions.  For a variety of chicken-and-egg reasons, Definitions and Prototypes are created
 * persistently on the fly. A definition will not be accepted if it is structurally deficient, but other than that
 * it is committed immediately and an instance can be made from it.
 * TODO evaluate use cases for where we might want to make this a little more flexible
 *
 * @author daverobin
 */
public class Definitions {

    public static DefinitionCollector getSystemDefinitionCollector() { return systemDefinitionCollector; }

    private static DefinitionCollector systemDefinitionCollector = new DefinitionCollector() {
        @Override public void addDefinition(Data definition)    throws XDException { Definitions.addDefinition(definition); }
        @Override public void addTagDefinition(Data definition) throws XDException { Definitions.addTagDefinition(definition); }
    };

    public static Data    getDefinitions()  {
        try { return DataStore.getSystemRootIHopeYouKnowWhatYouAreDoing().getOrCreate(".defs",null,Base.COLLECTION); }
        catch (XDException e) { throw new XDError("getDefinitions() failed!"); }
    }
    public static Data    getTagDefinitions()  {
        try { return DataStore.getSystemRootIHopeYouKnowWhatYouAreDoing().getOrCreate("..tags",null,Base.COLLECTION); }
        catch (XDException e) { throw new XDError("getTagDefinitions() failed!"); }
    }

    public static Data    findDefinition(String typeName)   throws XDException { return getDefinitions().find(typeName); }
    public static Data    findTagDefinition(String tagName) throws XDException { return getTagDefinitions().find(tagName); }

    public static void    addDefinition(Data given) throws XDException    { _addToDefCollection(getDefinitions(), given);   }
    public static void    addTagDefinition(Data given) throws XDException { _addToDefCollection(getTagDefinitions(), given);}

    public static void     removeDefinition(String name) throws XDException {  // also removes corresponding prototype
        getDefinitions().removeLocal(name);
        Prototypes.removePrototype(name);
    }
    public static void     removeTagDefinition(String name) throws XDException {
        getTagDefinitions().removeLocal(name);
    }

    public static Data getDefinitionContaining(Data instance) throws XDException { // will return null for builtins
        // called by serializers to include definitions when needed.
        // e.g., included in $$definitions and <Definitions> when metadata=defs
        return findDefinition(instance.getEffectiveType().split("/")[0]); // we'll let the other function do the heavy here
    }

    ///

    private static void   _addToDefCollection(Data collection, Data given) throws XDException {
        if (collection.find(given.getName())!=null) throw new XDException(Errors.CANNOT_CREATE, collection, given, "cannot add duplicate definition for '" + given.getName() + "'");
        Data newdef = collection.createChild(given.getName(), null, given.getBase());
        collection.removeLocal(newdef); // separate from immutable parent temporarily so we can put to it
        newdef.setParent(null);
        newdef.setIsDefinition(true); // set this FIRST since the behavior of put() is different for definitions
        newdef.put(given, Data.PUT_OPTION_FORCE_WRITE);  // we use a put() here to clean up any freshly parsed data that might still contain POLYs at the lower levels
        newdef.setIsImmutable(true);  // make sure it can no longer be changed by any means, even internally
        collection.addLocal(newdef); // give it back to the immutable parent now that we're done modifying it
    }



}