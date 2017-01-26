// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.ui;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.BooleanData;
import org.ampii.xd.data.basetypes.CollectionData;
import org.ampii.xd.data.basetypes.StringData;
import org.ampii.xd.database.DataStore;
import org.ampii.xd.database.Session;
import org.ampii.xd.definitions.Definitions;
import org.ampii.xd.definitions.Instances;
import org.ampii.xd.data.Context;
import org.ampii.xd.marshallers.CookieParser;
import org.ampii.xd.marshallers.DataParser;
import org.ampii.xd.resolver.Eval;
import org.ampii.xd.server.*;
import java.io.*;
import java.util.*;

/**
 * Static methods supporting the playground.js user interface.
 */
public class Playground {

    public static Response rpc(Request request) throws XDException {
        request.parseBodyParameters();
        String sid = getSid(request); // just about all messages are in the context of a session
        UserSession.bumpSession(sid);     // invalid sessions will be ignored, valid sessions will see this bump as "activity"
        String op = request.getParameter("op", "none");
        if      (op.equals("checkSession"))          return checkSession(sid, request);
        else if (op.equals("startSession"))          return startSession(request);
        else if (op.equals("endSession"))            return endSession(sid,request);
        else if (op.equals("csmlValidate"))          return csmlValidate(request);
        else if (op.equals("resetDatabase"))         return resetDatabase(request);
        else if (op.equals("createInstance"))        return createInstance(request);
        else if (op.equals("getParameters"))         return getParameters(request);
        else return new JSONResponse(HTTP.HTTP_200_OK, "err", "internal error: op="+op+" not handled");
    }

    private static Response checkSession(String sid, Request request) throws XDException {
        // if session is available, just return owner of "none"
        if (UserSession.getSid().isEmpty()) {
            return new JSONResponse(HTTP.HTTP_200_OK,
                    "own", "none");
        }
        // if session is in use, check to see if it's in use by this caller by checking cookie
        if (UserSession.getSid().equals(sid)) { // welcome back to active session
            return new JSONResponse(HTTP.HTTP_200_OK,
                    "own", "you",
                    "loc", DataStore.getDatabaseLocaleString(),
                    "dur", UserSession.getRemainingAsString(),
                    "exp", UserSession.getExpirationAsString());
        } else { // someone else owns the session
            return new JSONResponse(HTTP.HTTP_200_OK,
                    "own", "other",
                    "sid", UserSession.getSid(),
                    "dur", UserSession.getRemainingAsString(),
                    "exp", UserSession.getExpirationAsString());
        }
    }

    private static Response startSession(Request request) throws XDException {
        String nick = request.getParameter("nick", "anonymous");
        String sid = nick + " on " + request.peerAddress.toString() + " (" + UUID.randomUUID().toString() + ")";
        UserSession.startSession(sid);
        Response response = new JSONResponse(HTTP.HTTP_200_OK,
                "sid", sid,
                "dur", UserSession.getRemainingAsString(),
                "exp", UserSession.getExpirationAsString());
        response.addHeader("Set-Cookie", "sid=" + sid + ";");
        return response;
    }

    private static Response endSession(String sid, Request request) throws XDException {
        UserSession.endSession(sid);
        return new JSONResponse(HTTP.HTTP_200_OK);
    }

    private static Response getParameters(Request request) throws XDException {
        Response response = new JSONResponse(HTTP.HTTP_200_OK,
                "maxPopulateDepth", Application.maxPopulateDepth,
                "maxPopulateCount", Application.maxPopulateCount);
        return response;
    }


    private static Response resetDatabase(Request request) throws XDException {
        Locale loc = Locale.forLanguageTag(request.getParameter("loc", "en-US"));
        String cfg = request.getParameter("cfg", "config-empty");
        DataStore.initialize(loc, "resources/config/" + cfg + ".xml");
        return new JSONResponse(HTTP.HTTP_200_OK,"err","New database created with '"+cfg+"' for locale "+ DataStore.getDatabaseLocaleString());
    }

