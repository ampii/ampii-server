// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Base;
import org.ampii.xd.data.abstractions.AbstractData;
import org.ampii.xd.definitions.Builtins;

/**
 * This is the "placeholder" base type Any.  It's used by definitions to indicate that the actual type will be added at
 * runtime and is changeable. It is not suitable for instances except in rare exceptions like /.multi.
 *
 * @author drobin
 */
public class AnyData extends AbstractData /* implements Data - NOT! see note below */ {

    public AnyData(String name, Object... initializers) throws XDException {
        super(name, initializers);
        prototype = Builtins.getPrototypeOfBase(Base.ANY);
    }

    @Override public Base getBase() { return Base.ANY; }

    /*
    ///////////////// changeable base types with a delegate... DON'T DO IT (it's been tried... twice!) //////////////
    // AnyData as a wrapper for a delegate is fraught with problems: set/getParent() is just the first of many, especially
    // when the delegate calls into itself rather than through this "front door". And making all data "aware" that it is a
    // delegate is a messy been-there road to go down.
    // But it's tempting to try to change base types "in situ" so that pointers to the object do not get invalidated. But
    // to do that reasonably, it would be better to have a single "everybase" DataImpl class rather than individual classes
    // like StringData.  So, instead of trying to change base types in existing data, the put() and post() methods simply
    // delete and recreate as needed to change to a different class. This does orphan existing pointers to the data that
    // got replaced, but the alternative of always using a delegate is a storage and performance problem that is not worth
    // letting the rare Any-tail wag this whole dog.

    Data delegate;

    public AnyData(String name) throws XDException { delegate = DataFactory.make(Base.UNDEFINED,name); }

    @Override public String getName() { return delegate.getName(); }

    @Override public void setName(String name) { delegate.setName(name); }

    @Override public Base getBase() { return delegate.getBase(); }

    @Override public void setBase(Base base) throws XDException {
        // if changing base types, start over with a shiny new delegate
        // if we are *set* to ANY, make an UNDEFINED delegate as a placeholder (making an ANY inside an ANY is definitely not a good idea)
        if (delegate.getBase() != base) delegate = DataFactory.make(base==Base.ANY?base.UNDEFINED:base,delegate.getName());
    }

    @Override public boolean hasParent() { return delegate.hasParent(); }

    @Override public Data getParent() { return delegate.getParent(); }

    @Override public void setParent(Data parent) { delegate.setParent(parent); }

    @Override public Data makeDeepUnassignedCopy() throws XDException { return delegate.makeDeepUnassignedCopy(); }

    @Override public Data makeShadowCopy() throws XDException { return delegate.makeShadowCopy(); }

    @Override public void copyFrom(Data source) throws XDException { delegate.copyFrom(source); }

    @Override public boolean canHaveChildren() { return delegate.canHaveChildren(); }

    @Override public DataList getChildren() { return delegate.getChildren(); }

    @Override public DataList takeChildren() throws XDException { return delegate.takeChildren(); }

    @Override public void setChildren(DataList newChildren) throws XDException { delegate.setChildren(newChildren); }

    @Override public DataList getMetadata() { return delegate.getMetadata(); }

    @Override public DataList takeMetadata() throws XDException { return delegate.takeMetadata(); }

    @Override public void setMetadata(DataList newMetadata) throws XDException { delegate.setMetadata(newMetadata); }

    @Override public boolean canHaveValue() { return delegate.canHaveValue(); }

    @Override public boolean hasValue() { return delegate.hasValue(); }

    @Override public Object getValue() { return delegate.getValue(); }

    @Override public void setValue(Object value) throws XDException { delegate.setValue(value); }

    @Override public void destroy() { delegate.destroy(); }

    @Override public Data getPrototype() { return delegate.getPrototype(); }

    @Override public void setPrototype(Data prototype) { delegate.setPrototype(prototype); }

    @Override public Data find(String name) { return delegate.find(name); }

    @Override public Data findChild(String name) { return delegate.findChild(name); }

    @Override public Data findMetadata(String name) { return delegate.findMetadata(name); }

    @Override public Data findEffective(String name) { return delegate.findEffective(name); }

    @Override public Data get(String name) throws XDException { return delegate.get(name); }

    @Override public Data getOrNil(String name) { return delegate.getOrNil(name); }

    @Override public Data getOr(String name, Data defaultResult) { return delegate.getOr(name, defaultResult); }

    @Override public Data getEffectiveOrNil(String name) { return delegate.getEffectiveOrNil(name); }

    @Override public Data getOrCreate(String name) throws XDException { return delegate.getOrCreate(name); }

    @Override public Data getOrCreate(String name, String type) throws XDException { return delegate.getOrCreate(name,type); }

    @Override public Data getOrCreate(String name, Base base) throws XDException { return delegate.getOrCreate(name, base); }

    @Override public Data getOrCreate(String name, String type, Base base) throws XDException { return delegate.getOrCreate(name, type, base); }

    @Override public void add(Data sub) { delegate.add(sub); }

    @Override public void addChild(Data child) { delegate.addChild(child); }

    @Override public void addMetadata(Data meta) { delegate.addMetadata(meta); }

    @Override public void remove(Data sub) { delegate.remove(sub); }

    @Override public void remove(String name) { delegate.remove(name); }

    @Override public void removeChild(Data child) { delegate.removeChild(child); }

    @Override public void removeMetadata(Data meta) { delegate.removeMetadata(meta); }

    @Override public void postwrite() throws XDException { delegate.postwrite(); }

    @Override public void preread(Context context) throws XDException { delegate.preread(context); }

    @Override public Data prepost(Data data) throws XDException { return delegate.prepost(data); }

    @Override public boolean isNil() { return delegate==null; }

    @Override public boolean isMetadata() { return delegate.isMetadata(); }

    @Override public boolean isOptional() { return delegate.isOptional(); }

    @Override public boolean isVisible() { return delegate.isVisible(); }

    @Override public boolean isReadable() { return delegate.isReadable(); }

    @Override public boolean isWritable() { return delegate.isWritable(); }

    @Override public boolean isDefinition() { return delegate.isDefinition(); }

    @Override public boolean isPrototype() { return delegate.isPrototype(); }

    @Override public boolean isImmutable() { return delegate.isImmutable(); }

    @Override public boolean isInstance() { return delegate.isInstance(); }

    @Override public boolean isBuiltin() { return delegate.isBuiltin(); }

    @Override public boolean isShadow() { return delegate.isShadow(); }

    @Override public boolean isFromAny() {
        return true; // !!
    }

    @Override public boolean isFakeSub() { return delegate.isFakeSub(); }

    @Override public boolean isLocalizable() { return delegate.isLocalizable(); }

    @Override public void setIsDefinition(boolean state) { delegate.setIsDefinition(state); }

    @Override public void setIsPrototype(boolean state) { delegate.setIsPrototype(state); }

    @Override public void setIsImmutable(boolean state) { delegate.setIsImmutable(state); }

    @Override public void setIsBuiltin(boolean state) { delegate.setIsBuiltin(state); }

    @Override public void setIsShadow(boolean state) { delegate.setIsShadow(state); }

    @Override public void setIsFromAny(boolean state) { delegate.setIsFromAny(state); }

    @Override public String stringValue() { return delegate.stringValue(); }

    @Override public String stringValue(String locale) { return delegate.stringValue(locale); }

    @Override public StringSet stringSetValue() { return delegate.stringSetValue(); }

    @Override public boolean booleanValue() { return delegate.booleanValue(); }

    @Override public long longValue() { return delegate.longValue(); }

    @Override public double doubleValue() { return delegate.doubleValue(); }

    @Override public byte[] byteArrayValue() { return delegate.byteArrayValue(); }

    @Override public int intValue() { return delegate.intValue(); }

    @Override public float floatValue() { return delegate.floatValue(); }

    @Override public String stringValueOf(String name, String defaultValue) { return delegate.stringValueOf(name,defaultValue); }

    @Override public StringSet stringSetValueOf(String name, StringSet defaultValue) { return delegate.stringSetValueOf(name,defaultValue); }

    @Override public boolean booleanValueOf(String name, boolean defaultValue) { return delegate.booleanValueOf(name,defaultValue); }

    @Override public long longValueOf(String name, long defaultValue) { return delegate.longValueOf(name,defaultValue); }

    @Override public double doubleValueOf(String name, double defaultValue) { return delegate.doubleValueOf(name,defaultValue); }

    @Override public int intValueOf(String name, int defaultValue) { return delegate.intValueOf(name,defaultValue); }

    @Override public float floatValueOf(String name, float defaultValue) { return delegate.floatValueOf(name,defaultValue); }

    @Override public byte[] byteArrayValueOf(String name, byte[] defaultValue) { return delegate.byteArrayValueOf(name,defaultValue); }

    @Override public String toString() { return delegate.toString(); }
    */
}
