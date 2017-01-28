// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.managers;

import org.ampii.xd.application.Application;
import org.ampii.xd.application.Policy;
import org.ampii.xd.bindings.Binding;
import org.ampii.xd.bindings.DefaultBinding;
import org.ampii.xd.bindings.DefaultBindingPolicy;
import org.ampii.xd.client.Client;
import org.ampii.xd.common.*;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.*;
import org.ampii.xd.database.Session;
import org.ampii.xd.definitions.Instances;
import org.ampii.xd.data.Context;
import org.ampii.xd.marshallers.JSONGenerator;
import org.ampii.xd.marshallers.XMLGenerator;
import org.ampii.xd.resolver.Eval;
import org.ampii.xd.resolver.Path;
import org.ampii.xd.server.Server;
import static org.ampii.xd.data.Meta.*;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages the data under /.subs and the operations that occur on it.
 * <p>
 * It's an example of data-to-native-to-data binding code.
 *
 * @author daverobin
 */
public class SubsManager {

    /**
     * The native class that holds the info for each ./subs record
     */
    private static class SubsRecord {   //
        public String  name = "";       // will be given a GUID name
        public String  label;           // optional, can be null;
        public String  callback;        // optional, can be null;
        public int     lifetime = 0;
        public String  callbackError;   // optional, can be null;
        public String  dataError;       // optional, can be null;
        public List<SubsCovEntry> covs;
        public List<SubsLogEntry> logs;
    }
    private static class SubsCovEntry {
        public String  path;
        public float   increment = -1.0F;  // optional, -1 means none
        public Object  previousValue = null;
    }
    private static class SubsLogEntry {
        public String  path;
        public int     frequency; // optional, 0=none and 1="on-update", 2="hourly", 3="daily"
    }

    private static List<SubsRecord> records = new ArrayList<>();  // this is the native storage for "/.subs" items

    public static Binding  getBinding()  { return theBinding; }

    private static Binding theBinding = new DefaultBinding() {
        @Override public void         preread(Data data)               throws XDException {        SubsManager.preread(data); }
        @Override public Data         prefind(Data data, String name)  throws XDException { return SubsManager.prefind(data, name); }
        @Override public DataList getContextualizedChildren(Data data)             throws XDException { return SubsManager.prefilter(data); }
        @Override public Data         prepost(Data target, Data given) throws XDException { return SubsManager.prepost(given);}
        @Override public boolean      commit(Data data)                throws XDException { return SubsManager.commit(data); }
        @Override public Policy getPolicy()                      { return thePolicy; }
    };

    private static Policy thePolicy = new DefaultBindingPolicy() {
        @Override public boolean    allowCreate(Data target, String name, String type, Base base) {
            return Rules.isChild(name) || name.equals(Meta.TYPE);
        }
    };

    private static DataList prefilter(Data data) throws XDException {
        Context context = data.getContext();
        if (!context.isTarget(data)) return new DataList(true,false,null); // if we're not the target level, just return a truncated list
        List<SubsRecord> recordList = new ArrayList<>(records);         // get a separate list so we can reverse it
        if (context.getReverse()) Collections.reverse(recordList);
        Iterator<SubsRecord> iterator = recordList.iterator();          // then get an iterator for that list of records,
        return context.filterChildren(new Iterator<Data>() {            // and use it to generate a stream of Data items for the records.
            @Override public boolean hasNext() { return iterator.hasNext(); }
            @Override public Data    next()    { return recordToData(iterator.next()); }
        }, context.isTarget(data));
    }

    private static void    preread(Data data) throws XDException {
        data.set(COUNT,records.size()); // always set the $count metadata to the actual number of records we have (won't match local children present)
    }

    private static Data    prefind(Data data, String name) throws XDException {
        SubsRecord record = findRecord(name); // try to make a single record exist
        if (record != null) {
            Data recordData = recordToData(record);
            data.addLocal(recordData); // don't just return it, must add locally or commit() will not find it if you write to it!
            return recordData;
        }
        return null;
    }

