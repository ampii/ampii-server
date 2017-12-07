// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.managers;

import org.ampii.xd.application.Application;
import org.ampii.xd.application.Policy;
import org.ampii.xd.bindings.Binding;
import org.ampii.xd.bindings.DefaultBinding;
import org.ampii.xd.bindings.DefaultBindingPolicy;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.Log;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.*;
import org.ampii.xd.database.Session;
import org.ampii.xd.definitions.Instances;
import org.ampii.xd.data.Context;
import org.ampii.xd.resolver.Eval;

import java.util.*;

import static org.ampii.xd.data.Meta.*;

/**
 * Manages the data under /.multi and the operations that occur on it.
 * <p>
 * It's an example of data-to-native-to-data binding code.
 *
 * @author daverobin
 */
public class MultiManager {

    /**
     * The native class that holds the info for each /.multi record
     */
    private static class MultiRecord {
        public String       name = ".anonymous";   // name gets assigned to a GUID if persisted
        public int          lifetime;
        public List<String> paths = new ArrayList<>();
    }

    private static List<MultiRecord> records = new ArrayList<>();  // this is the native storage for "/.multi" items

    public static Binding   getBinding()            { return theBinding; }

    private static Binding theBinding = new DefaultBinding() {
        @Override public Integer      getTotalCount()                                         { return MultiManager.getTotalCount(); }                       // true count of children for sparse bindings, or null
        @Override public Data         prefind(Data data, String name)      throws XDException { return MultiManager.prefind(data, name); }
        @Override public DataList     getContextualizedChildren(Data data) throws XDException { return MultiManager.getContextualizedChildren(data); }
        @Override public Data         prepost(Data target, Data given)     throws XDException { return MultiManager.prepost(target, given);}
        @Override public boolean      commit(Data data)                    throws XDException { return MultiManager.commit(data); }
        @Override public Policy getPolicy()                      { return thePolicy; }
    };

    private static Policy thePolicy = new DefaultBindingPolicy() {
        @Override public boolean    allowCreate(Data target, String name, String type, Base base) {
            return Rules.isChild(name) || name.equals(Meta.TYPE) || name.equals(Meta.VIA) || name.equals(Meta.WRITABLE);
        }
    };

    private static Integer    getTotalCount()  {
        return records.size();
    }

    private static Data    prefind(Data data, String name) throws XDException {
        MultiRecord record = findRecord(name); // try to make a single record exist
        if (record != null) {
            Data recordData = recordToData(record,data.getContext());
            data.addLocal(recordData); // don't just return it, must add locally or commit() will not find it if you write to it!
            return recordData;
        }
        return null;
    }

    private static DataList getContextualizedChildren(Data data) throws XDException {
        Context context = data.getContext();
        if (!context.isTarget(data)) return new DataList(true,false,null); // if we're not the target level, just return a truncated list
        List<MultiRecord> recordList = new ArrayList<>(records);           // get a separate list so we can reverse it
        if (context.getReverse()) Collections.reverse(recordList);
        Iterator<MultiRecord> iterator = recordList.iterator();            // then get an iterator for that list of records,
        return context.filterChildren(new Iterator<Data>() {    // and use it to generate a stream of Data items for the records.
            @Override public boolean hasNext() { return iterator.hasNext(); }
            @Override public Data    next()    { return recordToData(iterator.next(), data.getContext()); }
        }, context.isTarget(data));
    }

    private static MultiRecord findRecord(String name) {
        if (Rules.isChild(name)) for (MultiRecord record : records) if (record.name.equals(name)) return record;
        return null;
    }

    private static boolean  commit(Data data) throws XDException {
        for (Data recordData : data.getLocalChildren()) { // for each of the given record data items...
            MultiRecord record = findRecord(recordData.getName());
            if (record != null) {
                // the only thing you can write to is the lifetime (unlike subscriptions where you can modify the record's internal lists)
                if (recordData.isDeleted()) records.remove(record);
                else record.lifetime = recordData.intValueOf("lifetime",record.lifetime); // and update the lifetime
            }
            else {
                records.add(recordFromData(recordData));
            }
        }
        return true; // we handled it.
    }

