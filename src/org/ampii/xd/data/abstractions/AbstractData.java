// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.abstractions;

import org.ampii.xd.application.ApplicationPolicy;
import org.ampii.xd.application.Bindings;
import org.ampii.xd.application.Policy;
import org.ampii.xd.bindings.Binding;
import org.ampii.xd.common.*;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.*;
import org.ampii.xd.database.Session;
import org.ampii.xd.definitions.Instances;
import org.ampii.xd.definitions.Prototypes;
import org.ampii.xd.data.Context;
import org.ampii.xd.resolver.Path;
import org.ampii.xd.security.Authorizer;
import org.ampii.xd.data.Meta;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 *  This provides all the behavior and data storage that is common to <i>all</i> base types.
 *  <p>
 *  It does most of the heavy lifting for all the basic data operations like get(), put(), post(), and delete().
 *  This includes all the rules, policies, and authorization checks.
 *  <p>
 *  It uses bindings to tie data items to live sources or external data storage.
 *  <p>
 *  Where needed, it defers basetype-specific actions, like addChild() or setValue(), and of course, getBase() to the
 *  derived classes.
 *
 *  @author drobin
 */
public abstract class AbstractData implements Data {


    protected String       name;           // general string name or one of the Meta.XXX constants for standard metadata
    protected Data         parent;         // null if parentless (i.e. root or simply free-floating data)
    protected Data         prototype;      // null if no prototype. in which case, getPrototype() will return a Builtin
    protected DataList     subs;           // metadata and/or children
    protected Session      session;        // only a session root will have this, so look up the chain to find it
    protected Context      context;        // generally only on the root of a tree of nodes, so look up the chain to find it
    protected Data         original;       // used when this is a shadow for a Data item in the database
    protected Binding      binding;        // used to bind to live/backend data
    private   int          flags = 0;      // contains FLAG_XXX bits, defined in the /// FLAGS /// section below

    public AbstractData(String name, Object... initializers) throws XDException {  // initializers are value, subs, or a List of subs
        this.name = name;
        for (Object initializer : initializers) {
            if (initializer instanceof Data) addLocal((Data)initializer);
            else if (initializer instanceof Binding) setBinding((Binding)initializer);
            else if (initializer instanceof List) {
                for (Object sub : (List)initializer) if (sub instanceof Data) addLocal((Data) sub);
            }
            else setValue(initializer);
        }
        totalCreatedItems++; // just for fun and/or performance metrics (helpful when you refactor something and it explodes!)
    }

    ////////////////////////////////////////////////////////////////////
    ///////////////////////// BASIC ACTIONS ////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public Base     getBase()              { throw new XDError(this,"getBase() called AbstractData!"); } // subclasses better override this!

    @Override public void     setBase(Base base)     { throw new XDError(this,"setBase() called on non-polymorphic data"); } // polymorphic subclass will override this

    @Override public boolean  isPoly()               { return false; }  // overridden by polymorphic types like POLY

    @Override public boolean  isMetadata()           { return Rules.isMetadata(this); }

    @Override public boolean  isChild()              { return !Rules.isMetadata(this); }

    @Override public String   getPath()              { return Path.toPath(this); }

    @Override public String   getName()              { return name; }

    @Override public void     setName(String name)   {
        if (isShadow()) throw new XDError("Can't rename a shadow (i.e., can't rename data once its been committed to the database)");
        this.name = name;
    }

    @Override public boolean  hasParent()            { return parent != null; }

    @Override public Data     getParent()            { return parent != null? parent : null; }

    @Override public void     setParent(Data newParent) {
        if (parent != newParent) {
            parent = newParent;
            context = null;  // we will inherit our context from our new parent
        }
    }

    @Override public Context  getContext() {
        if      (context != null) return context;
        else if (parent != null)  return parent.getContext();
        else                      return new Context();
    }

    @Override public void     setContext(Context context) {
        this.context = context;
    }

