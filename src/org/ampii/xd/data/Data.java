// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data;

import org.ampii.xd.application.Policy;
import org.ampii.xd.bindings.Binding;
import org.ampii.xd.common.StringSet;
import org.ampii.xd.common.XDException;
import org.ampii.xd.database.Session;
import java.util.Calendar;

/**
 * All data and metadata items implement this interface.
 * <p>
 * The core AMPII code has a single implementation of this interface called AbstractData. All base types are derived
 * from that. While it is theoretically possible for application-specific code to implement the Data interface
 * themselves, it would be much easier to use the binding mechanism to bind AbstractData derivatives to some
 * external/live data source.  Implementing the Data interface directly is possible of course, but the implied contract
 * of behavior is pretty extensive and all of that is already handed by AbstractData and it's derivatives.
 * See {@link Session} for a description of interactions with data using sessions and shadows.
 *
 * @author drobin
 */
public interface Data {

    String       getName();                   //
    void         setName(String name)  throws XDException; // shadows will throw because it will conflict with its original
    Base         getBase();                   //
    void         setBase(Base base);          // can only be used on polymorphic data like freshly parsed PolyData - throws Error otherwise
    boolean      isMetadata();                //
    boolean      isChild();                   //
    boolean      isPoly();                    // indicates polymorphic base type. i.e., you can call setBase();

    boolean      hasParent();                 // useful to test if getParent() will return null
    Data         getParent();                 // returns null if parentless
    void         setParent(Data parent);      // assigns parent pointer but DOES NOT make this an official child of that parent, use addLocal() pr post() to do that.
    String       getPath();                   // gets path as a single string

    boolean      canHaveChildren();
    DataList     getChildren()       throws XDException; // returns children, possibly filtered by context. modifying the returned list does not add or remove children
    DataList     getLocalChildren();                     // returns local children only (no bindings) with no filtering
    int          getCount()          throws XDException; // gets total count of available children (bindings can make this different from the count of local children for sparse lists)

    DataList     getMetadata()       throws XDException; // returns a separately created list of metadata, so modifying the list does not affect the actual metadata
    DataList     getLocalMetadata();                     // gets the current local metadata only (does not call bindings)

    boolean      canHaveValue();
    boolean      hasValue()                 throws XDException; // useful to test if getValue() will return null
    Object       getValue()                 throws XDException; // calls binding; will return null if doesn't (or can't) have a value; returned object is either immutable or a copy
    void         setValue(Object value)     throws XDException; // will make a copy of any mutable thing given to it
    Object       getLocalValue();                               // does not call binding; will return null if doesn't (or can't) have a value; returned object is either immutable or a copy
    void         setLocalValue(Object value);                   // only for internal ops or bindings - will set value without validation or marking dirty (so it won't be committed)

    Data         find(String name)          throws XDException; // calls binding; returns null if not found;
    Data         find(String name, Data defaultValue);          // calls binding; returns default if not found;
    Data         findLocal(String name);                        // finds local items only (no binding); returns null if not found;
    Data         findEffective(String name) throws XDException; // magical method: finds effective value - either from prototype or up parent chain, as appropriate;
    Data         findEffective(String name, Data defaultValue); // magical method: finds effective value - either from prototype or up parent chain, as appropriate;

    Data         get(String name)           throws XDException; // calls find(name) then throws if not found with given Authorization
    Data         getLocal(String name)      throws XDException; // calls findLocal(name) then throws if not found
    Data         getOrCreate(String name)   throws XDException; // tries to create from prototype
    Data         getOrCreate(String name, String type, Base base) throws XDException; // useful if no prototype for created item; type and/or base can be null
    void         set(String name, Object value) throws XDException; // convenient shorthand for getOrCreate(name).setValue(value); throws if not possible

    Data         createChild(String name, String type, Base base)    throws XDException;  // creates child or throws reason
    Data         createMetadata(String name, String type, Base base) throws XDException;  // creates metadata item or throws reason

