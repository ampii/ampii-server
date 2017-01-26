// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.database;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.Log;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Context;
import org.ampii.xd.resolver.Eval;
import org.ampii.xd.resolver.Path;

/**
 * Sessions are designed to provide commit/discard to shadows of data in the datastore. Operations on non-shadows take
 * place immediately but operations on shadows is deferred until commit() is called. You don't need a session with new
 * non-shadow data because that data is free floating and not related to anything in the datastore, so "commits" happen
 * immediately and a "discard" amounts to just throwing the data away.  But for data stored already in the datastore,
 * you should always use shadows and a session so that changes can be discarded if anything goes wrong after partial
 * modification has begun.
 * <p>
 * When ready to finally store the data, you call {@link Session#commit} on the session and it is distributed down from
 * there to all data using {@link Data#commit} on each data item. You should NOT call {@link Data#commit}  directly.
 * If a mistake occurs and you want to throw aways partial changes, just call {@link Session#discard} on the session
 * and it will trickle down to {@link Data#discard} in each item below.  It is harmless to call {@link Session#discard}
 * after calling {@link Session#commit} so you can put it in a 'finally' block.  Bindings can hook into commit() and
 * discard() to manage external resources.
 * <p>
 * You can't use sessions without at least one shadow as the root of the session, but you can <i>add</i> "new" non-shadow data
 * below that of course (how else could you add anything to the datastore?)
 * <p>
 * You call {@link Data#setSession} on a parentless shadow to make it the "session root". You can't merge sessions - a
 * session root cannot be added as a sub to another data item. This is not a problem since the session root is almost
 * always a shadow of the datastore root. But that is not technically necessary and it may be academically possible to
 * use the commit/discard mechanism on a shadow of a free floating data item as the "original". However, there are no
 * use cases for this and therefore no example code in the core AMPII code.
 * <p>
 * The default commit() behavior for a shadow is to copy changes back to the 'original'.
 * <p>
 * The relationship of a Session to shadows and originals is shown below:
 * <p><pre><code>{@code
 *  +------------------+
 *  |mySession:Session |<-----+        [shadow]          +---------------------------+        [new]
 *  +------------------+      |     +--------------+     |                           |    +--------------+
 *  |              root|----------->|..root:Data   |<----+   +--------------------------->|bar:Data      |
 *  |isWriteSession    |      |     +--------------+     |   |                       |    +--------------+
 *  +------------------+      +-----|session       |     |   |       [shadow]        |    |session (null)|
 *                                  |parent (null) |     |   |   +--------------+    +----|parent        |
 *                                  |        subs[]|=========+-->|foo:Data      |         |subs[] (null) |
 *           +----------------------|original      |     |       +--------------+         |original(null)|
 *           |                      +--------------+     |       |session (null)|         +--------------+
 *           |                                           +-------|parent        |
 *           |                                                   |subs[] (null) |
 *           |                             +---------------------|original      |
 *           v  [datastore]                |                     +--------------+
 *   +---------------+                     |
 *   |..root:Data    |<----+               |
 *   +---------------+     |               |
 *   |session (null) |     |               v  [datastore]
 *   |parent (null)  |     |       +---------------+
 *   |         subs[]|------------>|foo:Data       |
 *   |original (null)|     |       +---------------+
 *   +---------------+     |       |session (null) |
 *                         +-------|parent         |
 *                                 |subs[] (null)  |
 *                                 |original (null)|
 *                                 +---------------+
 * }</code></pre><p>
 * The session root is also the home of the Context information, which contains the client's authorization and query
 * parameters that are usually provided as part of an HTTP operation.  Internal operations are given a dummy context
 * that has full authorization and no filtering. The context typically contains information like filter/select/range/skip
 * that only apply to the end "target" of the operation. However, the context is important at all levels of the path
 * evaluation because it also contains authorization information that controls whether the path can be evaluated at
 * all for the given user.
 * <p>
 * Since the session root doesn't have to be a shadow of the datastore root, it might be tempting to evaluate a path
 * from the root directly on the datastore root and then only get a shadow of the "target" of operations and set that
 * target as the session root. Don't do it! Some of the nodes along the path might have bindings and bindings only
 * work correctly in shadows, so it is quite possible that a valid path evaluation will fail without using shadows
 * from the start. So unless you really know what you're doing, you should always be using a session, starting with
 * a shadow of the datastore root. We prevent this mistake by requiring that data used for the session root must be
 * parentless and by renaming Datastore.getRoot() to getSystemRootIHopeYouKnowWhatYouAreDoing().
 * <p>
 * Sessions are either "read sessions" or "write sessions", subject to these limitations of the current code:
 * <ul>
 *   <li>only supports one write session at a time<ul>
 *       <li>so no worries about corruption by concurrent modifications to datastore</li>
 *       <li>nested write sessions need to use the "sub-session" mechanism and a "trust me these are independent actions"</li>
 *   </ul></li>
 *   <li>supports multiple read sessions:<ul>
 *       <li>but if an intervening write session modifies the underlying data being read, the results are unpredictable</li>
 *   </ul></li>
 * </ul><p>
 * Sub-sessions are available for situations where you need to make multiple unrelated and independent
 * try/commit/discard operations, e.g., a POST handler makes a write session and calls prepost() which wants to make
 * its own smaller nested write sessions each with its own possibility of error and need for discard()... see
 * MultiManager for an example because a /.multi write operation consists of multiple independent changes where some
 * can succeed and others fail.
 * <p>
 * For sub-sessions, you can make write-under-write and read-under-write but not write-under-read. And you can only do
 * write-under-write one level deep.
 * <p>
 * Typical Session usage pattern:
 * <p><pre><code>{@code
 *         Session session = Session.makeWriteSession("Description",context);
 *         try {
 *             Data target = Eval.eval(session.getRoot(),"some data path");
 *             -- modify target --
 *             session.commit(); // everything went will, save the data
 *         }
 *         catch (XDException e)  { ... }  // do something, rethrow, etc.
 *         finally { session.discard(); }  // yaou can call discard() with or without a previous commit()
 * }</code></pre><p>
 * Typical Sub-session usage pattern:
 * <p><pre>{@code
 *      foo() {
 *         Session session = Session.makeWriteSession("Description",context);
 *         try {
 *             Data target = Eval.eval(session.getRoot(),"some data path");
 *             bar(target);  // call some helper function that needs to make its own session
 *             session.commit();
 *         }
 *         catch (XDException e)  { ... }  // do something, rethrow, etc.
 *         finally { session.discard(); }  // you can call discard() with or without a previous commit()
 *      }
 *
 *      bar(target) {
 *         Session subsession = target.getSession().makeWriteSubsession("OtherDescription"); // inherits context
 *         try {
 *             --- do something with the subsession ---
 *             subsession.commit();  // everything went will, save the data
 *         }
 *         catch (XDException e)  { ... }  // do something, rethrow, etc.
 *         finally { session.discard(); }  // you can call discard() with or without a previous commit()
 *      }
 * }</pre><p>
 * In addition to all that, there are "Atomic" methods so that simple callers do not need to create all that
 * try/catch/finally logic. HOWEVER, note that the atomic put and post methods STILL need to create a write session,
 * so they can't be called within an active write session (until we implement multiple write sessions).
 *
 * @author drobin
 */