    ////////////////////////////////////////////////////////////////////
    ///////////////////// SHADOW (SESSION) ACTIONS /////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public boolean    hasSession()                    {
        return session != null || parent != null && parent.hasSession();
    }

    @Override public Session    getSession() {
        if (session != null) return session;
        if (hasParent()) return parent.getSession();
        else throw new XDError("getSession() called on sessionless data");
    }

    @Override public void       setSession(Session session)   {
        this.session = session;
    }

    @Override public Data     makeShadow()  {
        Data shadow = DataFactory.make(getBase(),getName());
        shadow.setPrototype(getPrototype());
        shadow.setPersistentFlags(getFlags());
        shadow.setOriginal(this);
        // Making a shadow DOES NOT automatically make shadows of all subs - that would defeat the point of a "shadow" because
        // otherwise making a shadow of the root would recursively replicate the entire database as shadows.
        // Therefore, at this point, this shadow is only a placeholder - it has no children/metadata/value shadows of its own.
        // If you try to touch it with find()/getChildren()/getValue()/etc., preread() will make shadows one more layer deep
        return shadow;
    }


    @Override public void setOriginal(Data original)  {  // this should only be called by makeShadow()
        if (original == this) throw new XDError("Can't set self as original",this);
        if (this.original != null) throw new XDError("Can't set original twice",this);
        this.original = original;
    }

    protected void markDirty() {
        if (hasSession()) {  // we only mark things that are in a session
            // set this node dirty, then run up the parent chain and mark all ancestor dirtyBelow so commit()'s descent will know to find its way to us
            setIsDirty(true);
            for (Data parent = this.parent; parent != null; parent = parent.getParent()) parent.setIsDirtyBelow(true);
        }
    }

    protected boolean commitValue() throws XDException { return false; } // overridden by String and OctetString to do value merging


    @Override public void     commit() throws XDException {
        // see usage note in Bindings interface.
        if (!isShadow()) throw new XDError("Attempting to commit() a non-shadow",this);
        if (getBase() == Base.POLY) throw new XDError("Attempting to commit() a POLY",this);
        if (isDirty()) {
            // first, give the binding a chance to handle it entirely
            Binding binding = findBinding();
            if (binding != null && binding.commit(this)) return; // if binding says it handled it completely, we have to trust it (nervously)
            // otherwise, do it ourselves
            // first, set things that are not subs or value
            original.setPersistentFlags(getFlags());
            original.setPrototype(prototype);
            // then set value
            if(canHaveValue()) {
                if (!commitValue()) {  // let subclasses have a crack at the value first (String and OctetString do merging)
                    Object value = getLocalValue();
                    if (value != null) original.setLocalValue(value);
                }
            }
            // delete, add new, or update existing subs
            if (subs != null) for (Data sub : subs) {
                if (sub.isShadow()) {
                    if (sub.isDeleted()) original.removeLocal(sub.getName());
                    else if (sub.isDirty() || sub.isDirtyBelow()) sub.commit(); // recurse...
                }
                else { // it's not a shadow... it's fresh data under a shadow, so that means it's new or a replacement
                    original.addLocal(sub); // addLocal() will replace original if it exists
                }
            }
            try {
                if (Rules.canRenumberChildren(original.getBase())) {  // if these are numbered positionally, then renumber after a delete
                    int i = 1;
                    for (Data child : original.getChildren()) child.setName(Integer.toString(i++));
                }
            } catch (XDException e) { throw new XDError("Error renaming positional children upon commit()",this,original); }
        }
        else if (isDirtyBelow()) {  // we're not dirty, but one or more of of our subs are
            if (subs != null) for (Data sub : subs) {
                if (sub.isDirty() || sub.isDirtyBelow()) sub.commit(); // recurse...
            }
        }
    }

    @Override public void     discard() {
        // see usage note in Bindings interface.
        if (!isShadow()) return;
        if (binding != null && binding.discard(this)) return;  // if binding says it handled it from here down, then we have to trust it and leave
        if (subs != null) for (Data sub : subs) sub.discard(); // otherwise we'll descend for more discards
    }

    ////////////////////////////////////////////////////////////////////
    ///////////////////////// BINDING ACTIONS //////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public Binding findBinding() throws XDException { preread(); return binding; }

    @Override public void    setBinding(Binding binding) { this.binding = binding; }

    protected  void preread() throws XDException {
        if (!getFlag(FLAG_PREREAD_DONE)) {
            setFlag(FLAG_PREREAD_DONE);
            if (isShadow()) {
                Data bindingMeta = original.find(Meta.AMPII_BINDING); // we turn the original's binding metadata into a 'binding' member variable when we preread a shadow
                if (bindingMeta != null) setBinding(Bindings.getBinding(this, bindingMeta.stringValue("<empty-binding-string>")));
                for (Data child : original.getChildren()) addLocal(child.makeShadow());
                for (Data meta : original.getMetadata())  addLocal(meta.makeShadow());
                if (original.hasValue()) setLocalValue(original.getLocalValue());
            }
            if (binding != null) binding.preread(this);
        }
    }

    @Override public DataList getContextualizedChildren() throws XDException {
        Binding binding = findBinding();
        if (binding != null) {
            DataList results = binding.getContextualizedChildren(this);
            if (results != null) return results;
        }
        return getContext().makeContextualizedChildren(this);
    }

    @Override public DataList getContextualizedMetadata() throws XDException {
        Binding binding = findBinding();
        if (binding != null) {
            DataList results = binding.getContextualizedMetadata(this);
            if (results != null) return results;
        }
        return getContext().makeContextualizedMetadata(this);
    }

    @Override public String getContextualizedValue() throws XDException {
        Binding binding = findBinding();
        if (binding != null) {
            String results = binding.getContextualizedValue(this);
            if (results != null) return results;
        }
        return getContext().makeContextualizedValue(this);
    }

    protected  Data prefind(String name) throws XDException {
        Binding binding = findBinding();
        if (binding != null) return binding.prefind(this, name); // the binding can either return it here (fabricated) or add it locally
        return null; // either the binding did nothing or it made it locally
    }

    @Override public int      getCount() throws XDException {
        preread();
        // we don't just count the locally present children because sparse lists can have a different number of "available" children in the backend
        Integer result = null;
        Binding binding = findBinding(); // so let the binding have a chance to override the default count
        if (binding != null) result = binding.getTotalCount();
        return result != null? result : getChildren().size();
    }



    ////////////////////////////////////////////////////////////////////
    /////////////////////////////  FIND  ///////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public Data   findLocal(String name)  {
        // only finds existing local items, no bindings, inheritance, etc.
        if (subs != null) for (Data sub : subs) if (sub.getName().equals(name)) return sub;
        return null;
    }

    @Override public Data   find(String name, Data defaultValue) {
        try { Data data = find(name); return data != null? data : defaultValue; } catch (XDException e) { return defaultValue; }
    }

    @Override public Data   find(String name) throws XDException {
        // the process:
        // call preread() - do the normal preread to try to make all items - sparse lists will likely make no children
        // if named item does not exist, call binding.prefind(name) to give binding a second chance to create it
        // if the binding returned a data item directly, return that (it might be "fabricated" and not stored locally)
        // finally, look for the local item again (the binding might have made it) and return if found
        // if none of that worked, return null
        // so, if the binding wants to be called repeatedly, it will *not* make the item present locally.
        // it should, however, call setParent() on the returned item to make findEffective() and getPath() work.
        preread();
        Data found = findLocal(name);
        if (found != null) return found;
        found = prefind(name);
        if (found != null) return found;
        return findLocal(name);
    }


    @Override public Data   findEffective(String name, Data defaultValue) {
        try { Data data = findEffective(name); return data != null? data : defaultValue; } catch (XDException e) { return defaultValue; }
    }

    @Override public Data   findEffective(String name) throws XDException {
        // find either local or "effective" (inherited by either type or tree) metadata
        Data found = find(name);               // check locally first
        if (found != null && (!found.canHaveValue() || found.hasValue())) return found; // only return things that exist AND have meaning
        if (Rules.inheritsFromDefinition(name)) {   // then check for type-inherited value
            // if you're from Any, then you have *two* definitions to check, one from your defined abstract effective type
            // (Where the "Any" was defined), and the other from your actual run-time  concrete type.
            // The effective type wins, so check it first.
            if (isFromAny()) {
                Data effectivePrototype = getEffectivePrototype();
                if (!effectivePrototype.isBuiltin()) found = effectivePrototype.findEffective(name);
                if (found != null && !found.isOptional() && (!found.canHaveValue() || found.hasValue())) return found;
                // if not found there, fall through to check your actual instance type
            }
            Data prototype = getPrototype();
            if (prototype != null) found = prototype.isBuiltin()? prototype.find(name) : prototype.findEffective(name);
            if (found != null && !found.isOptional() && (!found.canHaveValue() || found.hasValue())) return found;
            // in prototypes, all children are always present, but only metadata that is different from the default is present.
            // therefore, if we're looking for metadata, we'll use recursion to check up the prototype chain for the default value.
            if (Rules.isMetadata(name) && prototype != null && !prototype.isBuiltin()) found = prototype.findEffective(name); // recurse
            if (found != null && !found.isOptional() && (!found.canHaveValue() || found.hasValue())) return found;
        }
        if (Rules.inheritsFromParent(name)) {                       // finally, check for parent-inherited (up the tree)
            if (hasParent()) return getParent().findEffective(name);
        }
        return null;
    }


    ////////////////////////////////////////////////////////////////////
    ////////////////////////////// GET /////////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public Data       get(String name)                      throws XDException {
        Data found = find(name); if (found != null) return found;
        if (Rules.isMetadata(name)) throw new XDException(Errors.METADATA_NOT_FOUND, this, "Metadata '" + name + "' not found ");
        else                        throw new XDException(Errors.DATA_NOT_FOUND,     this, "Data '"     + name + "' not found");
    }

    @Override public Data       getLocal(String name)                      throws XDException {
        Data found = findLocal(name); if (found != null) return found;
        if (Rules.isMetadata(name)) throw new XDException(Errors.METADATA_NOT_FOUND, this, "Metadata '" + name + "' not found ");
        else                        throw new XDException(Errors.DATA_NOT_FOUND,     this, "Data '"     + name + "' not found");
    }

    @Override public Data       getOrCreate(String name)              throws XDException { return getOrCreate(name, null, null); }

    @Override public Data       getOrCreate(String name, String type, Base base) throws XDException { // type and/or base can be null
        Data found = find(name); if (found != null) return found;
        markDirty();
        if (Rules.isMetadata(name)) return createMetadata(name, type, base);
        else                        return createChild(name, type, base);
    }

    ////////////////////////////////////////////////////////////////////
    ////////////////////////////// CREATE  /////////////////////////////
    ////////////////////////////////////////////////////////////////////


    @Override public Policy getPolicy() throws XDException {
        // give binding first crack at it
        Binding binding = findBinding(); if (binding != null) return binding.getPolicy();
        // if that didn't work, then ask our parent
        else if (hasParent()) return getParent().getPolicy();
        // until we get to the root, then just ask for the system policy
        else return ApplicationPolicy.getPolicy();
    }

    @Override public  Data      createMetadata(String name, String type, Base base) throws XDException {
        if (isImmutable()) throw new XDError("Trying to modify an immutable",this);
        if (isInstance() && Rules.isNotAllowedInInstances(name)) throw new XDException(Errors.ILLEGAL_METADATA, this, "Can't create metadata " + name + " on an instance");
        Data meta;
        Data metaPrototype = getPrototype().find(name);  // if we find a metadata prototype in our prototype, then use that
        if (metaPrototype != null) meta = Instances.makeInstance(metaPrototype, name);
        else {                                    // if it was not found directly in prototype...
            if (Rules.isStandardMetadata(name) || Rules.isServerSpecificMetadata(name)) { // standard metadata that is not found in the prototype must be from the base type
                Data basePrototype = Prototypes.getPrototypeFor(getBase());
                metaPrototype = basePrototype.find(name); // if we find a metadata item in the base, then use that
                if (metaPrototype != null) meta = Instances.makeInstance(metaPrototype, name);
                else throw new XDException(Errors.ILLEGAL_METADATA, this, "Illegal metadata '" + name + "' for this base type");
            }
            else { // extended metadata needs to be allowed by policy, and needs a type name or base
                meta = Instances.makeInstance(type,base,name);
            }
        }
        // OK so it's legit for this base type and well formed, but is it allowed?
        if (!getPolicy().allowCreate(this, name, type, base)) throw new XDException(Errors.CANNOT_CREATE, this, "Not allowed to create metadata '" + name + "' here");
        markDirty();
        addLocal(meta);
        return meta;
    }

    @Override public Data       createChild(String name, String type, Base base) throws XDException {
        if (!getPolicy().allowCreate(this, name, type, base)) throw new XDException(Errors.CANNOT_CREATE, this, "Can't create child '" + name + "' here (denied by policy)");
        if (type != null && type.isEmpty()) type = null; // pointless to give a blank type name, we'll turn it into null
        Data child;
        try {
            if (isDefinition()) {
                // definitions have no type checking because they are not proper instances (they can contain forward type references)
                if (base == null) throw new XDException(Errors.INVALID_DATATYPE, this, "Missing $base for '" + name + "' in definition context");
                name = Rules.getNextAvailableChildName(this, name);
                child = DataFactory.make(base, name);
            } else {
                // we're not in a definition context, so let's make an instance of a real type from a real prototype, if possible
                Data myPrototype = getPrototype();
                switch (getBase()) {
                    case SEQUENCEOF:
                    case ARRAY:
                    case LIST:
                    case COLLECTION:
                    case UNKNOWN:
                        // These create new child from either: type name in $memberType, anonymous definition in $memberTypeDefintion, or the given type name or base
                        // SequenceOf, Array, List, and Unknown will number the child with the next available position.
                        name = Rules.getNextAvailableChildName(this, name);
                        // unlike $memberTypeDefinition, a simple $memberType is OK to be from either instance or prototype so OK to use stringValueOf
                        Data memberType = findEffective(Meta.MEMBERTYPE);
                        if (memberType != null) {  // if we have a $memberType...
                            String memberTypeName = memberType.stringValue();
                            if (memberTypeName.isEmpty()) throw new XDException(Errors.INVALID_DATATYPE, this, "Provided $memberType is empty string");
                            // if we were *also* given a type name, check that the given type is an extension of $memberType before using it
                            if (type != null) {
                                _validateCompatibleTypes(Prototypes.getPrototypeFor(memberTypeName), type); // validate that given $type is an extension of $memberType.
                                child = Instances.makeInstance(type, name);
                                if (memberTypeName.equals(type)) child.removeLocal(Meta.TYPE); // if declared type is the same as the defined type, it's redundant so don't store it
                            } else {
                                child = Instances.makeInstance(memberTypeName, name); // if we were not given a type name, then use $memberType as is
                                child.removeLocal(Meta.TYPE); // type is defined by context, so it's redundant so don't store it
                            }
                        } else { // $memberType was not found, so look for $memberTypeDefinition in our prototype (not in our instance, that's invalid)
                            Data memberTypeDefinition = myPrototype.find(Meta.MEMBERTYPEDEFINITION);
                            if (memberTypeDefinition != null && !memberTypeDefinition.isOptional()) {
                                String effectiveTypeName = memberTypeDefinition.get("1").getEffectiveType();
                                if (type != null && !type.equals(effectiveTypeName)) throw new XDException(Errors.INVALID_DATATYPE, this, "Provided $type '" + type + "' disagrees with $memberTypeDefinition '" + effectiveTypeName + "'");
                                child = Instances.makeInstance(memberTypeDefinition.get("1"), name); // the prototype is always at .../$memberTypeDefinition/1
                            } else { // neither $memberType nor $memberTypeDefinition present (equivalent to member type of "Any"), so just use provided type or base
                                if (base == Base.POLY) throw new XDException(Errors.CANNOT_CREATE,this, "Can't create child '"+name+"' without a specified or defined base type.");
                                child = Instances.makeInstance(type, base, name);
                                child.setIsFromNothing(true); // indicate that this instance was created without a definition
                            }
                        }
                        break;
                    case CHOICE:
                        Data choices = myPrototype.findEffective(Meta.CHOICES);// check the prototype's $choices, which is the merged collection of choices from the definition chain
                        if (!myPrototype.isBuiltin() && choices != null) {
                            Data childPrototype = choices.find(name);
                            if (childPrototype == null) throw new XDException(Errors.CANNOT_CREATE, this, "Data named '" + name + "' is not a valid choice");
                            child = Instances.makeInstance(childPrototype, name); // make fresh copy of it
                        } else {                             // my prototype has no choices?  it better be a builtin then!
                            if (!myPrototype.isBuiltin()) throw new XDException(Errors.CANNOT_CREATE, this, "Can't create '" + name + "' because type '" + myPrototype.getEffectiveType() + "' has no defined choices");
                            if (base == Base.POLY) throw new XDException(Errors.CANNOT_CREATE, this, "Can't create child '"+name+"' without a specified or defined base type.");
                            child = Instances.makeInstance(type, base, name);
                            child.setIsFromNothing(true); // indicate that this instance was created without a definition
                        }
                        for (Data existing : getChildren())
                            this.removeLocal(existing);// there should only be one child, but this will also clean up past sins :-)
                        break;
                    case COMPOSITION:
                    case OBJECT:
                    case SEQUENCE:
                        Data childPrototype = myPrototype.find(name); // Does our prototype have the named child?
                        if (childPrototype != null) {                 // Yes, we found the child in our prototype.
                            if (!childPrototype.isOptional())         // The child better be optional, or it would already be present!
                                throw new XDError(this, "Internal Error: non-optional item '" + name + "' found in prototype but not in instance?");
                            if (childPrototype.getBase() == Base.ANY) {  // if the definition is ANY, then instantiate it and mark it as isANy()
                                child = Instances.makeInstance(type, base, name);
                                child.setIsFromAny(true); // indicate that this instance was created from an ANY definition
                            } else
                                child = Instances.makeInstance(childPrototype, name); // make in instance of that subprototype
                        } else {                             // my prototype doesn't have this child?  it better be a builtin or ANY then!
                            if (!(myPrototype.isBuiltin() || myPrototype.getBase() == Base.ANY))
                                throw new XDException(Errors.CANNOT_CREATE, this, "Can't create '" + name + "' because type '" + getEffectiveType() + "' does not define that child");
                            if (base == Base.POLY) throw new XDException(Errors.CANNOT_CREATE, this, "Can't create child '"+name+"' without a specified or defined base type.");
                            child = Instances.makeInstance(type, base, name);
                            child.setIsFromNothing(true); // indicate that this instance was created without a definition
                        }
                        break;
                    default:
                        throw new XDException(Errors.CANNOT_CREATE, this, "Can't create/post new children under base type " + getBase()
                                + (name.equals("value") ? ". Attempted child name is \"value\"; did you mean \"$value\"?" : ""));
                }
            }
        } catch (XDException e) { throw e.add("createChild",this,name,type,base); }
        markDirty();
        addLocal(child);
        return child;
    }

    ////////////////////////////////////////////////////////////////////
    /////////////////////////////   PUT   //////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public Data       put(Data given)               throws XDException { return put(given, 0); }

    @Override public Data       put(Data given, int options)  throws XDException {
        preread();
        markDirty();
        // We have to use a "target" here rather than assume "this" throughout because when you put() to an Any instance, it could get *replaced* by
        // another instance of a different type. So the result of the put() may not be the same as the original target of the put()
        Data target = this; // of course we start out operating on ourselves
        try {
            if (target.isImmutable()) throw new XDError(target, given, "Oops! Trying to modify immutable data"); // not even "godmode" can modify this
            // writable? and authorized to do so?
            Authorizer authorizer = target.getContext().getAuthorizer();
            if (!target.isWritable() && !authorizer.inGodMode()) throw new XDException(Errors.NOT_WRITABLE, target, "Data is not writable");
            if (!authorizer.checkWrite(target)) throw new XDException(Errors.NOT_AUTHORIZED, target, "Data is not writable with given authorization");

            // compatible base types?
            Base givenBase = given.getBase();
            Base targetBase = target.getBase();
            // if we're not given a base for an ANY any target, we're in trouble
            if (givenBase == Base.POLY && targetBase == Base.ANY) throw new XDException(Errors.VALUE_FORMAT, target, given, "Failed to give a base type on data for a target of base type 'Any'");
            // if we *are* given a base and it's not right...
            if (    givenBase != Base.POLY &&
                    givenBase != targetBase &&
                    targetBase != Base.ANY &&
                    targetBase != Base.POLY &&
                    !(givenBase == Base.NULL && target.isCommandable()) &&
                    !target.isFromNothing() &&
                    !target.isFromAny()
                    ) throw new XDException(Errors.TARGET_DATATYPE, target, given, "Given base type " + givenBase + " is not compatible with target base type " + targetBase);
            // basic consistency checks...  checking only local presence for these at this point
            Data givenType = given.find(Meta.TYPE);
            Data givenExtends = given.find(Meta.EXTENDS);
            Data givenOverlays = given.find(Meta.OVERLAYS);
            Data targetType = target.find(Meta.TYPE);
            Data targetExtends = target.find(Meta.EXTENDS);
            Data targetOverlays = target.find(Meta.OVERLAYS);
            String givenTypeName = givenType == null ? null : givenType.stringValue();
            String givenExtendsName = givenExtends == null ? null : givenExtends.stringValue();
            String givenOverlaysName = givenOverlays == null ? null : givenOverlays.stringValue();
            String targetTypeName = targetType == null ? null : targetType.stringValue();
            String targetExtendsName = targetExtends == null ? null : targetExtends.stringValue();
            String targetOverlaysName = targetOverlays == null ? null : targetOverlays.stringValue();
            // check given against itself
            if (givenType != null && givenExtends != null) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given both $type and $extends");
            if (givenExtends != null && givenOverlays != null) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given both $extends and $overlays");
            if (givenOverlays != null && givenType != null) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given both $type and $overlays");
            if (givenTypeName != null && givenTypeName.isEmpty()) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $type name is empty");;
            if (givenExtendsName != null && givenExtendsName.isEmpty()) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $extends name is empty");;
            if (givenOverlaysName != null && givenOverlaysName.isEmpty()) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $overlays name is empty");;
            // check given against target
            if (givenType != null) {
                if (targetType != null && !targetTypeName.equals(givenTypeName)) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $type does not match target $type");
                if (targetExtends != null) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $type but target already has $extends");
                if (targetOverlays != null) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $type but target already has $overlays");
            }
            if (givenExtends != null) {
                if (targetType != null) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $extends but target already has $type");
                if (targetExtends != null && !targetExtendsName.equals(givenExtendsName)) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $extends does not match existing $extends");
                if (targetOverlays != null) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $type but target already has $overlays");
            }
            if (givenOverlays != null) {
                if (targetType != null) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $overlays but target already has $type");
                if (targetExtends != null) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $overlays but target already has $extends");
                if (targetOverlays != null && !targetOverlaysName.equals(givenOverlaysName)) throw new XDException(Errors.VALUE_FORMAT, target, given, "Given $overlays does not match target $overlays");
            }
            // check the given $type for compatibility
            // (do this only for instances, skip if building a definitions because those can contain forward type references!)
            if (givenTypeName != null && target.isInstance()) {
                // we are explicitly given a $type name. if the target also has an explicit type, are thy compatible?
                _validateCompatibleTypes(target, givenTypeName);
            }
            // now the tricky part, dealing with ANYs.
            if ( (target.isFromAny() || target.getBase() == Base.ANY) && (options&Data.PUT_OPTION_NO_TYPE_CHECK)==0)  {
                // if the target is a different base or type, it gets replaced
                String effectiveTargetTypeName = target.getEffectiveType(); // return "" for builtins
                String effectiveGivenTypeName  = givenTypeName!=null? givenTypeName : given.getEffectiveType(); // use same format so we can compare
                if ((givenBase != Base.POLY && givenBase != targetBase) || !effectiveGivenTypeName.equals(effectiveTargetTypeName)) {
                    //
                    // WE'RE CHANGING TYPES... so the old target gets *replaced* with a fresh new one
                    //
                    // OK, we can make a fresh new target and update it from given
                    Data parent = target.getParent();
                    if (parent!=null) parent.removeLocal(target); // out with the old...
                    target = (givenTypeName != null)? Instances.makeInstance(givenTypeName, target.getName()) : DataFactory.make(givenBase,target.getName());
                    if (parent != null) parent.addLocal(target);    // ... and in with the new
                    target.put(given, options | Data.PUT_OPTION_NO_TYPE_CHECK);  // this time the types will match!, so we're not stuck in an infinite loop of replacement
                    target.setIsFromAny(true); // Important! remember the ANY status so it can be replaced again in the future.
                }
            }
            // now recurse into given metadata...
            for (Data givenMeta : given.getMetadata()) {
                String givenMetaName = givenMeta.getName();
                // if we're not using "client rules", then we don't allow things like $truncated, $children, etc.
                if ((options&Data.PUT_OPTION_USE_CLIENT_RULES)==0) { // this is the normal case - data incoming to the server
                    if (givenMetaName.equals(Meta.PARTIAL)) continue; // we don't store $partial, but we allow it (we'll handle it later below)
                    if ((options&Data.PUT_OPTION_USE_POST_RULES)!=0) {
                        if (Rules.notAllowedForPost(givenMetaName)) throw new XDException(Errors.VALUE_FORMAT, "Can't post computed metadata '" + givenMetaName + "'");
                    }
                    else {
                        if (Rules.notAllowedForPut(givenMetaName)) throw new XDException(Errors.VALUE_FORMAT, "Can't put computed metadata '" + givenMetaName + "'");
                    }
                }
                Data targetMeta = target.find(givenMetaName);
                if (targetMeta == null) {
                    // extended metadata needs to provide either type or base in order to be created since it doesn't exist in the definition
                    Data givenMetaType = givenMeta.find(Meta.TYPE);
                    String givenMetaTypeString = givenMetaType != null ? givenMetaType.stringValue() : null;
                    Base givenMetaBase = givenMeta.getBase();
                    if (!getPolicy().allowCreate(this, givenMetaName, givenMetaTypeString, givenMetaBase)) continue; // W.27 says: for PUT, metadata not creatable shall be ignored
                    targetMeta = target.createMetadata(givenMetaName, givenMetaTypeString, givenMetaBase);
                }
                if (!targetMeta.isWritable() && !authorizer.inGodMode()) continue; // W.27 says: for PUT, metadata not writable shall be ignored
                targetMeta.put(givenMeta, options);
            }
            // now recurse into the children...
            // checking children depends on base and presence of $partial or $truncated and whether we're an instance or not
            // Unless the data has been marked $partial or $truncated, then this is the entire set of children.
            // We also don't clear existing children when building a prototype from a definition (because we always need to merge them)
            boolean givenPartial   = given.booleanValueOf(Meta.PARTIAL, false);
            boolean givenTruncated = given.booleanValueOf(Meta.TRUNCATED, false);
            if (!givenPartial)   target.removeLocal(Meta.PARTIAL);  // TODO: still needed? code vestage?
            if (!givenTruncated) target.removeLocal(Meta.TRUNCATED);// TODO: still needed? code vestage?
            if ((options&Data.PUT_OPTION_GIVEN_PARTIAL)!=0) givenPartial = true;
            if (target.canHaveChildren() && target.isInstance() && !(givenPartial || givenTruncated)) {
                // this was *not* marked marked $partial or $truncated, so we will try to remove any existing children that are not in
                // the given list of children. To avoid concurrent modification problems, we'll first make a list of children to remove.
                DataList childrenToDelete = new DataList();
                for (Data targetChild : target.getChildren()) {       // for every target child...
                    if (given.find(targetChild.getName()) == null) {  // ...if not found in the given children then delete it, but...
                        if (!authorizer.checkVisible(targetChild)) continue; // don't delete things that the client can't see, it's not his fault he didn't provide it back
                        childrenToDelete.add(targetChild);
                    }
                }
                switch (target.getBase()) {  // now we have a list to remove; the specific behavior for that removal is based on base
                    case ARRAY:
                    case UNKNOWN:
                        // we must be given children named exactly "1".."n" and we will only allow removal of those > n
                        int countOfGivenChildren = given.getChildren().size();
                        for (int i = 1; i <= countOfGivenChildren; i++) if (given.find(Integer.toString(i)) == null) throw new XDException(Errors.INCONSISTENT_VALUES, target, "Given data contains " + countOfGivenChildren + " array members, but member '" + i + "' was not provided");
                        for (Data childToDelete : childrenToDelete) target.delete(childToDelete.getName()); // if we passed the above test, then the others must be > n
                        break;
                    case LIST:
                    case SEQUENCEOF:
                    case POLY:
                    case COLLECTION:
                        for (Data childToDelete : childrenToDelete) target.delete(childToDelete.getName()); // freely remove the ones not found...
                        break;
                    case CHOICE:
                        if (given.getChildren().size() == 0)
                            throw new XDException(Errors.CANNOT_DELETE, target, "Provided data is not marked $partial and a valid choice was not provided");
                        for (Data childToDelete : childrenToDelete) target.delete(childToDelete.getName()); // freely remove the ones not found...(should only be one!)
                        break;
                    case OBJECT:
                    case COMPOSITION:
                    case SEQUENCE:
                        for (Data childToDelete : childrenToDelete) {
                            Data childProto = childToDelete.getPrototype();
                            if (childProto != null && !childProto.isOptional())
                                throw new XDException(Errors.CANNOT_DELETE, target, "Provided data is not marked $partial and non-optional child '" + childToDelete.getName() + "' was not provided");
                            target.delete(childToDelete.getName());
                        }
                        break;
                    default:
                        throw new XDError(target, "Unhandled base " + target.getBase() + " in put()");
                }
            }
            // now do a precheck of given size against limits (usually for clear-only or never-empty lists)
            if (given.getCount() > target.intValueOf(Meta.MAXIMUMSIZEFORWRITING, Integer.MAX_VALUE))
                throw new XDException(Errors.VALUE_OUT_OF_RANGE, target, "Number of provided children ("+given.getCount()+") is greater than allowed by $maximumSizeForWriting ("+target.intValueOf(Meta.MAXIMUMSIZEFORWRITING,Integer.MAX_VALUE)+ ")");
            if (given.getCount() < target.intValueOf(Meta.MINIMUMSIZEFORWRITING,0))
                throw new XDException(Errors.VALUE_OUT_OF_RANGE, target, "Number of provided children ("+given.getCount()+") is fewer than allowed by $minimumSizeForWriting ("+target.intValueOf(Meta.MINIMUMSIZEFORWRITING,0)+ ")");
            // now that we've possibly deleted children that were not provided, we can recurse into those that *are* provided
            for (Data givenChild : given.getChildren()) {
                String givenChildName = givenChild.getName();
                Data targetChild = target.find(givenChildName);
                if (targetChild == null) {
                    // if the target child doesn't exist, we create it locally here.
                    // the child's type can be specified by $type, or $extends or $overlays if we're in a definition
                    Data   givenChildType       = givenChild.find(Meta.TYPE);
                    String givenChildTypeString = givenChildType != null ? givenChildType.stringValue() : null;
                    Base   givenChildBase       = givenChild.getBase();
                    targetChild = target.createChild(givenChildName, givenChildTypeString, givenChildBase);
                }
                targetChild.put(givenChild, options);
            }
            // now that we have set our metadata, try to set the value (it will be checked against metadata limits)
            if (target.canHaveValue()) _putValueHelper(target, given);
            // and one more consistency check to check metadata against each other and value
            target.validateConsistency(); // throws if problem found
            // last minute sanity check
            if (target.getBase() == Base.POLY && !(target instanceof ParsedData)) throw new XDError("put ended up with a POLY target",target);
            // whew!  did we make it this far?
            return target;
        } catch (XDException e) {
            throw e.add(target,given);
        }
    }

    @Override public void  validateConsistency()  throws XDException { } // overridden by base types

    private static void   _putValueHelper(Data target, Data given) throws XDException {
        // handles commandable values that are not from BACnet; a Binding should be used to handle BACnet properties
        Context context = target.getContext();
        if (target.isCommandable()) {
            int  priority = context.isTarget(target) ? context.getPriority() : 16; // if top level, then the priority parameter applies, else 16
            Data priorityArray = getOrCreatePriorityArray(target);
            Data slot = priorityArray.get(String.valueOf(priority));
            slot.deleteChildren(); // remove previous 'chosen'
            if (given.getBase() == Base.NULL || given.canHaveValue() && !given.hasValue()) slot.addLocal(new NullData("null")); // if we are not given a value, then that's the same as a null
            else switch (target.getBase()) {
                case REAL:
                    slot.addLocal(new RealData("real",given.getValue()));
                    break;
                case ENUMERATED:
                    slot.addLocal(new EnumeratedData("enumerated",given.getValue()));
                    break;
                case UNSIGNED:
                    slot.addLocal(new UnsignedData("unsigned",given.getValue()));
                    break;
                case BOOLEAN:
                    slot.addLocal(new BooleanData("boolean",given.getValue()));
                    break;
                case INTEGER:
                    slot.addLocal(new IntegerData("integer",given.getValue()));
                    break;
                case DOUBLE:
                    slot.addLocal(new DoubleData("double",given.getValue()));
                    break;
                case TIME:
                    slot.addLocal(new TimeData("time",given.getValue()));
                    break;
                case STRING:
                    slot.addLocal(new StringData("characterstring",given.getValue()));
                    break;
                case OCTETSTRING:
                    slot.addLocal(new OctetStringData("octetstring",given.getValue()));
                    break;
                case BITSTRING:
                    slot.addLocal(new BitStringData("bitstring",given.getValue()));
                    break;
                case DATE:
                    slot.addLocal(new DateData("date",given.getValue()));
                    break;
                case OBJECTIDENTIFIER:
                    slot.addLocal(new DateData("objectidentifier",given.getValue()));
                    break;
                case DATETIME:
                    slot.addLocal(new DateData("datetime",given.getValue()));
                    break;
                default:
                    slot.addLocal(DataFactory.make(given.getBase(), "constructed-value", given.getValue()));
                    break;
            }
            // now that we've stored the new value in the right slot, reevaluate the entire array
            Object value = evaluatePriorityArray(priorityArray, target.findEffective(Meta.RELINQUISHDEFAULT));
            if (value != null) target.setValue(value);
        }
        else if (context.isTarget(target) && context.hasPriority()) {
            // weird rule! do nothing if we are given a priority and we're not commandable
        }
        else if (given.hasValue()) target.setValue(given.getValue());
    }

    private static Data     getOrCreatePriorityArray(Data target) throws XDException {
        Data priorityArray = target.getOrCreate(Meta.PRIORITYARRAY);
        for (int i=1 ; i<=16; i++) {   // make sure all slots are initialized
            String name = String.valueOf(i);
            Data slot = priorityArray.find(name);
            if (slot == null) slot = priorityArray.post(Instances.makeInstance("0-BACnetPriorityValue",name,new NullData("null")));
        }
        return priorityArray;
    }

    private static Object   evaluatePriorityArray(Data priorityArray, Data relinquishDefault) throws XDException {
        for (int i=1 ; i<=16; i++) {
            Data slot = priorityArray.get(String.valueOf(i));
            DataList children = slot.getChildren();
            if (children.size()==0) throw new XDError("priorityArray slot '"+i+"' is uninitialized");
            Data chosen = children.get(0);
            if (chosen.getName().equals("null")) continue; // if the value is the 'null' choice, then skip to next
            return chosen.getValue();
        }
        if (relinquishDefault != null && relinquishDefault.hasValue()) return relinquishDefault.getValue();
        return null;
    }


    private static void    _validateCompatibleTypes(Data target, String givenTypeName) throws XDException {
        if (!_isCompatibleType(target,givenTypeName)) throw new XDException(Errors.INCONSISTENT_VALUES, target, "Given type name " + givenTypeName + " is not compatible with target type " + target.getEffectiveType());
    }

    private static boolean _isCompatibleType(Data target, String givenTypeName) {
        try {
            // first check the simplest case: Any means any!
            if (target.getBase() == Base.ANY || target.isFromAny()) return true;

            // if the target has an explicit type, check that
            String targetTypeName = target.stringValueOf(Meta.TYPE, "");
            if (givenTypeName.equals(targetTypeName)) return true; // well, that was pretty easy too

            // now try the "effective type" name
            String targetEffectiveTypeName = target.getEffectiveType();
            if (givenTypeName.equals(targetEffectiveTypeName)) return true;

            // OK, so it's not an exact match... but is it compatible? i.e., given extends target
            Data targetPrototype = target.getPrototype();
            Data givenPrototype = Prototypes.getPrototypeFor(givenTypeName);
            if (targetPrototype == givenPrototype) return true;

            // try to find a match between prototype chains
            for (Data toMatch = givenPrototype; ; toMatch = toMatch.getPrototype()) { // go up the prototype chain for the given type to see if we can find a match to the target's prototype
                if (toMatch == targetPrototype) return true;
                if (toMatch.isBuiltin()) break;
            }
            // try again one level up in case the target is just an implied type based on a builtin
            if (!targetPrototype.isBuiltin()) {
                Data targetPrototypePrototype = targetPrototype.getPrototype();
                for (Data toMatch = givenPrototype; ; toMatch = toMatch.getPrototype()) { // go up the prototype chain for the given type to see if we can find a match to the target's prototype
                    if (toMatch == targetPrototypePrototype) return true;
                    if (toMatch.isBuiltin()) break;
                }
            }
        } catch (XDException e) {} // fall through to return false
        return false;
    }

    ////////////////////////////////////////////////////////////////////
    ////////////////////////////  POST  ////////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public Data       post(Data data)  throws XDException {
        // This returns a newly created child or ephemeral data. So in either case, the given data is invalid/unused after this.
        // The given data can contain POLYs that don't know their base type.  Since this codde can't necessarily know what an RPC binding
        // might want for its posted data, the given data is passed as-is to binding.prepost(). So bindings should do a put() to a target
        // of the expected type to validate the given data and set its base types.
        // For data that is to be persisted, this "clean up" operation is done with a create-then-put operation for the new child.
        preread();
        Authorizer authorizer  = getContext().getAuthorizer();
        if (isImmutable())                            throw new XDException(Errors.NOT_WRITABLE,   this, "Trying to modify immutable data");
        if (!isWritable() && !authorizer.inGodMode()) throw new XDException(Errors.NOT_WRITABLE,   this, "Target is not writable");
        if (!authorizer.checkWrite(this))             throw new XDException(Errors.NOT_AUTHORIZED, this, "Target is not writable with given authorization");
        // pick a name for the new child (binding can override this)
        data.setName(Rules.getNextAvailableChildName(this,data.getName()));
        // first give the binding a crack at it...
        Binding binding = findBinding();
        if (binding != null) data = binding.prepost(this,data);
        // if the binding changed this to an ephemeral, then short circuit the rest and just return it because
        // this post() is not persisting/creating/dirtifying anything.  e.g., it's actually an RPC-style "POST"
        if (data.getName().equals("..ephemeral")) return data;
        // otherwise, we have data to persist and later commit, so create the new child and put the given data into it.
        markDirty();
        // if the given data is a POLY (freshly parsed), then create a new child, then update it with put() to clean it up and validate it.
        if (data.isPoly()) {
            Data fresh = createChild(data.getName(), data.stringValueOf(Meta.TYPE, null), data.getBase());
            fresh.put(data,Data.PUT_OPTION_USE_POST_RULES);
            data = fresh;
        }
        addLocal(data);
        return data;
    }

    ////////////////////////////////////////////////////////////////////
    ///////////////////////////  DELETE  ///////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public void       delete(String name)    throws XDException {
        Data found = find(name); // item has to be present to be deleted so we'll give every chance to make it first
        if (found == null) return;// if nothing to delete... never mind.
        if (isImmutable()) throw new XDException(Errors.NOT_WRITABLE, this, "Can't delete '" + name + "' (or anything) from  immutable data");
        if (!getPolicy().allowDelete(this,name)) throw new XDException(Errors.CANNOT_DELETE, this, "Can't delete '" + name + "', denied by policy");
        getContext().getAuthorizer().requireWrite(this);
        if (isShadow()) {  // original children will be renamed on commit (we can't rename a shadow)
            markDirty();
            found.setIsDeleted(true);
            found.setIsDirty(true);
        }
        else {  // non-shadow children will be renamed right here, because we can, and there isn't going to be a commit.
            removeLocal(found);
            if (Rules.canRenumberChildren(getBase())) {  // if these are numbered positionally, then renumber after a delete
                int i = 1;
                for (Data child : getChildren()) child.setName(Integer.toString(i++));
            }
        }
    }

    @Override public void       deleteChildren() throws XDException {
        for (Data child : getChildren()) delete(child.getName());
    }



    ////////////////////////////////////////////////////////////////////
    ////////////////////////////  TYPE  ////////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public String     getEffectiveType()  throws XDException  {
        Data effectivePrototype = getEffectivePrototype();
        return !effectivePrototype.isBuiltin() ?  Prototypes.getPathFor(effectivePrototype) : "";
    }

    @Override public void       setPrototype(Data prototype)           {
        this.prototype = prototype;
    }

    @Override public Data       getPrototype()                         {
        return prototype != null? prototype : Prototypes.getPrototypeFor(getBase());
    }

    @Override public Data       getEffectivePrototype() throws XDException {
        // if it's NOT from an Any, then it's just normal instance data and its prototype points to its effective prototype directly
        if (isFromAny()) {
            // if it IS from an Any, its prototype points to its actual concrete type, not to its defined effective type
            // so we have to find the effective type by asking for the parent prototype
            Data parent = getParent();
            if (parent != null) { // if we have nowhere "above" to look, then we have no effective type
                Data parentProto = parent.getPrototype();
                if (!parentProto.isBuiltin()) { // if parent is a builtin, then it has no definition, so we can't be in it
                    switch (parent.getBase()) {
                        case OBJECT:
                        case COMPOSITION:
                        case SEQUENCE:
                            // find myself in parent's definition
                            Data meInParentsProto = parentProto.find(getName());
                            if (meInParentsProto == null) throw new XDError("Instance named '" + getName() + "' does not exist in definition " + Prototypes.getPathFor(parentProto), this);
                            return meInParentsProto;
                        case CHOICE:
                            // find myself in parent's $choices definition
                            Data choices = parentProto.findEffective(Meta.CHOICES);
                            if (choices != null) {
                                Data choice = choices.find(getName());
                                if (choice != null) return choice;
                                // if parent has defined choices and I'm not in it, that's bad
                                throw new XDError("Data named '" + getName() + "' not found as valid choice in " + Prototypes.getPathFor(parentProto), this);
                            }
                            break; // if parent doesn't define $choices, then just fall through to the end here
                    }
                }
            }
        }
        return getPrototype(); // if none of that caught anything, then our "effective" prototype is just our regular prototype
    }

    ////////////////////////////////////////////////////////////////////
    ////////////////////////////  COPY  ////////////////////////////////
    ////////////////////////////////////////////////////////////////////


    @Override public Data makeDeepCopy() throws XDException {  // makes a parentless copy, without specialization (isDefinition, etc.)
        Data copy = DataFactory.make(getBase(), getName());
        copy.setPrototype(getPrototype());
        // we have to set the flags individually because we don't have access to implementation-specific 'flags'
        // isDefinition(), isImmutable(), isPrototype(), isBuiltin() are not copied because this "fresh" data hasn't been "specialized" yet
        // isRooted() is not copied because this new clone is free-floating at the moment.
        // isShadow() is not copied because this is not a shadow of anything.
        // Other aspects like isOptional, isReadable, isWritable, etc. are determined by metadata, not flags, so those *will* get copied below
        copy.setIsFromNothing(isFromNothing());
        copy.setIsFromAny(isFromAny());
        copy.setIsLocalizable(isLocalizable());
        // now make shallow copies of children, metadata and value
        for (Data meta  : getMetadata())  copy.addLocal(meta.makeDeepCopy());  // must use getMetadata() to force preread (can't just copy existing 'subs' list)
        for (Data child : getChildren())  copy.addLocal(child.makeDeepCopy()); // must use getChildren() to force preread (can't just copy existing 'subs' list)
        if (canHaveValue() && hasValue()) copy.setLocalValue(getValue());      // values from getValue() are immutable or already a copy so we don't have to worry about making a copy
        // done. do with it as you please since it's disconnected from the original and not "specialized"
        return copy;
    }


    ////////////////////////////////////////////////////////////////////
    //////////////////////////  METADATA  //////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public DataList getLocalMetadata()  {
        DataList results = new DataList();
        if (subs != null) for (Data sub : subs) { if (Rules.isMetadata(sub)) results.add(sub); }
        return results;
    }

    @Override public DataList getMetadata() throws XDException {
        preread();
        return getLocalMetadata();
    }

    // also see getMetadataView() in BINDINGS section


    ////////////////////////////////////////////////////////////////////
    //////////////////////////  CHILDREN  //////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public boolean    canHaveChildren()  { return false; }   // overridden by ConstructedData

    @Override public DataList   getLocalChildren()  {  // returns a *separate* list  (deleting or adding from the returned list does not affect the real list)
        DataList results = new DataList();
        if (subs != null) for (Data sub : subs) { if (sub.isChild()) results.add(sub); }
        return results;
    }

    @Override public DataList   getChildren() throws XDException {  // returns a *separate* list  (deleting or adding from the returned list does not affect the real list)
        preread();
        DataList results = new DataList();
        if (subs != null) for (Data sub : subs) { if (sub.isChild() && !sub.isDeleted()) results.add(sub); }
        return results;
    }

    // also see getChildrenView() in BINDINGS section



    ////////////////////////////////////////////////////////////////////
    //////////////////////////  ADD / REMOVE  //////////////////////////
    ////////////////////////////////////////////////////////////////////
    // Low level functions that do no preread, renaming, or permissions checking.
    // They also do not mark "dirty", so will not be committed if using a Session!
    // Call getOrCreate(), post(), delete() if you want to commit in a Session.

    @Override public void addLocal(Data sub)   {
        if (subs != null) for (Data existing: subs) if (existing.getName().equals(sub.getName())) { subs.remove(existing); break; } // remove existing with same name!
        if (subs == null) subs = new DataList();
        subs.add(sub);
        sub.setParent(this);
    }

    @Override public void removeLocal(String name)  {
        if (subs != null) {
            for (Data sub : subs) {
                if (sub.getName().equals(name)) {
                    subs.remove(sub);
                    break;
                }
            }
        }
    }

    @Override public void removeLocal(Data sub)  {
        if (subs != null) subs.remove(sub);
    }

    @Override public void removeLocalChildren()  {
        for (Data child : getLocalChildren()) removeLocal(child);
    }

    ////////////////////////////////////////////////////////////////////
    ///////////////////////////  VALUE  ////////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public boolean    canHaveValue()              { return false; }  // overridden by subclasses

    @Override public boolean    hasValue()              throws XDException { return false; }  // overridden by subclasses

    @Override public Object     getValue()              throws XDException { return null; }   // overridden by subclasses

    @Override public void       setValue(Object value)  throws XDException {  }     // overridden by subclasses

    @Override public Object     getLocalValue()              { return null; }   // overridden by subclasses

    @Override public void       setLocalValue(Object value)  {  }           // overridden by subclasses

    @Override public void       set(String name, Object value) throws XDException {
        // a convenient shorthand for the messy looking getOrCreate(name).setvalue(value) for when you know the item exists or can be created
        getOrCreate(name).setValue(value);
    }



    ////////////////////////////////////////////////////////////////////
    ///////////////////////////// FLAGS ////////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public int        getFlags()                      { return flags; }
    @Override public void       setPersistentFlags(int flags)   { this.flags = (flags & FLAG_PERSIST_MASK) | (this.flags & FLAG_PERSIST_MASK); }  // only set low order flags

    @Override public boolean    getFlag(int flag)               { return (flags & flag) != 0; }
    @Override public void       setFlag(int flag, boolean state){ flags = state? flags | flag : flags & ~flag; }
    @Override public void       setFlag(int flag)               { flags = flags |  flag;  }
    @Override public void       clearFlag(int flag)             { flags = flags & ~flag;  }

    @Override public boolean    isDefinition()                  { return name.equals(Meta.AMPII_DEFINITIONS) || getFlag(FLAG_DEFINITION) || hasParent() && getParent().isDefinition(); }
    @Override public void       setIsDefinition(boolean state)  { setFlag(FLAG_DEFINITION,state); }

    @Override public boolean    isBuiltin()                     { return getFlag(FLAG_BUILTIN) || hasParent() && getParent().isBuiltin(); }
    @Override public void       setIsBuiltin(boolean state)     { setFlag(FLAG_BUILTIN,state); }

    @Override public boolean    isPrototype()                   { return getFlag(FLAG_PROTOTYPE) || hasParent() && getParent().isPrototype();  }
    @Override public void       setIsPrototype(boolean state)   { setFlag(FLAG_PROTOTYPE,state); }

    @Override public boolean    isImmutable()                   { return getFlag(FLAG_IMMUTABLE) || hasParent() && getParent().isImmutable(); }
    @Override public void       setIsImmutable(boolean state)   { setFlag(FLAG_IMMUTABLE,state); }

    @Override public boolean    isFromAny()                     { return getFlag(FLAG_FROM_ANY); }
    @Override public void       setIsFromAny(boolean state)     { setFlag(FLAG_FROM_ANY, state);  }

    @Override public boolean    isFromNothing()                 { return getFlag(FLAG_FROM_NOTHING); }
    @Override public void       setIsFromNothing(boolean state) { setFlag(FLAG_FROM_NOTHING, state); }

    @Override public boolean    isRooted()                      { return  getFlag(FLAG_ROOTED) || parent != null && parent.isRooted();}
    @Override public void       setIsRooted(boolean state)      { setFlag(FLAG_ROOTED, state); }

    @Override public boolean    isDirty()                       { return getFlag(FLAG_DIRTY); }
    @Override public void       setIsDirty(boolean state)       { setFlag(FLAG_DIRTY, state); }

    @Override public boolean    isDirtyBelow()                  { return getFlag(FLAG_DIRTY_BELOW); }
    @Override public void       setIsDirtyBelow(boolean state)  { setFlag(FLAG_DIRTY_BELOW, state); }

    @Override public boolean    isDeleted()                     { return getFlag(FLAG_DELETED); }
    @Override public void       setIsDeleted(boolean state)     { setFlag(FLAG_DELETED, state); }

    @Override public boolean    isInstance()                    { return !( isDefinition() || isBuiltin() || isPrototype() ); }

    @Override public boolean    isOptional() throws XDException { return effectiveBooleanValueOf(Meta.OPTIONAL, false); }

    @Override public boolean    isWritable() throws XDException { return !isImmutable() && !isBuiltin() && (effectiveBooleanValueOf(Meta.WRITABLE, false) || !isRooted()); }

    @Override public boolean    isCommandable() throws XDException { return !isImmutable() && !isBuiltin() && (effectiveBooleanValueOf(Meta.COMMANDABLE, false)); }

    @Override public boolean    isReadable() throws XDException { return effectiveBooleanValueOf(Meta.READABLE, true) || !isRooted(); }

    @Override public boolean    isVisible()  throws XDException { return effectiveBooleanValueOf(Meta.AUTHVISIBLE, true) || !isRooted(); }

    @Override public boolean    isLocalizable()                 { return getFlag(FLAG_LOCALIZABLE); } // StringData class sets this by default
    @Override public void       setIsLocalizable(boolean state) { setFlag(FLAG_LOCALIZABLE, state); }

    @Override public boolean    isShadow()                      { return original != null; }



    ////////////////////////////////////////////////////////////////////
    ////////////////////  CONVENIENCE METHODS  /////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public String    stringValue()                 throws XDException  { return "<???>"; }        // every subclass should override this
    @Override public boolean   booleanValue()                throws XDException  { return false; }          // every subclass overrides booleanValue() for filter evaluation to use
    @Override public int       intValue()                    throws XDException  { return 0; }
    @Override public long      longValue()                   throws XDException  { return 0;}
    @Override public float     floatValue()                  throws XDException  { return 0.0F; }
    @Override public double    doubleValue()                 throws XDException  { return 0.0; }
    @Override public byte[]    byteArrayValue()              throws XDException  { return new byte[0]; }
    @Override public StringSet stringSetValue()              throws XDException  { return new StringSet(); }
    @Override public Calendar  calendarValue()               throws XDException  { return new GregorianCalendar(); }

    @Override public String    stringValue(String defaultValue)       { try { return stringValue();    } catch (XDException e) { return defaultValue; } }
    @Override public boolean   booleanValue(boolean defaultValue)     { try { return booleanValue();   } catch (XDException e) { return defaultValue; } }
    @Override public int       intValue(int defaultValue)             { try { return intValue();       } catch (XDException e) { return defaultValue; } }
    @Override public long      longValue(long defaultValue)           { try { return longValue();      } catch (XDException e) { return defaultValue; } }
    @Override public float     floatValue(float defaultValue)         { try { return floatValue();     } catch (XDException e) { return defaultValue; } }
    @Override public double    doubleValue(double defaultValue)       { try { return doubleValue();    } catch (XDException e) { return defaultValue; } }
    @Override public byte[]    byteArrayValue(byte[] defaultValue)    { try { return byteArrayValue(); } catch (XDException e) { return defaultValue; } }
    @Override public StringSet stringSetValue(StringSet defaultValue) { try { return stringSetValue(); } catch (XDException e) { return defaultValue; } }
    @Override public Calendar  calendarValue(Calendar defaultValue)   { try { return calendarValue();  } catch (XDException e) { return defaultValue; } }

    @Override public String    stringValueOf(String name, String defaultValue)       { try { Data data=get(name);  return data.hasValue()? data.stringValue(defaultValue)   : defaultValue; } catch (XDException e) { return defaultValue; }}
    @Override public boolean   booleanValueOf(String name, boolean defaultValue)     { try { Data data=get(name);  return data.hasValue()? data.booleanValue(defaultValue)  : defaultValue; } catch (XDException e) { return defaultValue; }}
    @Override public int       intValueOf(String name, int defaultValue)             { try { Data data=get(name);  return data.hasValue()? data.intValue(defaultValue)      : defaultValue; } catch (XDException e) { return defaultValue; }}
    @Override public long      longValueOf(String name, long defaultValue)           { try { Data data=get(name);  return data.hasValue()? data.longValue(defaultValue)     : defaultValue; } catch (XDException e) { return defaultValue; }}
    @Override public float     floatValueOf(String name, float defaultValue)         { try { Data data=get(name);  return data.hasValue()? data.floatValue(defaultValue)    : defaultValue; } catch (XDException e) { return defaultValue; }}
    @Override public double    doubleValueOf(String name, double defaultValue)       { try { Data data=get(name);  return data.hasValue()? data.doubleValue(defaultValue)   : defaultValue; } catch (XDException e) { return defaultValue; }}
    @Override public byte[]    byteArrayValueOf(String name, byte[] defaultValue)    { try { Data data=get(name);  return data.hasValue()? data.byteArrayValue(defaultValue): defaultValue; } catch (XDException e) { return defaultValue; }}
    @Override public StringSet stringSetValueOf(String name, StringSet defaultValue) { try { Data data=get(name);  return data.hasValue()? data.stringSetValue(defaultValue): defaultValue; } catch (XDException e) { return defaultValue; }}
    @Override public Calendar  calendarValueOf(String name, Calendar defaultValue)   { try { Data data=get(name);  return data.hasValue()? data.calendarValue(defaultValue) : defaultValue; } catch (XDException e) { return defaultValue; }}

    @Override public String    effectiveStringValueOf(String name, String defaultValue)                { Data data=findEffective(name,null); return data!=null? data.stringValue(defaultValue)      : defaultValue; }
    @Override public boolean   effectiveBooleanValueOf(String name, boolean defaultValue)              { Data data=findEffective(name,null); return data!=null? data.booleanValue(defaultValue)     : defaultValue; }
    @Override public int       effectiveIntValueOf(String name, int defaultValue)                      { Data data=findEffective(name,null); return data!=null? data.intValue(defaultValue)         : defaultValue; }
    @Override public long      effectiveLongValueOf(String name, long defaultValue)                    { Data data=findEffective(name,null); return data!=null? data.longValue(defaultValue)        : defaultValue; }
    @Override public float     effectiveFloatValueOf(String name, float defaultValue)                  { Data data=findEffective(name,null); return data!=null? data.floatValue(defaultValue)       : defaultValue; }
    @Override public double    effectiveDoubleValueOf(String name, double defaultValue)                { Data data=findEffective(name,null); return data!=null? data.doubleValue(defaultValue)      : defaultValue; }
    @Override public byte[]    effectiveByteArrayValueOf(String name, byte[] defaultValue)             { Data data=findEffective(name,null); return data!=null? data.byteArrayValue(defaultValue)   : defaultValue; }
    @Override public StringSet effectiveStringSetValueOf(String name, StringSet defaultValue)          { Data data=findEffective(name,null); return data!=null? data.stringSetValue(defaultValue)   : defaultValue; }
    @Override public Calendar  effectiveCalendarValueOf(String name, Calendar defaultValue)            { Data data=findEffective(name,null); return data!=null? data.calendarValue(defaultValue)    : defaultValue; }


    ////////////////////////////////////////////////////////////////////
    ////////////////////////////  MISC  ////////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override public String toString() { return toString(false); }  // never call toString() for error messages, you should expose unauthorized/unreadable values

    @Override public String toString(boolean showData)  {  // use toString(false) for error messages to avoid exposing unauthorized/unreadable values
        StringBuilder results = new StringBuilder();
        results.append("[");
        boolean prefix = false;
        if (isDeleted())                  { results.append("X"); prefix=true; }
        if (getFlag(FLAG_PREREAD_DONE))   { results.append("P"); prefix=true; }
        if (isShadow())                   { results.append("S"); prefix=true; }
        if (isDirty())                    { results.append("D"); prefix=true; }
        if (isDirtyBelow())               { results.append("B"); prefix=true; }
        if (isRooted())                   { results.append("R"); prefix=true; }
        if (isFromAny())                  { results.append("A"); prefix=true; }
        if (isFromNothing())              { results.append("N"); prefix=true; }
        if (isDefinition())               { results.append("d"); prefix=true; }
        if (isPrototype())                { results.append("p"); prefix=true; }
        if (isImmutable())                { results.append("i"); prefix=true; }
        if (isBuiltin())                  { results.append("b"); prefix=true; }
        if (getFlag(Data.FLAG_BINDING_1)) { results.append("1"); prefix=true; }
        if (getFlag(Data.FLAG_BINDING_2)) { results.append("2"); prefix=true; }
        if (getFlag(Data.FLAG_BINDING_3)) { results.append("3"); prefix=true; }
        if (getFlag(Data.FLAG_BINDING_4)) { results.append("4"); prefix=true; }
        //if (isOptional())   { results.append("O"); prefix=true; } // this causes prereads and causes trouble in the debugger if you use toString for visualization
        //if (isWritable())   { results.append("W"); prefix=true; } // this causes prereads and causes trouble in the debugger if you use toString for visualization
        if (prefix) results.append("-");
        if (isPoly() && getBase()!= Base.POLY) results.append("("+Base.toString(getBase())+")"); // show a poly's assigned base in parentheses
        else results.append(Base.toString(getBase()));
        results.append("]");
        results.append(Path.toPath(this,true)); // true = annotateFakeParents for human consumption
        // don't try to show value - it causes prereads and causes trouble in the debugger if you use toString for visualization
        // try { if (showData && hasValue()) results.append(" = ").append(stringValue()); } catch (XDException e) {results.append("{value error}");}
        return results.toString();
    }

    private static long totalCreatedItems = 0; // instance counter, for diagnostics
    public  static long getTotalCreatedItems()   { return totalCreatedItems; }
    public  static void resetTotalCreatedItems() { totalCreatedItems = 0; }


}