    private static Response csmlValidate(Request request) throws XDException {
        String csml = request.getBodyAsString();
        Locale loc  = Locale.forLanguageTag("en-US");
        String cfg  = request.getParameter("cfg", "current"); // cfg is the name of an initialization file or one of "current" or "current-persist"
        Data oldDataStore = null;

        if (!(cfg.equals("current") || cfg.equals("current-persist"))) {  // if NOT "current" or "current-persist", temporarily RESET the database
            // cfg is the name of a initialization file, so reinitialize the database temporarily with that content
            oldDataStore = DataStore.initialize(loc, "resources/config/" + cfg + ".xml");
        }
        Session session = Session.makeWriteSession("csmlValidate");
        try {
            // unfortunately, definitions can't use sessions, so they can't be rolled back, and we have to use getSystemDefinitionCollector()
            Data parsed = DataParser.parse(csml, Definitions.getSystemDefinitionCollector());
            Data target = null;
            if (cfg.equals("current-persist")) {
                target = session.getRoot().getOrCreate("my-data", null, Base.COLLECTION);
                if (!target.isWritable()) target.addLocal(new BooleanData(Meta.WRITABLE, true));
            }
            else {
                target = new CollectionData("temporary-target", new BooleanData(Meta.WRITABLE, true));
            }
            // unwrap it if it's a <CSML> or {"name":".csml"}
            if (parsed.getName().equals(".csml")) for (Data child : parsed.getChildren()) target.post(child);
            else target.post(parsed);
            // OK we made it this far without errors, now are we supposed to actually commit to the datastore?
            if (cfg.equals("current-persist")) session.commit();
        }
        catch (XDException e) {
            session.discard();
            return new JSONResponse(HTTP.HTTP_200_OK,"err",e.getLocalizedMessage());
        }
        finally {
            session.discard();
            if (oldDataStore != null) DataStore.setSystemRootIHopeYouKnowWhatYouAreDoing(oldDataStore);
        }
        return new JSONResponse(HTTP.HTTP_200_OK,"err","OK!");

    }

    private static Response createInstance(Request request) throws XDException {
        String path = request.getParameter("path", "");
        String type = request.getParameter("type", "");
        String name = request.getParameter("name", "new");
        String opt  = request.getParameter("opt",  "none");    // "all", "none", "random"
        String init = request.getParameter("init", "default"); // "default", "none", "random"
        String memb = request.getParameter("memb", "none");    // "none", "random"
        int maxd = Integer.parseInt(request.getParameter("maxd", String.valueOf(Application.maxPopulateDepth)));
        int maxc = Integer.parseInt(request.getParameter("maxc", String.valueOf(Application.maxPopulateCount)));
        int minm = Integer.parseInt(request.getParameter("minm", "0"));
        int maxm = Integer.parseInt(request.getParameter("maxm", "10"));
        int optp = Integer.parseInt(request.getParameter("optp", "50"));
        if (path.isEmpty()) throw new XDError("Missing path argument in RPC call");
        if (type.isEmpty()) throw new XDException(Errors.MISSING_PARAMETER,"Can't make instance of empty type name");
        Session session = Session.makeWriteSession("UI.createInstance");
        try {
            Data target = Eval.eval(session.getRoot(),path);
            Data data = Instances.makeInstance(type, name);
            data.addLocal(new StringData(Meta.TYPE, type));
            populate(data, opt, init, memb, 0, 0, maxd, maxc, minm, maxm, optp);
            target.post(data);
            session.commit();
        }
        finally { session.discard(); }
        return new JSONResponse(HTTP.HTTP_200_OK, "err", "OK");
    }

