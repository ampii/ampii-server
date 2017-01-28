// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.managers;

import org.ampii.xd.bindings.Binding;
import org.ampii.xd.bindings.DefaultBinding;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.LinkData;
import org.ampii.xd.resolver.Eval;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The default Index methods just simply create the needed indexes in /.data on the fly by brute force.
 * <p>
 * These simplistic default bindings search most of the data in the system!  in memory!  in one session!
 * <p>
 * Any *real* application will likely want to replace these with sparse bindings. Additionally, a real application
 * will want to maintain these lists persistently, not make them on the fly, and should include "watcher" hooks to
 * notice when changes have happened that affect the persistent indexes.
 *
 * @author drobin
 */
public class IndexManager {

    private static class IndexLocation {
        public IndexLocation(String path, int depth) { this.path = path; this.depth = depth; }
        String path;
        int    depth;
    }

    static List<IndexLocation> historyLocations = new ArrayList<>();
    static List<IndexLocation> eventsLocations  = new ArrayList<>();
    static List<IndexLocation> nodesLocations   = new ArrayList<>();
    static List<IndexLocation> objectsLocations = new ArrayList<>();

    static {

        addHistoriesLocation(".../.trees");
        addHistoriesLocation(".../my-data");
        addHistoriesLocation(".../test-data");

        addEventsLocation(".../.bacnet", 3);

        addObjectsLocation(".../.bacnet", 3);

        addNodesLocation(".../.bacnet", 3);
        addNodesLocation(".../.trees");
        addNodesLocation(".../my-data");
        addNodesLocation(".../test-data");

    }

    static public void addHistoriesLocation(String path)             { historyLocations.add(new IndexLocation(path,Integer.MAX_VALUE)); }
    static public void addHistoriesLocation(String path, int depth)  { historyLocations.add(new IndexLocation(path,depth)); }
    static public void addEventsLocation(String path)                { eventsLocations.add(new IndexLocation(path,Integer.MAX_VALUE)); }
    static public void addEventsLocation(String path, int depth)     { eventsLocations.add(new IndexLocation(path, depth)); }
    static public void addNodesLocation(String path)                 { nodesLocations.add(new IndexLocation(path,Integer.MAX_VALUE)); }
    static public void addNodesLocation(String path, int depth)      { nodesLocations.add(new IndexLocation(path, depth)); }
    static public void addObjectsLocation(String path)               { objectsLocations.add(new IndexLocation(path,Integer.MAX_VALUE)); }
    static public void addObjectsLocation(String path, int depth)    { objectsLocations.add(new IndexLocation(path, depth)); }

    private static Binding theHistoriesBinding = new DefaultBinding() {
        // at present, this just looks for the presence of the AMPII_HISTORY_LOCATION metadata; it doesn't consult the Historian class
        @Override public void preread(Data target) throws XDException {
            prereadSomething(target, historyLocations,
                    (t, d) -> {
                        try {
                            if (d.find(Meta.AMPII_HISTORY_LOCATION,null)!=null) {
                                t.post(new LinkData("", d.getPath()));
                            }
                        } catch (XDException e) {} // can't throw from BiConsumer
                    }
            );
        }
    };


    private static Binding theEventsBinding = new DefaultBinding() {
        // at present, this just looks for BACnet Event Log objects and makes link to the log-buffer property
        @Override public void preread(Data target) throws XDException {
            prereadSomething(target, eventsLocations,
                    (t, d) -> {
                        try {
                            if (d.stringValueOf("object-type","").equals("event-log")) {
                                t.post(new LinkData("", d.getPath()+"/log-buffer"));
                            }
                        } catch (XDException e) {} // can't throw from BiConsumer
                    }
            );
        }
    };


    private static Binding theNodesBinding = new DefaultBinding() {
        // this just looks for the presence of the $nodeType metadata
        @Override public void preread(Data target) throws XDException {
            prereadSomething(target, nodesLocations,
                    (t, d) -> {
                        try {
                            String nodeType = d.stringValueOf(Meta.NODETYPE, "");
                            // post the link to the data under a list with the same name as the node type
                            if (!nodeType.isEmpty()) t.getOrCreate(nodeType,null,Base.LIST).post(new LinkData("", d.getPath()));
                        } catch (XDException e) { } // can't throw from BiConsumer
                    }
            );
        }
    };

    private static Binding theObjectsBinding = new DefaultBinding() {
        // this looks for anything of base type Object
        @Override public void preread(Data target) throws XDException {
            prereadSomething(target, objectsLocations,
                    (t, d) -> {
                        try { if (d.getBase()==Base.OBJECT) target.post(new LinkData("", d.getPath())); }
                        catch(XDException e) {} // can't throw from BiConsumer
                    }
            );
        }
    };

    public static Binding   getHistoriesBinding()  { return theHistoriesBinding; }
    public static Binding   getEventsBinding()     { return theEventsBinding; }
    public static Binding   getNodesBinding()      { return theNodesBinding; }
    public static Binding   getObjectsBinding()    { return theObjectsBinding; }


    private static void  prereadSomething(Data target, List<IndexLocation> locations, BiConsumer<Data,Data> consumer) throws XDException {
        target.getContext().getAuthorizer().enterGodMode(); // we need to write to the unwritable lists /.data/xxx
        for (IndexLocation location : locations) {
            Data data = Eval.eval(target, location.path);
            try { check(data, target, 0, location.depth, consumer); } catch (XDException e) { }
        }
        target.getContext().getAuthorizer().exitGodMode();
    }


    private static void check(Data data, Data target, int curDepth, int depthLimit, BiConsumer<Data,Data> consumer) throws XDException {
        consumer.accept(target, data);
        if (curDepth < depthLimit) for (Data child : data.getChildren()) check(child, target, curDepth + 1, depthLimit, consumer);
    }


}