    private static SubsRecord findRecord(String name) {
        if (Rules.isChild(name)) for (SubsRecord record : records) if (record.name.equals(name)) return record;
        return null;
    }

    private static boolean  commit(Data data) throws XDException {
        for (Data recordData : data.getLocalChildren()) { // for each of the given record data items...
            SubsRecord record = findRecord(recordData.getName());
            if (record != null) {
                if (recordData.isDeleted()) records.remove(record);
                else dataToRecord(record, recordData); // update lifetime and covs and logs lists
            }
            else {
                record = new SubsRecord(); // make it
                dataToRecord(record,recordData);      // update it
                records.add(record);                  // persist it
            }
        }
        return true; // we handled it.
    }

    private static Data prepost(Data given) throws XDException {
        // called upon POST to /.subs
        // first, check and properly instantiate the freshly parsed data
        Data data = Instances.makeInstance("0-BACnetWsSubscriptionRecord",UUID.randomUUID().toString());  // use GUID for name
        data.put(given,Data.PUT_OPTION_USE_POST_RULES); // this will validate the given data and throw any problems
        data.setName(UUID.randomUUID().toString()); // names created under /.subs should not be reused so give it a unique name
        //TODO$$$ validate that the targets are readable/visible and...
        //TODO someday, keep a copy of the Authorizer with the subscription so that callbacks don't leak unauthorized data when serialized with an internal context
        //TODO we *should* expire the access to nonpublic data when the original token expires, but since the standard doesn't say that it might be a surprise!
        return data; // return it to post() so it can get it's new name for the Location header and give it back to us for commit()
    }


    private static void dataToRecord(SubsRecord record, Data data) throws XDException {
        Data givenCovs = data.find("covs");
        Data givenLogs = data.find("logs");
        if ((givenCovs == null || givenCovs.getLocalChildren().size()==0) && (givenLogs == null || givenLogs.getLocalChildren().size()==0))
            throw new XDException(Errors.LIST_OF_PATHS_IS_EMPTY, "The 'covs' and 'logs' lists are both empty and/or missing");
        record.name = data.getName();
        record.lifetime = data.intValueOf("lifetime", 0);
        record.label = data.stringValueOf("label", null);
        record.callback = data.stringValueOf("callback", "");
        record.callbackError = null;
        record.dataError = null;
        record.covs = null;
        if (givenCovs != null) for (Data givenCov : givenCovs.getChildren()) {
            if (record.covs == null) record.covs = new ArrayList<>();
            SubsCovEntry entry = new SubsCovEntry();
            record.covs.add(entry);
            entry.path = givenCov.stringValueOf("path", "");
            entry.increment = givenCov.floatValueOf("increment", -1.0F);
        }
        record.logs = null;
        if (givenLogs != null) for (Data givenLog : givenLogs.getChildren()) {
            if (record.logs == null) record.logs = new ArrayList<>();
            SubsLogEntry entry = new SubsLogEntry();
            record.logs.add(entry);
            entry.path = givenLog.stringValueOf("path", "");
            entry.frequency = frequencyFromString(givenLog.stringValueOf("frequency", ""));
        }
    }

    private static Data recordToData(SubsRecord record) {
        try {
            Data result = Instances.makeInstance("0-BACnetWsSubscriptionRecord", record.name);
            result.addLocal(new UnsignedData("lifetime", record.lifetime, new BooleanData(WRITABLE, true))); // lifetime is writable
            if (record.label != null) result.addLocal(new StringData("label", record.label));
            if (record.callback != null) result.addLocal(new StringData("callback", record.callback));
            if (record.callbackError != null) result.addLocal(new StringData("callbackError", record.callbackError));
            if (record.dataError != null) result.addLocal(new StringData("dataError", record.dataError));
            if (record.covs != null) {
                Data covs = new ListData("covs", new BooleanData(WRITABLE, true)); // covs list is writable
                result.addLocal(covs);
                int index = 1;
                for (SubsCovEntry cov : record.covs) {
                    Data member = new CompositionData(String.valueOf(index++)); // name = "1", "2", etc.
                    covs.addLocal(member);
                    member.addLocal(new StringData("path", cov.path));
                    if (cov.increment != -1.0F) member.addLocal(new RealData("increment", cov.increment));
                }
            }
            if (record.logs != null) {
                Data logs = new ListData("logs", new BooleanData(WRITABLE, true));  // logs list is writable
                result.addLocal(logs);
                int index = 1;
                for (SubsLogEntry log : record.logs) {
                    Data member = new CompositionData(String.valueOf(index++)); // name = "1", "2", etc.
                    logs.addLocal(member);
                    member.addLocal(new StringData("path", log.path));
                    if (log.frequency != 0) member.addLocal(new EnumeratedData("frequency", frequencyToString(log.frequency)));
                }
            }
            return result;
        }
        catch (XDException e) { throw new XDError("MultiManager.recordToData() had internal failure",e); }
    }