    private static Data     prepost(Data target, Data data) throws XDException {
        // called upon POST to /.multi
        Data valuesList = data.findLocal("values");
        if (valuesList == null) throw new XDException(Errors.LIST_OF_PATHS_IS_EMPTY,"The 'values' list is missing");
        boolean doingRead=false, doingWrite=false;
        for (Data one : valuesList.getLocalChildren()) {
            if (one.getBase() == Base.ANY ) {
                doingRead = true;
                if (doingWrite) throw new XDException(Errors.INCONSISTENT_VALUES,"Cannot have both Any and non-Any data in 'values' list.");
            }
            else {
                doingWrite = true;
                if (doingRead) throw new XDException(Errors.INCONSISTENT_VALUES,"Cannot have both Any and non-Any data in 'values' list.");
            }
        }
        int lifetime = data.intValueOf("lifetime",0);
        if (doingRead) {
            // in the read case, we reuse the recordToData() code by creating a native record then "reading" it.
            MultiRecord record = new MultiRecord();
            // add all the given $via paths to the native record
            for (Data one : valuesList.getLocalChildren()) record.paths.add(one.get(Meta.VIA).stringValue()); // throws if $via not present
            // is this ephemeral or persistent?
            if (lifetime != 0) {
                record.name = UUID.randomUUID().toString(); // names created under /.multi should not be reused
                record.lifetime = lifetime;
            }
            else {
                record.name = "..ephemeral";
            }
            data = recordToData(record, target.getContext()); // IMPORTANT!! we need the context of the requester because we are reading data other than the target
            // if this is an HTTP POST (most likely), and nothing else goes wrong, then this data will come back to us in commit() and we will persist it then
        }
        else if (doingWrite) {
            if (lifetime != 0) throw new XDException(Errors.INCONSISTENT_VALUES,"Cannot specify 'lifetime' and non-Any value");
            // in the write case, we just modify the given data in-place rather than creating a native record
            data = Instances.makeInstance("0-BACnetWsMultiRecord","..ephemeral").put(data);  // clean up parsed data into proper instance and mark it ephemeral
            for (Data one : data.getLocal("values").getLocalChildren()) {
                // rather than doing an all-or-nothing, a /.multi write operation works by writing as many as possible and returning a list of failures.
                // so we DO EACH WRITE IN ITS OWN SESSION and just record the failures.
                Session subsession = target.getSession().makeWriteSubsession("MultiManager.prepost");// IMPORTANT! make sub session so we keep the same context for authorization
                try {
                    String path = one.getLocal(Meta.VIA).stringValue(); // will throw if $via is missing
                    if (path.length() == 0) throw new XDException(Errors.METADATA_NOT_FOUND, one, "The 'via' metadata is empty");
                    Data referent = Eval.eval(subsession.getRoot(),path);
                    referent.put(one,Data.PUT_OPTION_NO_NAME_CHECK);
                    subsession.commit(); // if the put succeeded, then commit it.
                }
                catch (XDException e) {
                    one.set(Meta.ERROR,e.getErrorNumber());
                    one.set(Meta.ERRORTEXT,e.getErrorText());
                    data.getOrCreate(Meta.FAILURES).post(new LinkData("", "values/" + one.getName()));
                }
                finally {
                    subsession.discard();
                }
            }
        }
        else throw new XDException(Errors.LIST_OF_PATHS_IS_EMPTY,"The 'values' list is empty");
        // make sure $via, $value, $error, etc. shows up in the results to the client
        data.setContext(target.getContext());
        data.getContext().getMetadataFilter().add(Meta.VIA,Meta.VALUE,Meta.ERROR,Meta.ERRORTEXT,Meta.FAILURES);
        return data;
    }