public class Session {

    private String  name = "internal";
    private Data    root;
    private boolean isWriteSession;
    private static Session activeWriteSession = null; // in the core AMPII code, there is only one active write session at a time (but it can has subsessions)

    private Session(String name, Data root, boolean isWriteSession) {
        this.name = name;
        this.root = root;
        root.setSession(this);
        this.isWriteSession = isWriteSession;
    }

    public String  getName()                    { return name; }
    public boolean isWriteSession()             { return isWriteSession; }
    public Data    getRoot()                    { return root; }

    public void    commit() throws XDException  {
        root.commit();
        if (isWriteSession) close();
    }

    public void    discard() {
        root.discard();
        if (isWriteSession) close();
    }

    private void    close() { // called by commit() and discard() for write sessions
        if (activeWriteSession == this) {
            activeWriteSession = null;
            DataStore.release();
        }
    }

    public static Session  makeReadSession(String name)                  { return makeReadSession(name,new Context()); }

    public static Session  makeReadSession(String name, Context context) {
        Data root = DataStore.getSystemRootIHopeYouKnowWhatYouAreDoing().makeShadow();
        // even though readonly sessions do NOT allow committable changes, we need to make the shadow mutable
        // to allow local non-committed changes, possibly made by a preread().
        // so, we always make the root shadow mutable
        root.setIsImmutable(false);
        root.setContext(context);
        return new Session(name,root,false);
    }