    // the following flags can be or'ed together for the 'options' argument to put()
    int          PUT_OPTION_USE_CLIENT_RULES = 0x00000001;  // modify put to use "client rules" i.e., retain things like "$partial"
    int          PUT_OPTION_NO_TYPE_CHECK    = 0x00000002;  // hope you know what you're doing
    int          PUT_OPTION_GIVEN_PARTIAL    = 0x00000004;  // pretend $partial is present
    int          PUT_OPTION_FORCE_WRITE      = 0x00000008;  // play god with unwritable things, like definitions
    int          PUT_OPTION_USE_POST_RULES   = 0x00000010;  // this put is part of a post (e.g. allow $subscriptions)

    Data         put(Data given)               throws XDException;  // puts given data item on top of this; calls binding; follows rules
    Data         put(Data given, int options)  throws XDException;  // puts given data item on top of this; calls binding; follows rules modified by 'options'
    void         validateConsistency()         throws XDException;  // called after put(), checks for consistency between value and metadata items, throws if problem

    Data         post(Data given)              throws XDException;  // adds given data item as child of this; calls binding

    void         delete(String name)           throws XDException;  // removes item from this; calls binding; follows rules
    void         deleteChildren()              throws XDException;  // removes all children; calls binding; follows rules

    void         addLocal(Data data);           // does not call binding, validate, rename, or mark dirty; use getOrCreate() or post() to commit in a Session
    void         removeLocal(Data data);        // does not call binding or mark dirty; use delete() to commit in a Session
    void         removeLocal(String name);      // does not call binding or mark dirty; use delete() to commit in a Session
    void         removeLocalChildren();         // does not call binding or mark dirty; use deleteChildren() to commit in a Session

    String       getEffectiveType()      throws XDException;  // returns "" for no effective type (i.e., no defined type, just a base type)
    Data         getPrototype();                              // no null; will always return something, even if it's a builtin
    Data         getEffectivePrototype() throws XDException;  // no null; will always return something, even if it's a builtin
    void         setPrototype(Data prototype);                // not for you! should only be used by Instances and AbstractData for make/copy/commit

    // Session-oriented methods.  See Session.java for explanation
    boolean      hasSession();                 // is under a session node, and therefore committable/discardable
    Session      getSession();                 // see Session class
    void         setSession(Session session);  // not for you! only called by Session class when making a session node. will throw error otherwise
    boolean      isShadow();                   // changes to shadows can be discarded, but if isShadow is false, there is no "original" and changes are immediate
    Data         makeShadow();                 // makes a comittable/discardable shadow wrapping an "original", usually used by sessions
    void         setOriginal(Data original);   // not for you! should only be used by makeShadow()
    void         commit() throws XDException;  // commits shadow data to original
    void         discard();                    // discards shadow data and notifies bindings to release external resources

    Context      getContext();                 // gets context or makes one up if it doesn't have one
    void         setContext(Context context);  // sets context for authorization and serialization (usually set by Session on the session root)
    DataList     getContextualizedChildren() throws XDException; // gets filtered/selected/skipped/maxxed list of children
    DataList     getContextualizedMetadata() throws XDException; // gets filtered list of metadata
    String       getContextualizedValue()    throws XDException; // gets possibly-partial value based on skip/max

    Binding      findBinding() throws XDException ;       // returns null if there is no binding for this item
    void         setBinding(Binding binding);
    Policy       getPolicy() throws XDException;          // gets policy from binding or will return default policy (which can be extended with hooks)

    Data         makeDeepCopy()      throws XDException;  // makes a parentless deep clone of this item // TODO analyze usage to prevent accidentally huge copies

    // flags in 0x00000fff range are persisted by setFlags()
    int FLAG_PERSIST_MASK        = 0x00000fff;
    int FLAG_DEFINITION          = 0x00000001;
    int FLAG_IMMUTABLE           = 0x00000002;
    int FLAG_PROTOTYPE           = 0x00000004;
    int FLAG_BUILTIN             = 0x00000008;
    int FLAG_FROM_ANY            = 0x00000010;
    int FLAG_FROM_NOTHING        = 0x00000020;
    int FLAG_LOCALIZABLE         = 0x00000040;
    int FLAG_ROOTED              = 0x00000080;

    // flags in 0xfffff000 range are NOT persisted by setFlags()
    int FLAG_DIRTY               = 0x00001000;
    int FLAG_DIRTY_BELOW         = 0x00002000;
    int FLAG_DELETED             = 0x00004000;
    int FLAG_PREREAD_DONE        = 0x00008000;

