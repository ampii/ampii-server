// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.definitions;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.StringData;
import org.ampii.xd.data.Context;

import java.util.List;

/**
 * Makes instances of defined types.  These are made from prototypes, which are made from definitions.
 *
 * @author drobin
 */
public class Instances {

    public static Data makeInstance(Data parsed) throws XDException {
        Data results = Instances.makeInstance(parsed.stringValueOf(Meta.TYPE, null), parsed.getBase(), parsed.getName());
        results.put(parsed);
        return results;
    }

    /**
     * Both type and/or base can be null. If both are provided, type is used and base is ignored (no check is made for
     * consistency). If both are null, an exception is thrown.
     */
    public static Data makeInstance(String type, Base base, String name, Object... initializers) throws XDException {
        if (type != null) return makeInstance(type, name, initializers);
        if (base == null) throw new XDException(Errors.NOT_REPRESENTABLE,"Neither type nor base provided");
        return DataFactory.make(base, name, initializers);
    }

    public static Data makeInstance(String type, Object... initializers) throws XDException {
        return makeInstance(type,"..new",initializers);
    }

    public static Data makeInstance(String type, String name, Object... initializers) throws XDException {
        Data instance = makeInstance(Prototypes.getPrototypeFor(type), name, initializers);
        if (Base.fromString(type)==Base.INVALID) instance.addLocal(new StringData(Meta.TYPE, type)); // if not base type, remember it
        return instance;
    }


    public static Data makeInstance(Data prototype, String name, Object... initializers) throws XDException {
        if (!prototype.isPrototype()) throw new XDError("Given prototype is not really a prototype",prototype,name);
        Data instance = makeInstanceOf_recurse(prototype, name, new Context("makeInstance()"));
        for (Object initializer : initializers) {
            if (initializer instanceof Data) instance.addLocal((Data)initializer);
            else if (initializer instanceof List) for (Object sub : (List)initializer)instance.addLocal((Data)sub);
            else instance.setValue(initializer);
        }
        return instance;
    }

    ///////////////////////////////////

    private static Data makeInstanceOf_recurse(Data prototype, String name, Context context) throws XDException {
        if (++context.cur_depth > Application.maxDefinitionDepth) throw new XDException(Errors.INCONSISTENT_VALUES,"Circular definition encountered while instantiating "+prototype.getName());
        Data instance;
        if (prototype.isBuiltin()) {  // builtins with $type cause a problem when bootstrapping so we have to be carefull
            String type = prototype.stringValueOf(Meta.TYPE, null);
            if (type != null && Definitions.findDefinition(type) != null) {
                instance = Instances.makeInstanceOf_recurse(Prototypes.findPrototypeFor(type), name, context);
            }
            else { // oh, well, we tried. the definition is not defined yet at this point in the bootstrapping
                instance = DataFactory.make(prototype.getBase(),name);
                instance.setPrototype(prototype);
            }
        }
        else {
            instance = DataFactory.make(prototype.getBase(),name);
            instance.setPrototype(prototype);
        }
        // children and metadata have different behavior in instances.
        // The spec requires that: all children are return but only metadata this is different from its definition is returned.
        // Therefore, we simply mimic this requirement in the instance itself: children are copied from the prototype but metadata is not.
        for (Data childPrototype : prototype.getChildren()) {
            if (!childPrototype.isOptional()) {
                Data childInstance = makeInstanceOf_recurse(childPrototype, childPrototype.getName(), context);
                instance.addLocal(childInstance);
            }
        }
        if (prototype.canHaveValue() && prototype.hasValue()) instance.setValue(prototype.getValue());
        context.cur_depth--;
        return instance;
    }



}