    private static MultiRecord  recordFromData(Data data) throws XDException {
        Data valuesList = data.findLocal("values");
        if (valuesList == null) throw new XDException(Errors.LIST_OF_PATHS_IS_EMPTY,"The 'values' list is missing");
        MultiRecord record = new MultiRecord();
        record.name = data.getName();
        record.lifetime = data.intValueOf("lifetime",0);
        for (Data one : valuesList.getLocalChildren()) record.paths.add(one.getLocal(Meta.VIA).stringValue()); // throws if $via not present
        return record;
    }

    private static Data      recordToData(MultiRecord record, Context context)  {
        try {
            // <Composition name="0-BACnetWsMultiRecord">
            //   <Unsigned name="lifetime" optional="true"/>
            //   <List name="values"/>
            // </Composition>
            Data recordData = Instances.makeInstance("0-BACnetWsMultiRecord", record.name);
            // persistent records always have the optional "lifetime" member present and *writable*
            if (!record.name.equals("..ephemeral")) recordData.addLocal(new UnsignedData("lifetime", record.lifetime, new BooleanData(Meta.WRITABLE, true)));
            // the "values" List is non-optional so we just get() it from the instance
            Data valuesList = recordData.getLocal("values");
            for (int i = 0; i < record.paths.size(); i++) { // use traditional for loop here because we need index number as name
                String path = record.paths.get(i);
                Data entry = null; // will either be a copy of the target data or a fresh Any
                Session session = Session.makeReadSession("MultiManager",context); // IMPORTANT!! we use the context of the requester. no "confused deputies" here!
                try {
                    if (path.isEmpty()) throw new XDException(Errors.CANNOT_FOLLOW, "The 'via' metadata is empty");
                    Data target = Eval.eval(session.getRoot(),path);
                    if (target.getName().equals("..root")) throw new XDException(Errors.TOO_DEEP,"Can't read root with /.multi");
                    entry = target.makeDeepCopy(); // TODO: refactor this to use a shadow instead (but we will have to be given a session from above, not a local one here)
                    entry.setName(String.valueOf(i + 1)); // names start at "1"
                    entry.setIsFromAny(true); // don't forget that this is from an Any (will force $base to be included in JSON)
                } catch (XDException e) {
                    entry = new AnyData(String.valueOf(i + 1)); // result type remains an Any
                    entry.set(Meta.ERROR,e.getErrorNumber());
                    entry.set(Meta.ERRORTEXT,e.getErrorText());
                    recordData.getOrCreate(Meta.FAILURES).post(new LinkData("", "values/" + (i + 1))); // point to this entry in parent $failures
                }
                finally {
                    session.discard();
                }
                entry.addLocal(new LinkData(Meta.VIA, path)); // assign path as $via because valuesList is unordered and client needs to match it up
                entry.setIsFromAny(true); // generators need to know this was made from an Any to return $base and $type
                valuesList.addLocal(entry);    // we've already assigned names so we don't need to post()
            }
            return recordData;
        }
        catch (XDException e) { throw new XDError("MultiManager.recordToData() had internal failure",e); }
    }

    ////////////////// Watcher thread //////////////

    private static boolean shutdown;
    private static Thread  thread;

    public static void start() {
        shutdown = false;
        if (thread != null) return;
        thread = new Thread(new Watcher());
        thread.setDaemon(true);
        thread.start();
    }

    public static void stop() {
        shutdown = true;
        if (thread == null) return;
        thread.interrupt();
    }

    private static class Watcher implements Runnable {
        public void run() {
            for (;;) {
                try {
                    Thread.sleep(Application.multiWatchInterval);
                    if (shutdown) break;
                    MultiManager.pruneRecords(Application.multiWatchInterval/1000);
                }
                catch (InterruptedException e) {
                    Log.logSevere("Multi Watcher got interrupted: " + e.getMessage());
                }
            }
        }
    }

    private static void  pruneRecords(int elapsed) {
        List<MultiRecord> toRemove = new ArrayList<>();
        for (MultiRecord record : records) {
            record.lifetime -= elapsed;
            if (record.lifetime <= 0) toRemove.add(record);
        }
        for (MultiRecord record : toRemove) {
            records.remove(record);
        }
    }



}