    private static String frequencyToString(int frequency) {
        return frequency==1 ? "on-update" : frequency==2 ? "hourly" : frequency==3? "daily" : "invalid";
    }

    private static int frequencyFromString(String frequency) {  // no exceptions, will have been validated by post()
        return frequency.equals("on-update") ? 1 : frequency.equals("hourly") ? 2 : frequency.equals("daily") ? 3 : 0;
    }

    ////////////////// Watcher threads //////////////

    static boolean shutdown;
    static Thread  subsWatcher;
    static Thread  queueWatcher;

    public static void start() {
        shutdown = false;
        if (subsWatcher == null) {
            subsWatcher = new Thread(new SubsWatcher());
            subsWatcher.setDaemon(true);
        }
        subsWatcher.start();
        if (queueWatcher == null) {
            queueWatcher = new Thread(new QueueWatcher());
            queueWatcher.setDaemon(true);
        }
        queueWatcher.start();
    }

    public static void stop() {
        shutdown = true;
        if (subsWatcher  != null) subsWatcher.interrupt();
        if (queueWatcher != null) queueWatcher.interrupt();
    }

    private static class CallbackInfo {
        SubsRecord record;
        String     body;
        String     contentType;
    }

    static BlockingQueue<CallbackInfo> queue = new LinkedBlockingQueue<CallbackInfo>();

    static class SubsWatcher implements Runnable {
        public void run() {
            try {
                for (; ; ) {
                    Thread.sleep(Application.subsWatchInterval);
                    if (shutdown) break;
                    // first process all the subscriptions
                    for (SubsRecord record : records) processSubscription(record);
                    // then prune expired records
                    SubsManager.pruneRecords(Application.multiWatchInterval / 1000);
                }
            }
            catch (InterruptedException e) {Log.logSevere("Subscription SubsWatcher interrupted: " + e.getMessage());}
            catch (XDException e) {Log.logSevere("Subscription SubsWatcher exception: " + e.getMessage());}
        }
    }

    private static void  pruneRecords(int elapsed) {
        List<SubsRecord> toRemove = new ArrayList<>();
        for (SubsRecord record : records) {
            record.lifetime -= elapsed;
            if (record.lifetime <= 0) toRemove.add(record);
        }
        for (SubsRecord record : toRemove) {
            records.remove(record);
        }
    }

    static class QueueWatcher implements Runnable {
        public void run() {
            try  {
                for (;;) {
                    CallbackInfo info = queue.take();
                    if (shutdown) break;
                    try { Client.doCallback(info.record.callback, info.body, info.contentType);}
                    catch (XDException e) { info.record.callbackError = e.getLocalizedMessage(); }
                }
            } catch (InterruptedException e) { Log.logSevere("Subscription QueueWatcher interrupted: " + e.getMessage()); }
        }
    }