    //////////////////////
    public static long populate(Data data, String opt, String init, String memb, int depth, long count, int maxd, int maxc, int minm, int maxm, int optp) throws XDException {

        if (++depth > maxd) throw new XDException(Errors.CANNOT_CREATE,data,"Populate() got too deep (more than "+maxd+" levels). Circular definition?");
        if (++count > maxc)   throw new XDException(Errors.CANNOT_CREATE,data,"Populate() made too many data items (more than "+maxc+")");

        boolean initDefault = init.equals("default");
        boolean initRandom  = init.equals("random");
        // else init was "none" and we don't initialize the values
        boolean optAll      = opt.equals("all");
        boolean optRandom   = opt.equals("random");
        // else opt was "none" and we make no optional children
        boolean membRandom  = memb.equals("random");
        // else memb was "none" and we make no members
        Context context = new Context("populate()");

        switch (data.getBase()) {

            case NULL:
            case BIT:
            case ANY:
            case POLY:
                break;

            case BOOLEAN:
                if      (initDefault) data.setValue(false);
                else if (initRandom)  data.setValue(random.nextBoolean());
                // else leave unassigned
                break;

            case UNSIGNED:
                if      (initDefault) data.setValue(0);
                else if (initRandom)  data.setValue(random.nextInt(101));
                break;

            case INTEGER:
                if      (initDefault) data.setValue(0);
                else if (initRandom)  data.setValue(random.nextInt(201)-100);
                break;

            case REAL:
            case DOUBLE:
                if      (initDefault) data.setValue(0D);
                else if (initRandom)  data.setValue((random.nextInt(20100)-10000)/100D); // +/-100.00
                break;

            case RAW:
            case OCTETSTRING:
                if      (initDefault) data.setValue("");
                else if (initRandom)  data.setValue(generateRandomBytes());
                break;

            case STRING:
                if      (initDefault) data.setValue("");
                else if (initRandom)  data.setValue(getRandomWord());
                break;

            case LINK:
                if      (initDefault) data.setValue("");
                else if (initRandom)  data.setValue("/path/to/"+generateRandomWord());
                break;

            case STRINGSET:
                if      (initDefault) data.setValue("");
                else if (initRandom)  data.setValue(getRandomWord()+";"+getRandomWord());
                break;

            case BITSTRING:
                if (initDefault) {
                    data.setValue("");  // the default for bitstring is all bits false
                }
                else if (initRandom) {  // we are to pick some random bits
                    Data namedValues = data.findEffective(Meta.NAMEDBITS);
                    if (namedValues == null) { // if I don't have any named values, then we're free to make something up!
                        data.setValue(getRandomWord() + ";"+getRandomWord());
                    }
                    else { // there *is* $namedValues...
                        if (namedValues.getCount() == 0) { // ... but it's empty
                            data.setValue("");  // we can't make a legitimate random value with empty $namedValues
                        }
                        else { // Yay! we have at least one named value that we can randomly pick from
                            DataList names = namedValues.getChildren();
                            float probability = (names.size()==1)? 0.5F : 1.0F/(names.size()/2.0F);
                            String value = "";  boolean first = true; // start with empty value string
                            for (int i = 0; i < names.size(); i++) {
                                if (random.nextFloat() < probability) {
                                    if (!first) value += ";";
                                    first = false;
                                    value += names.get(i).getName();
                                }
                            }
                            data.setValue(value);
                        }
                    }
                }
                break;

            case ENUMERATED:
                Data namedValuesMeta = data.findEffective(Meta.NAMEDVALUES);
                if (namedValuesMeta == null)  {
                    if (initDefault) data.setValue("");
                    if (initRandom) data.setValue(generateRandomWord());
                }
                else {
                    String names[] = Eval.eval(namedValuesMeta, "$children", "").split(";");
                    if (initDefault) data.setValue(names[0]); // this will correctly init value to "" if $namesValues is empty
                    if (initRandom) data.setValue(names.length!=0&&!names[0].isEmpty()?names[random.nextInt(names.length)]:generateRandomWord());
                }
                break;

            case DATE:
            case DATETIME:
            case TIME:
                if      (initDefault) data.setValue(new GregorianCalendar(1900,1,1,0,0,0));
                else if (initRandom)  data.setValue(generateRandomCalendar());
                break;

            case DATEPATTERN:
                if (initDefault) data.setValue("*-*-* *");
                if (initRandom)  data.setValue(
                        (random.nextBoolean()?2000+random.nextInt(100):"*")+"-"+
                        (random.nextBoolean()?1+random.nextInt(12):"*")+"-"+
                        (random.nextBoolean()?1+random.nextInt(31):"*")+" *");
                break;

            case DATETIMEPATTERN:
                if (initDefault) data.setValue("*-*-* * *:*:*.*");
                if (initRandom)  data.setValue(
                        (random.nextBoolean()?2000+random.nextInt(100):"*")+"-"+
                        (random.nextBoolean()?1+random.nextInt(12):"*")+"-"+
                        (random.nextBoolean()?1+random.nextInt(31):"*")+" * " +
                        (random.nextBoolean()?random.nextInt(24):"*")+":"+
                        (random.nextBoolean()?random.nextInt(60):"*")+":"+
                        (random.nextBoolean()?random.nextInt(60):"*")+"."+
                        (random.nextBoolean()?random.nextInt(100):"*"));
                break;

            case TIMEPATTERN:
                if (initDefault) data.setValue("*:*:*.*");
                if (initRandom)  data.setValue(
                        (random.nextBoolean()?random.nextInt(24):"*")+":"+
                        (random.nextBoolean()?random.nextInt(60):"*")+":"+
                        (random.nextBoolean()?random.nextInt(60):"*")+"."+
                        (random.nextBoolean()?random.nextInt(100):"*"));
                break;

            case WEEKNDAY:
                if (initDefault) data.setValue("*,*,*");
                if (initRandom)  data.setValue(
                        (random.nextBoolean()?1+random.nextInt(14):"*")+","+
                        (random.nextBoolean()?1+random.nextInt(9):"*")+","+
                        (random.nextBoolean()?1+random.nextInt(7):"*"));
                break;

            case OBJECTIDENTIFIER:
                if      (initDefault) data.setValue("1023,4194303");
                else if (initRandom)  data.setValue(generateRandomObjectType()+","+random.nextInt(50));
                break;

            case OBJECTIDENTIFIERPATTERN:
                if      (initDefault) data.setValue(new GregorianCalendar(1900,1,1,0,0,0));
                else if (initRandom)  data.setValue(generateRandomObjectType()+",*");
                break;

            case SEQUENCE:
            case COMPOSITION:
            case OBJECT:
                Data prototype = data.getPrototype();
                if (prototype.isBuiltin()) {  // there is no definition...
                    if (membRandom) {  // if we are allowed to, let's make some random kiddies
                        for (int i = random.nextInt(maxm-minm)+minm; i != 0; i--)  {  // make members of random primitive base type
                            data.createChild(getRandomWord(), null, getRandomPrimitiveBaseType());
                        }
                    }
                }
                else {  // there *is* a definition, so let's look for optional children
                    for (Data definedChild : prototype.getChildren()) {
                        if (definedChild.isOptional()) {
                            if (optAll || (optRandom && random.nextInt(100)>optp)) {  // we will instantiate random optional children optp % of the time
                                data.createChild(definedChild.getName(), null, null);
                            }
                        }
                    }
                }
                // descend and populate the children
                for (Data child : data.getChildren()) {
                    count = populate(child, opt, init, memb, depth, count, maxd, maxc, minm, maxm, optp);
                }
                break;

            case COLLECTION:
            case ARRAY:
            case LIST:
            case SEQUENCEOF:
            case UNKNOWN:
                if (membRandom) {
                    data.deleteChildren(); // remove any inherited children and replace with new random ones
                    for (int i = random.nextInt(maxm-minm)+minm; i!=0; i--)  {  // make members of random primitive base type
                        data.createChild(getRandomWord(), null, getRandomPrimitiveBaseType());
                    }
                }
                // descend and populate the children
                for (Data child : data.getChildren()) {
                    count = populate(child, opt, init, memb, depth, count, maxd, maxc, minm, maxm, optp);
                }
                break;

            case CHOICE:
                if (initDefault || initRandom) { // the child of a Choice is it's "value", not a "member", so we don't pay attention to membXxxx
                    Data choices = data.findEffective(Meta.CHOICES);
                    if (choices == null) { // no defined $choices, so make one up with a random primitive type
                        if (initRandom) {
                            data.createChild(getRandomWord(), null, getRandomPrimitiveBaseType());
                        }
                    }
                    else {
                        DataList definedChoices = choices.getChildren();
                        if (definedChoices.size() != 0) {  // (if the defined choices list is empty, there nothing we can do)
                            // get either the first or a random choice
                            String name = initDefault? definedChoices.get(0).getName() : definedChoices.get(random.nextInt(definedChoices.size())).getName();
                            data.createChild(name, null, null);
                        }
                    }
                    for (Data child : data.getChildren()) {
                        count = populate(child, opt, init, memb, depth, count, maxd, maxc, minm, maxm, optp); // there should only be one but this is a nice safe way of handling zero as well
                    }
                }
                break;
        }
        return count;
    }
    private static Random   random = new Random();
    private static byte[]   generateRandomBytes()                       { return generateRandomBytes(4, 4+random.nextInt(8)); }
    private static byte[]   generateRandomBytes(int minLen, int maxLen) { byte bytes[] = new byte[minLen+random.nextInt(maxLen-minLen+1)]; random.nextBytes(bytes); return bytes; }
    private static Calendar generateRandomCalendar()                    { Calendar cal = new GregorianCalendar(); cal.add(Calendar.SECOND, random.nextInt(10000) - 10000); return cal; }
    private static List<String> words = null;
    private static void loadWords() {
        if (words == null) {
            words = new ArrayList<>(10000); // original words.txt file is 10000 most common words in english
            try {
                BufferedReader br = new BufferedReader(new FileReader(Application.wordsFile));
                for (String word; (word = br.readLine()) != null;) { words.add(word); }
                br.close();
            } catch (IOException e) {} // leave words list empty on error;
        }
    }


