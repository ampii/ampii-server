// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.application;

import org.ampii.xd.application.hooks.BindingHooks;
import org.ampii.xd.bindings.Binding;
import org.ampii.xd.bindings.DefaultBinding;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.DataList;
import org.ampii.xd.data.Meta;
import org.ampii.xd.data.basetypes.UnsignedData;
import org.ampii.xd.database.DataStore;
import org.ampii.xd.managers.*;
import org.ampii.xd.security.AuthManager;

/**
 * This is a way to bind a data item data to some kind of backend processing. See {@link Binding} for more.
 * <p>
 * At the moment, this is based on a metadata string. This is bad because it is rather slow to do the lookup,
 * but it is good because nothing special is needed to persist it.
 * <p>
 * There is no automagical connection between classes wanting to provide bindings and the hard-coded switch below.
 * If you want to n binding type without changing this file, create a {@link BindingHooks.External} interface and
 * inject it with {@link BindingHooks#registerExternal}
 *
 * @author daverobin
 */
public class Bindings {

    public static final String ROOT_AUTH        = "auth";
    public static final String ROOT_MULTI       = "multi";
    public static final String ROOT_SUBS        = "subs";
    public static final String DATA_HISTORIES   = "histories";
    public static final String DATA_EVENTS      = "events";
    public static final String DATA_NODES       = "nodes";
    public static final String DATA_OBJECTS     = "objects";
    public static final String DATA_REVISION    = "revision";
    public static final String ROOT_CALLBACK    = "callback";
    public static final String LARGE_VALUE      = "large";
    public static final String CLIENT           = "client";

    public static Binding getBinding(Data data, String bindingName) {
        Binding binding = BindingHooks.getBinding(data,bindingName);
        if (binding != null) return binding;
        // if no external hook, then look for built in rules
        switch(bindingName) {
            case ROOT_AUTH:        return AuthManager.getBinding();
            case ROOT_MULTI:       return MultiManager.getBinding();
            case ROOT_SUBS:        return SubsManager.getBinding();
            case DATA_HISTORIES:   return IndexManager.getHistoriesBinding();
            case DATA_EVENTS:      return IndexManager.getEventsBinding();
            case DATA_NODES:       return IndexManager.getNodesBinding();
            case DATA_OBJECTS:     return IndexManager.getObjectsBinding();
            case DATA_REVISION:    return DataStore.getRevisionBinding();
            case ROOT_CALLBACK:    return CallbackManager.getBinding();
            case LARGE_VALUE:      return theLargeValueBinding;
            case CLIENT:           return ClientManager.getBinding();
            default: throw new XDError(data, "Unknown binding name '"+bindingName+"'");
        }
    }

    public static LargeValueBinding theLargeValueBinding = new LargeValueBinding();

    /**
     *
     */
    public static class LargeValueBinding extends DefaultBinding { // default large value binding
        @Override public String getContextualizedValue(Data data) throws XDException {
            return data.getContext().isTarget(data)? data.getContext().makeContextualizedValue(data) :  ""; // truncate value when not top
        }
        @Override public DataList getContextualizedMetadata(Data data) throws XDException {
            if (!data.getContext().isTarget(data)) { // truncate value when not top
                DataList metadata = data.getContext().makeContextualizedMetadata(data);
                metadata.add(new UnsignedData(Meta.ERROR, Errors.TOO_LARGE));
                return metadata;
            }
            else return null;
        }
    }

}