    public static void processSubscription(SubsRecord record) throws XDException {
        record.dataError = null; // gets overwritten be any errors encountered while processing covs
        Session session = Session.makeWriteSession("SubsManager.processSubscription");
        for (SubsCovEntry entry : record.covs) {
            try {
                Data target = Eval.eval(session.getRoot(),entry.path);
                if (!target.canHaveValue()) throw new XDException(Errors.TARGET_DATATYPE,target,"target has no value for COV comparison");
                Object currentValue = target.getValue();
                if (!isApprovedType(currentValue)) throw new XDException(Errors.TARGET_DATATYPE,target,"incompatible datatype for COV comparison");
                if (entry.previousValue == null || isChange(currentValue, entry.previousValue, entry.increment)) {
                    queueCOVNotification(target,record);
                    entry.previousValue = currentValue;
                }
            }
            catch (Throwable e) {
                if (record.dataError == null) record.dataError = e.getLocalizedMessage(); // record the first error
            }
            finally {
                session.discard();
            }
        }
    }

    private static boolean isApprovedType(Object value) {
        return value instanceof Float || value instanceof Double || value instanceof Integer || value instanceof Long;
    }

    private static boolean isChange(Object currentValue, Object previousValue, double increment) {
        if ( currentValue instanceof Float) {
            float difference =  ((Float)currentValue) - ((Float)previousValue);
            if (difference < 0) difference = -difference;
            return  (difference > increment);
        }
        else if ( currentValue instanceof Double) {
            double difference =  ((Double)currentValue) - ((Double)previousValue);
            if (difference < 0) difference = -difference;
            return  (difference > increment);

        }
        else if ( currentValue instanceof Integer) {
            int difference = ((Integer)currentValue) - ((Integer)previousValue);
            if (difference < 0) difference = -difference;
            return  (difference > increment);
        }
        else if ( currentValue instanceof Long) {
            long difference = ((Long)currentValue) - ((Long)previousValue);
            if (difference < 0) difference = -difference;
            return  (difference > increment);
        }
        else return false; // shouldn't get here because we've already approved the datatype
    }

    private static void queueCOVNotification(Data target, SubsRecord record)  {
        if (record.callback == null || record.callback.isEmpty()) { // if there is no url, record complaint and leave
            record.callbackError = "? "+Errors.CALLBACK_FAILED+" 'callback' URL is empty";
            return;
        }
        Log.logInfo("Subscriptions: Sending the new value " + target.stringValue("<novalue>") + " for " + target.getName() + " to callback \"" + record.callback + "\"");
        try {
            //
            // we need a List with 'subscription' metadata set; each list member is a clone with 'via' pointing to the target
            //
            Data wrapper = new ListData("..callback-wrapper");
            wrapper.set(SUBSCRIPTION,Server.getHttpBaseDataURI() + "/.subs/" + record.name);
            Data clone = target.makeDeepCopy();
            clone.set(VIA,Path.toURI(target));
            //Adding $updated was nonstandard, so removed:  clone.getOrCreate(UPDATED).setValue(new GregorianCalendar());
            wrapper.post(clone);
            //
            // even though we have a safe sessionless clone...
            // just to be clear to future code maintainers, rather than hang on to the cloned Data,
            // we do the marshaling here and just queue up the body as a string for use in another thread.
            //
            // we make a marshalling context that will include the value-related metadata, plus $subscription, $via and $base
            //
            Context context = new Context("queueCOVNotification()");
            StringSet metadataFilter = new StringSet(Rules.valueMetadata);
            metadataFilter.add(SUBSCRIPTION);
            metadataFilter.add(VIA);
            metadataFilter.add(BASE);
            context.setMetadataFilter(metadataFilter);
            wrapper.setContext(context);

            CallbackInfo info = new CallbackInfo(); // we will queue up everything we need in here and return

            Writer writer = new StringWriter();
            if (record.callback.contains("?alt=xml") || record.callback.contains("&alt=xml")) {
                XMLGenerator xml = new XMLGenerator();
                xml.generate(writer, wrapper);
                info.contentType = "application/xml";
            } else {
                JSONGenerator json = new JSONGenerator();
                json.generate(writer, wrapper);
                info.contentType = "application/json";
            }
            info.record = record;
            info.body = writer.toString();
            queue.offer(info);  // we'll pick this up later
        }
        catch (XDException e) { record.callbackError = e.getLocalizedMessage(); } // shouldn't happen with all this internal stuff
    }
}
