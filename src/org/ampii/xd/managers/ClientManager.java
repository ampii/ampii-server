// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.managers;

import org.ampii.xd.application.Application;
import org.ampii.xd.application.Policy;
import org.ampii.xd.bindings.Binding;
import org.ampii.xd.bindings.DefaultBinding;
import org.ampii.xd.bindings.DefaultBindingPolicy;
import org.ampii.xd.client.Client;
import org.ampii.xd.common.Log;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.*;
import org.ampii.xd.definitions.Instances;
import org.ampii.xd.data.Context;

import java.util.*;

import static org.ampii.xd.data.Meta.*;

/**
 * Nonstandard extension - Manages the data under /client and the operations that occur on it.
 *
 * @author daverobin
 */
public class ClientManager {

    /**
     * The native class that holds the info for each /client record
     */
    private static class ClientRecord {
        public String  name;          // will be assigned a GUID
        public int     interval=60;   // seconds for polling, 0=cov
        public int     timer;         // seconds left before next poll
        public String  source;        // required
        public String  type="Any";    // definition type name
        public String  authorization; // null = none
        public Data    data;          // null = none received yet
        public boolean error;         // defaults to false
        public String  status="idle";
    }

    private static List<ClientRecord> records = new ArrayList<>();  // this is the native storage for "/client" items

    public  static Binding  getBinding() { return theBinding; }

    private static Binding  theBinding = new DefaultBinding() {    // singleton binding for connecting "/.subs" to SubsManager.records
        @Override public void         preread(Data data)                   throws XDException {        ClientManager.preread(data); }
        @Override public Data         prefind(Data data, String name)      throws XDException { return ClientManager.prefind(data, name); }
        @Override public Data         prepost(Data target, Data given)     throws XDException { return ClientManager.prepost(target, given);}
        @Override public DataList     getContextualizedChildren(Data data) throws XDException { return ClientManager.getContextualizedChildren(data); }
        @Override public boolean      commit(Data data)                    throws XDException { return ClientManager.commit(data); }
        @Override public Policy       getPolicy()                                             { return thePolicy; }
    };

    private static Policy thePolicy = new DefaultBindingPolicy() {
        @Override public boolean    allowCreate(Data target, String name, String type, Base base) {
            return Rules.isChild(name) || name.equals(Meta.TYPE) || name.equals(Meta.VIA) || name.equals(Meta.WRITABLE);
        }
    };

    private static DataList getContextualizedChildren(Data data) throws XDException {
        Context context = data.getContext();
        if (!context.isTarget(data)) return new DataList(true,false,null); // if we're not the target level, just return a truncated list
        List<ClientRecord> recordList = new ArrayList<>(records);          // get a separate list so we can reverse it
        if (context.getReverse()) Collections.reverse(recordList);
        Iterator<ClientRecord> iterator = recordList.iterator();           // then get an iterator for that list of records,
        return context.filterChildren(new Iterator<Data>() {               // and use it to generate a stream of Data items for the records.
            @Override public boolean hasNext() { return iterator.hasNext(); }
            @Override public Data    next()    { return recordToData(iterator.next()); }
        }, context.isTarget(data));
    }

    private static void    preread(Data data) throws XDException {
        data.set(COUNT,records.size()); // always set the $count metadata to the actual number of records we have (won't match local children present)
    }

    private static Data    prefind(Data data, String name) throws XDException {
        ClientRecord record = findRecord(name); // try to make a single record exist
        if (record != null) {
            Data recordData = recordToData(record);
            data.addLocal(recordData); // don't just return it, must add locally or commit() will not find it if you write to it!
            return recordData;
        }
        return null;
    }

    private static ClientRecord findRecord(String name) {
        if (Rules.isChild(name)) for (ClientRecord record : records) if (record.name.equals(name)) return record;
        return null;
    }

    private static boolean  commit(Data data) throws XDException {
        for (Data recordData : data.getLocalChildren()) { // for each of the given record data items...
            ClientRecord record = findRecord(recordData.getName());
            if (record == null) records.add(dataToRecord(recordData));
            else if (recordData.isDeleted()) records.remove(record);
        }
        return true; // we handled it.
    }

    private static Data     prepost(Data target, Data data) throws XDException {
        // called upon POST to /client
        // we don't store anything here but this gives us an opportunity to throw problems before commit is called.
        ClientRecord record = dataToRecord(data);
        record.name = UUID.randomUUID().toString(); // names created under /.multi should not be reused
        return recordToData(record);
    }

    private static ClientRecord dataToRecord(Data data) throws XDException {
        ClientRecord record  = new ClientRecord();
        record.name          = data.getName();
        record.source        = data.stringValueOf("source", "");
        record.type          = data.stringValueOf("type", "Any");
        record.authorization = data.stringValueOf("authorization", null);
        record.interval      = data.intValueOf("interval", 60);
        return record;
    }

    private static Data      recordToData(ClientRecord record)  {
        try {
            /*
            <Composition  name="org.ampii.types.ClientRecord">
                <String     name="source"/>
                <String     name="type"/>
                <String     name="authorization" optional="true"/>
                <Integer    name="interval"      optional="true"/>
                <Integer    name="timer"         optional="true"/>
                <Any        name="value"         optional="true"/>
                <Boolean    name="error"         optional="true"/>
                <String     name="status"        optional="true"/>
            </Composition>
            */
            Data recordData = Instances.makeInstance("org.ampii.types.ClientRecord", record.name);
            recordData.addLocal(new StringData("source",record.source));
            recordData.addLocal(new StringData("type",record.type));
            if (record.authorization!= null) recordData.addLocal(new StringData("authorization",record.authorization));
            if (record.data != null) recordData.addLocal(record.data.makeDeepCopy());
            recordData.addLocal(new BooleanData("error",record.error));
            recordData.addLocal(new StringData("status",record.status));
            recordData.addLocal(new UnsignedData("timer", record.timer));
            return recordData;
        }
        catch (XDException e) { throw new XDError("ClientManager.recordToData() had internal failure",e); }
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
                    Thread.sleep(Application.clientWatchInterval);
                    if (shutdown) break;
                    ClientManager.checkRecords(Application.clientWatchInterval / 1000);
                }
                catch (InterruptedException e) {
                    Log.logSevere("Multi Watcher got interrupted: " + e.getMessage());
                }
            }
        }
    }

    private static void  checkRecords(int elapsed) {
        for (ClientRecord record : records) {
            record.timer -= elapsed;
            if (record.timer < 0) {
                record.timer = record.interval;
                doOne(record);
            }
        }
    }

    private static void  doOne(ClientRecord record) {
        try {
            Data data = Instances.makeInstance(record.type,"data");
            record.data = Client.doHttp(record.source, "GET", data, "", null, "", "application/json", null);
            record.error = false;
            record.status = "Success";
        } catch (XDException e) {
            record.error = true;
            record.status = "Error: " + e.getLocalizedMessage();
        }
    }


}
