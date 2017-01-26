// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.resolver;

import org.ampii.xd.application.Historian;
import org.ampii.xd.application.hooks.FabricatorHooks;
import org.ampii.xd.client.Client;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.BooleanData;
import org.ampii.xd.data.basetypes.LinkData;
import org.ampii.xd.data.basetypes.ListData;
import org.ampii.xd.data.basetypes.StringData;
import org.ampii.xd.data.Context;

/**
 * This can make "fabricated" ephemeral data that is not actually stored persistently. We have to fabricate these on the
 * fly. They are read-only, so we don't have to worry if someone tries to set the values in these parentless ephemerals.
 * If you want to add a fabricator without changing this core AMPII file, make a {@link FabricatorHooks.External} and
 * inject it with {@link FabricatorHooks#registerExternal}.
 *
 * @author drobin
 */
public class Fabricator {

    public static Data fabricate(Data data, String name) throws XDException {
        Data results = FabricatorHooks.fabricate(data, name);
        if (results != null) {
            return results;
        }
        else if (name.equals(Meta.NAME)) {
            return makeEphemeral(data, Base.STRING, name, data.getName());
        }
        else if (name.equals(Meta.BASE)) {
            return makeEphemeral(data, Base.STRING, name, Base.toString(data.getBase()));
        }
        else if (name.equals(Meta.COUNT)) {
            return makeEphemeral(data, Base.UNSIGNED, name, data.getCount());
        }
        else if (name.equals(Meta.CHILDREN)) {
            return makeEphemeral(data, Base.STRINGSET, name, concatenateNames(data.getChildren()));
        }
        else if (name.equals(Meta.DESCENDANTS)) {
            return makeDescendants(data);
        }
        else if (name.equals(Meta.HASHISTORY)) {
            return makeEphemeral(data, Base.BOOLEAN, name, Historian.findHistoryLogBuffer(data) != null);
        }
        else if (name.equals(Meta.HISTORY)) {
            return  Historian.findHistoryLogBuffer(data);
        }
        else if (name.equals(Meta.INALARM) || name.equals(Meta.OVERRIDDEN) || name.equals(Meta.FAULT) || name.equals(Meta.OUTOFSERVICE)) {
            // according to the spec, if these don't actually exist, they are invented on the fly... but only for points
            return (data.find(name) == null && data.stringValueOf(Meta.NODETYPE, "").equals("Point"))? makeEphemeral(data, Base.BOOLEAN, name, false) : null;
        }
        else if (name.equals(Meta.TYPE)) {
            String typeName = data.getEffectiveType();
            return typeName.isEmpty()? null : makeEphemeral(data, Base.STRING, name, typeName);
        }
        else if ( // return parentaly-inherited effective values
                  name.equals(Meta.AUTHREAD) ||
                  name.equals(Meta.AUTHWRITE) ||
                  name.equals(Meta.AUTHVISIBLE) ||
                  name.equals(Meta.READABLE) ||
                  name.equals(Meta.WRITABLE) ||
                  name.equals(Meta.VARIABILITY) ||
                  name.equals(Meta.VOLATILITY) ||
                  name.equals(Meta.ERROR) ||
                  name.equals(Meta.ERRORTEXT) ||
                  name.equals(Meta.PUBLISHED) ||
                  name.equals(Meta.UPDATED) ||
                  name.equals(Meta.AUTHOR)
                ) {
            Data found = data.findEffective(name);
            if (found != null) return makeEphemeral(data, found.getBase(), name, found.getValue());
            else               return null;
        }
        // looking for Meta.TARGET? that's an alias (redirector to existing data) not a fabrication, so that is handled by the Alias class
        return null;
    }

    public static String concatenateNames(DataList items) {
        StringBuilder sb = new StringBuilder();
        for (Data child : items) {
            if (sb.length() != 0) sb.append(';');
            sb.append(child.getName());
        }
        return sb.toString();
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    private static Data makeEphemeral(Data data, Base base, String name, Object... initializers) throws XDException{
        Data results = DataFactory.make(base, name, initializers);
        results.setParent(data); // only sets parent, does not actually "add"
        return results;
    }

    private static Data makeDescendants(Data data) throws XDException {
        Context context = data.getContext();
        Data list = new ListData(Meta.DESCENDANTS,new BooleanData(Meta.WRITABLE,true));
        list.setParent(data); // only sets the parent so that relative paths can be evaluated, does not "add" to parent
        context.resetCur();
        // 'max-result' *could* start out 0 (dumb client or evil tester), so we have to check it first and return a $next pointer with max-results=1
        if (context.cur_max == 0) {
            list.addLocal(new StringData(Meta.NEXT, Client.makeSimpleNextPointer(context)));
        }
        else {
            _getDescendants_Helper(data, list, data, context);
        }
        // since we have already self-limited, the context limit no longer apply (otherwise they get applied twice!)
        // TODO: check this:
        context.setSkip(null);
        context.setMaxResults(null);
        return list;
    }

    private static void _getDescendants_Helper(Data base, Data list, Data data, Context context) throws XDException {
        if (context.cur_descendantDepth >= context.getDescendantDepth()) return;
        for (Data child: data.getChildren()) {
            Data link = new LinkData("", Path.toRelativePath(base, child)); // "" is just a temporary name for now
            // preset the parent so filter can evaluate silly things like filter=../{something in parent}
            link.setParent(list);
            if (context.canIncludeChild(link)) {
                context.cur_descendantNumber++; // regardless of whether it will be skipped or not, the number indicates the position in the unskipped list
                // for every result that *would* go in the list, skip over the first 'skip-results'
                if (context.cur_skip > 0) {
                    if (list.find(Meta.PARTIAL)==null) list.addLocal(new BooleanData(Meta.PARTIAL, true)); // if we skipped any, we're partial
                    context.cur_skip--;
                    continue;
                }
                link.setName(String.valueOf(context.cur_descendantNumber));
                list.post(link);  // yay! someone made the cut!
                if (--context.cur_max == 0) { // if this is the last one that we can return, return a $next pointer
                    list.addLocal(new StringData(Meta.NEXT, Client.makeSimpleNextPointer(context)));
                    break;  // no more
                }
            }
            if (context.cur_max != 0) { // if we can report more, then descend and get them
                context.cur_descendantDepth++;
                _getDescendants_Helper(base, list, child, context);
                context.cur_descendantDepth--;
            }
        }
    }


}
