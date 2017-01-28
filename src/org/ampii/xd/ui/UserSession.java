// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.ui;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDException;
import org.ampii.xd.database.Session;
import javax.xml.bind.DatatypeConverter;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Support for the one-at-a-time "user sessions" in the Playground.
 * <p>
 * At the moment, this is only used by Playground.java to only allow only one user at a time in the playground.
 * So this is actually not so much a "session" manager as just a fancy lockout semaphore.
 * Everything here is static since there can be only one "user session" at a time.
 * This has nothing to do with data sessions defined by {@link Session}.
 *
 * @author daverobin
 */
public class UserSession {

    private static String currentID = "";
    private static long start;

    public static String getSid() {
        if (getRemaining()==0) endSession();
        return currentID;
    }

    public static void startSession(String id) throws XDException {
        if (!currentID.isEmpty()) throw new XDException(Errors.NOT_AUTHORIZED, "Cannot start new session. Currently already in session with '"+currentID+"' until " + getExpirationAsString());
        currentID = id;
        start = System.currentTimeMillis();
    }

    public static void endSession(String id) throws XDException {
        if (!currentID.equals(id)) throw new XDException(Errors.NOT_AUTHORIZED, "Invalid session ID. Currently in session with '"+currentID+"' until " +getExpirationAsString());
        currentID = "";
    }
    public static void endSession() {
        currentID = "";
    }

    public static void bumpSession(String id) throws XDException {
        // invalid sessions will be ignored, valid sessions will see this as "activity"
        if (currentID.equals(id)) start = System.currentTimeMillis();
    }

    public static long getRemaining() {
        long duration  = (System.currentTimeMillis() - start) / 1000;
        long remaining = Application.sessionTimeout - duration;
        return remaining < 0? 0 : remaining;
    }

    public static String getRemainingAsString() {
        long remaining = getRemaining();
        long secs      = remaining%60;
        return remaining/60 +  (secs<10? ":0"+secs : ":"+secs);
    }
    public static String getExpirationAsString() {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.SECOND, (int) getRemaining());
        return DatatypeConverter.printDateTime((Calendar) cal);
    }



}
