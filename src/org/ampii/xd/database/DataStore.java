// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.database;

import org.ampii.xd.application.Application;
import org.ampii.xd.application.Policy;
import org.ampii.xd.bindings.Binding;
import org.ampii.xd.bindings.DefaultBinding;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.CollectionData;
import org.ampii.xd.definitions.Builtins;
import org.ampii.xd.definitions.Definitions;
import org.ampii.xd.marshallers.DataParser;
import org.ampii.xd.data.abstractions.AbstractData;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 *  This is a local database made out of in-memory {@link Data} items, i.e., instances of {@link AbstractData} derivatives.
 *  <p>
 *  Native java class storage, or actual "database" storage, can be accomplished using bindings, e.g., {@link Binding#preread} and
 *  {@link Binding#commit} to move data to/from in-memory AbstractData items.
 *  <p>
 *  Even in an implementation using mostly "native" data, this DataStore will likely remain the home of all type
 *  definitions and prototypes.
 *  <p>
 *  At this time, this DataStore is constructed by Application class at startup by reading config files and is not
 *  persisted.
 *  <p>
 *  See {@link Session} for a description of interactions with data in this datastore using sessions and shadows.
 *
 *  @author daverobin
 */
public class DataStore {

    static private Data      root;
    static private Semaphore lock;
    static private Locale    locale;
    static private long      revision=1;

    public static Data initialize(Locale locale, String configFile) throws XDException {
        Data oldRoot = root;
        DataStore.locale = locale;
        DataStore.lock = new Semaphore(1);
        Builtins.initialize();
        DataStore.root = new CollectionData(Application.rootName);
        DataStore.root.setIsRooted(true);    // root is most definitely rooted :-)
        DataStore.root.setIsImmutable(true); // the datastore is not changeable without a Session (Session clears this flag in its root shadow)
        if (configFile != null && !configFile.isEmpty()) consumeFile(new File(Application.baseDir + File.separatorChar + configFile));
        return oldRoot; // yes this returns the entire old DataStore to you so you can restore it later with setSystemRootIHopeYouKnowWhatYouAreDoing()
    }

    public static void setSystemRootIHopeYouKnowWhatYouAreDoing(Data root) { DataStore.root = root; }  // no sesions had better be active!
    public static Data getSystemRootIHopeYouKnowWhatYouAreDoing()          { return root; }            // allows sessionless manipulation, be careful!

    static boolean  tryAcquire(int millis)             throws InterruptedException { return lock.tryAcquire(millis, TimeUnit.MILLISECONDS); }
    static void     release()                          { lock.release(); }

    public static Locale getDatabaseLocale()  {
        if (locale == null) throw new Error("Internal error: getDatabaseLocale() called on uninitialized database");
        return locale;
    }

    public static long   getRevision()              { return revision; }

    public static void   setRevision(long revision) { DataStore.revision = revision; }

    private static Binding theRevisionBinding = new DefaultBinding() {
        @Override public void preread(Data data)  { data.setLocalValue(revision); } // called upon access of /.data/database-revision
    };

    public static Binding getRevisionBinding()      { return theRevisionBinding; }

    public static Policy getPolicy()               { return DataStorePolicy.getThePolicy(); }

    public static String getDatabaseLocaleString()  {  return getDatabaseLocale().toLanguageTag();  }

    public static void consumeFile(File file)  throws XDException {
        try {
            // if we got a <CSML> wrapper, then it can contain multiple elements, else it's  just a single item
            Session session = Session.makeWriteSession("DataStore.consumeFile");
            try {
                Data incoming = DataParser.parse(file, Definitions.getSystemDefinitionCollector());
                if (incoming.getName().equals(".csml")) for (Data child : incoming.getChildren()) session.getRoot().post(child);
                else session.getRoot().post(incoming);
                session.commit();
            }
            finally {
                session.discard();
            }
        }
        catch(XDException e) { throw e.add("Error reading file '" + file.getName() + "'. "); }
    }

    public static int checkConsistency() throws XDException {
        return _checkConsistencyRecurse(DataStore.getSystemRootIHopeYouKnowWhatYouAreDoing());
    }
    private static int _checkConsistencyRecurse(Data data) throws XDException {
        int numberOfItemsChecked = 1;
        if (!data.isBuiltin() && data.getPrototype() == null) throw new XDError(data,"Non-builtin data has no prototype");
        if (data.getBase() == Base.INVALID) throw new XDError(data,"Base type is INVALID");
        if (data.getBase() == Base.POLY)    throw new XDError(data, "Base type is POLY");
        for (Data child : data.getChildren()) numberOfItemsChecked += _checkConsistencyRecurse(child);
        for (Data meta  : data.getMetadata()) numberOfItemsChecked += _checkConsistencyRecurse(meta);
        return numberOfItemsChecked;
    }

}
