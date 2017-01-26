// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.resolver;

import org.ampii.xd.application.Application;
import org.ampii.xd.application.hooks.AliasHooks;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.LinkData;

/**
 * This handles URI aliases, like ".blt", "$target", etc.
 * <p>
 * Aliases are things that do not exist as data items at the location named in the URI. They are internal "redirections".
 * <p>
 * If you want to add an alias without changing this core AMPII file, make an {@link AliasHooks.External} and inject it with
 * {@link AliasHooks#registerExternal}.
 *
 * @author drobin
 */
public class Alias {

    public static Data resolve(Data data, String name) throws XDException {
        Data hooked = AliasHooks.resolve(data, name);
        if (hooked != null) return hooked;

        if (name.equals(Meta.TARGET)) {
            if (data instanceof LinkData) {
                Data target = ((LinkData)data).findTarget();
                if (target == null) throw new XDException(Errors.BAD_LINK_TARGET, data, "The 'value' of the Link data is not valid or not supported for evaluation as a $target");
                return target;
            }
            // TODO handle other kinds of references, like BACnetDeviceObjectReference. Do we need a new kind of hook?
            return data; // spec says that $target evaluates to self if data is not a kind of reference
        }
        else if (name.equals(".this")) {
            Data thisDevice = data.find(String.valueOf(Application.thisDeviceInstance));
            if (thisDevice != null) return thisDevice;
            throw new XDException(Errors.BAD_LINK_TARGET, data, "Could not resolve the alias \".this\" under the given path; no device with instance "+Application.thisDeviceInstance+" was found",data);
        }
        else if (name.equals(".device")) {
            for (Data child : data.getChildren()) { // find the Device object among the children
                if (child.stringValueOf("object-type","").equals("device")) return child;
            }
            throw new XDException(Errors.BAD_LINK_TARGET, data, "Could not resolve the alias \".device\" under the given path",data);
        }
        else if (name.equals(".blt")) {
            return Eval.eval(data,".../.bacnet/.local/.this");
        }
        else if (name.equals(".bltd")) {
            return Eval.eval(data,".../.bacnet/.local/.this/.device");
        }
        return null;
    }

}