    public Session  makeReadSubsession(String name) {
        return makeReadSession(name, root.getContext()); // make new read session with same context
    }

    public static Session    makeWriteSession(String name)   { return makeWriteSession(name, new Context()); }

    public static Session    makeWriteSession(String name, Context context) {
        // This code is single-sessioned for writing at this time... this would all change considerably for multiple write sessions
        // To ensure only one write session at a time, simply try to acquire exclusive access to the database (with a "wait" for other sessions to finish)
        boolean acquired = false;
        try { acquired = DataStore.tryAcquire(Application.acquireDatabaseTimeout); }
        catch (InterruptedException e) { }
        if (!acquired) {
            String reason = activeWriteSession != null?
                    "Session '" + name + "' couldn't acquire database lock while session '"+activeWriteSession.getName()+"' is still open" :
                    "Session '" + name + "' couldn't acquire database lock even though no write session is open (yikes!)";
            Log.logSevere(reason);
            throw new XDError(reason);
        }
        Data root = DataStore.getSystemRootIHopeYouKnowWhatYouAreDoing().makeShadow();
        root.setIsImmutable(false);
        root.setContext(context);
        Session session = new Session(name,root,true);
        activeWriteSession = session;
        return session;
    }

    public Session  makeWriteSubsession(String name)  {  // can only make a write subsession under an active write session
        if (!isWriteSession)            throw new XDError("trying to make write subsession under read session");
        if (activeWriteSession != this) throw new XDError("trying to make write subsession under non-current write session");
        Data newRoot = DataStore.getSystemRootIHopeYouKnowWhatYouAreDoing().makeShadow();
        newRoot.setIsImmutable(false);
        newRoot.setContext(root.getContext());
        return new Session(name,newRoot,true);
    }

    public static String  atomicGetString(String sessionName, String path, String defaultValue) {
        Session session = Session.makeReadSession(sessionName);
        try                   { return Eval.eval(session.getRoot(),path).stringValue(); }
        catch (XDException e) { return defaultValue; }
        finally               { session.discard(); }
    }

    // WARNING! atomicGetCopy(...) makes a *copy* so it can produce HUGE in-memory results if you're not careful where it's used!!!
    public static Data    atomicGetCopy(String sessionName, String path) throws XDException {
        Session session = Session.makeReadSession(sessionName);
        try {
            Data target = Eval.eval(session.getRoot(),path);
            Data result = target.makeDeepCopy();
            target.discard();
            return result;
        }
        finally { session.discard(); }
    }

    public static void   atomicPut(String sessionName, String path, Data given) throws XDException {
        Session session = Session.makeWriteSession(sessionName);
        try {
            Data target = Eval.eval(session.getRoot(), path);
            target.put(given);
            target.commit();
        }
        finally { session.discard(); }
    }

    public static String atomicPost(String sessionName, String path, Data given) throws XDException {
        Session session = Session.makeWriteSession(sessionName);
        try {
            Data target = Eval.eval(session.getRoot(),path);
            Data   resultData = target.post(given);
            String resultPath = Path.toPath(resultData);
            target.commit();
            return resultPath; // can't return the Data outside of this session,  but its name is safe to return.
        }
        finally { session.discard(); }
    }

    public static void   atomicDelete(String sessionName, String path) throws XDException {
        Session session = Session.makeWriteSession(sessionName);
        try {
            Data target = Eval.eval(session.getRoot(), path);
            if (target.hasParent()) target.getParent().delete(target.getName());
            else throw new XDException(Errors.CANNOT_DELETE,"Can't delete root node!");
            target.commit();
        }
        finally { session.discard(); }
    }

}