    // the following flags are for private use by bindings (their meaning is defined by the binding)
    int FLAG_BINDING_1           = 0x10000000;
    int FLAG_BINDING_2           = 0x20000000;
    int FLAG_BINDING_3           = 0x40000000;
    int FLAG_BINDING_4           = 0x80000000;

    int        getFlags();                     // there is no general setFlags() since that's too dangerous
    void       setPersistentFlags(int flags);  // flags not in FLAG_PERSIST_MASK are not set by setPersistentFlags()

    boolean    getFlag(int flag);
    void       setFlag(int flag, boolean state);
    void       setFlag(int flag);
    void       clearFlag(int flag);

    boolean    isDefinition();
    void       setIsDefinition(boolean state);
    boolean    isPrototype();
    void       setIsPrototype(boolean state);
    boolean    isImmutable();
    void       setIsImmutable(boolean state);
    boolean    isBuiltin();
    void       setIsBuiltin(boolean state);
    boolean    isRooted();
    void       setIsRooted(boolean state);
    boolean    isFromAny();
    void       setIsFromAny(boolean state);
    boolean    isFromNothing();
    void       setIsFromNothing(boolean state);
    boolean    isLocalizable();
    void       setIsLocalizable(boolean state);
    boolean    isDirty();
    void       setIsDirty(boolean state);
    boolean    isDirtyBelow();
    void       setIsDirtyBelow(boolean state);
    boolean    isDeleted();
    void       setIsDeleted(boolean state);

    // computed from metadata or other flags, so no setters for these
    boolean    isOptional()    throws XDException;
    boolean    isVisible()     throws XDException;
    boolean    isReadable()    throws XDException;
    boolean    isWritable()    throws XDException;
    boolean    isCommandable() throws XDException;
    boolean    isInstance();

    // convenience casters to cast getValue() to a friendly data type
    String     stringValue()              throws XDException;
    boolean    booleanValue()             throws XDException;
    int        intValue()                 throws XDException;
    long       longValue()                throws XDException;
    float      floatValue()               throws XDException;
    double     doubleValue()              throws XDException;
    byte[]     byteArrayValue()           throws XDException;
    StringSet  stringSetValue()           throws XDException;
    Calendar   calendarValue()            throws XDException;
    
    String     stringValue(String defaultValue);
    //String     stringValueForLocale(String locale, String defaultValue);
    boolean    booleanValue(boolean defaultValue);
    int        intValue(int defaultValue);
    long       longValue(long defaultValue);
    float      floatValue(float defaultValue);
    double     doubleValue(double defaultValue);
    byte[]     byteArrayValue(byte[] defaultValue);
    StringSet  stringSetValue(StringSet defaultValue);
    Calendar   calendarValue(Calendar defaultValue);

    // convenience find-then-cast, with default if not found
    String     stringValueOf(String name, String defaultValue); 
    //String     stringValueForLocaleOf(String name, String locale, String defaultValue);
    boolean    booleanValueOf(String name, boolean defaultValue); 
    int        intValueOf(String name, int defaultValue); 
    long       longValueOf(String name, long defaultValue); 
    float      floatValueOf(String name, float defaultValue); 
    double     doubleValueOf(String name, double defaultValue);
    byte[]     byteArrayValueOf(String name, byte[] defaultValue);
    StringSet  stringSetValueOf(String name, StringSet defaultValue);
    Calendar   calendarValueOf(String name, Calendar defaultValue);


    // convenience findEffective-then-cast, with default if not found
    String     effectiveStringValueOf(String name, String defaultValue); 
    boolean    effectiveBooleanValueOf(String name, boolean defaultValue); 
    int        effectiveIntValueOf(String name, int defaultValue); 
    long       effectiveLongValueOf(String name, long defaultValue); 
    float      effectiveFloatValueOf(String name, float defaultValue); 
    double     effectiveDoubleValueOf(String name, double defaultValue); 
    byte[]     effectiveByteArrayValueOf(String name, byte[] defaultValue); 
    StringSet  effectiveStringSetValueOf(String name, StringSet defaultValue);
    Calendar   effectiveCalendarValueOf(String name, Calendar defaultValue);

    // and finally... the most important method!; makes human readable string for debugging
    String     toString();
    String     toString(boolean showValue); // never call plain toString() in error messages, you could expose unauthorized/unreadable values!

}
