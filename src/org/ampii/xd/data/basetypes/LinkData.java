// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.data.abstractions.AbstractTextData;
import org.ampii.xd.definitions.Builtins;
import org.ampii.xd.resolver.Eval;
import org.ampii.xd.server.Server;

/**
 * The implementation of the Link base type. Most behavior is provided by super classes and {@link AbstractData}
 *
 * @author daverobin
 */
public class LinkData extends AbstractTextData {

    public LinkData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.LINK);
    }

    @Override public Base getBase() { return Base.LINK; }

    public Data findTarget()  {  // returns null if can't be evaluated locally
        String path = Server.getDataPath(stringValue("")); // will return empty if it's not our data, must use "." if you want a relative path to yourself!
        if (path.isEmpty()) return null;
        // so we have a local pointer, go see if that data exists
        Data relativeTo = Rules.findBasisForRelativeLink(this);
        return Eval.eval(relativeTo, path, (Data)null); // will return default value of null if not found, absolute paths will override 'relativeTo', ,
    }

    // TODO restore this code for the new bindings environment
    /*
    private void checkTarget()  {  // sets or clears $error and $errorText for find(...) and getMetadata()
        String error = null;
        if (Server.isOurData(stringValue())) {
            Data target = findTarget();
            if (target != null) {  // we found a local target, now is it the right type?
                Data targetType = find(Meta.TARGETTYPE);
                if (targetType != null) {
                    if (Prototypes.findPrototypeFor(targetType.stringValue()) == null)
                        error = "Can't find definition for $targetType of '" + targetType.stringValue() + "'"; // is the targetType a valid definition name?
                    else if (!DataOps.isCompatibleType(target, targetType.stringValue()))
                        error = "Target data of type '" + target.getEffectiveType() + "' is not compatible with $targetType of '" + targetType + "'";
                }
            }
            else { // we didn't find the local data
                error = "Can't find local data for link";
            }
        }
        if (error != null) setError(error);
        else               clearError();
    }

    private void clearError()  {
        removeLocal(Meta.ERROR);
        removeLocal(Meta.ERRORTEXT);
    }

    private void setError(String message) {
        try {
            addLocal(new UnsignedData(Meta.ERROR, Errors.BAD_LINK_TARGET));
            addLocal(new StringData(Meta.ERRORTEXT, message));
        } catch(XDException e) { throw new XDError("Link.setError() failed"); }
    }

    */

}