    private static Base   getRandomBaseType() { return baseTypes[random.nextInt(baseTypes.length)]; }
    private static Base[] baseTypes = { Base.BOOLEAN, Base.UNSIGNED, Base.INTEGER, Base.REAL, Base.DOUBLE, Base.OCTETSTRING, Base.STRING,
            Base.BITSTRING, Base.ENUMERATED, Base.DATE, Base.DATETIME, Base.TIME, Base.OBJECTIDENTIFIER, Base.SEQUENCE, Base.ARRAY,
            Base.LIST, Base.SEQUENCEOF, Base.CHOICE, Base.OBJECT, Base.LINK, Base.STRINGSET, Base.COMPOSITION, Base.COLLECTION};

    private static Base   getRandomPrimitiveBaseType() { return primitiveBaseTypesForRandom[random.nextInt(primitiveBaseTypesForRandom.length)]; }
    private static Base[] primitiveBaseTypesForRandom = { Base.BOOLEAN, Base.UNSIGNED, Base.INTEGER, Base.REAL, Base.DOUBLE, Base.OCTETSTRING,
            Base.STRING, Base.BITSTRING, Base.ENUMERATED, Base.DATE, Base.DATETIME, Base.TIME, Base.OBJECTIDENTIFIER, Base.LINK, Base.STRINGSET };

    private static String   getRandomWord() {
        loadWords();
        int wordsSize = words.size();
        if (wordsSize > 0)  return words.get(random.nextInt(wordsSize));
        else return generateRandomWord();
    }
    private static String   generateRandomWord()  { return generateRandomWord(3, 3 + random.nextInt(5)); } // 3-8 random chars
    private static String   generateRandomWord(int min, int max) {
        int wordLength = min + random.nextInt(max-min+1);
        StringBuilder sb = new StringBuilder(wordLength);
        // for each letter in the wordgenerate a letter between a and z and add it to the String
        for(int i = 0; i < wordLength; i++) sb.append((char)('a' + random.nextInt('z' - 'a')));
        return sb.toString();
    }

    private static String   generateRandomObjectType() {
        try {
            DataList names = Session.atomicGetCopy("generateRandomObjectType",".../.defs/0-BACnetObjectType/$namedValues").getChildren();
            return names.get(random.nextInt(names.size())).getName();
        }
        catch(XDException e) { return String.valueOf(random.nextInt(1024)); }
    }




    //////////////////////

    private static String getSid(Request request) throws XDException {
        Data cookie = new CookieParser().parse(request.getHeader("cookie", ""));
        return cookie.stringValueOf("sid", "no-session");
    }



}
