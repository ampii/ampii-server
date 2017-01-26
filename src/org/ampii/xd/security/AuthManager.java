// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.security;

import org.ampii.xd.application.Policy;
import org.ampii.xd.bindings.Binding;
import org.ampii.xd.bindings.DefaultBinding;
import org.ampii.xd.bindings.DefaultBindingPolicy;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.database.Session;
import org.ampii.xd.marshallers.DataParser;
import java.io.File;
import java.util.*;

/**
 * Manages the authorization data, including mapping between the internal native representation and the external "/.auth" data.
 *
 * @author drobin
 */
public class AuthManager {

    private static AuthSettings settings = new AuthSettings();  // native structure that holds the settings for the /.auth data

    public static AuthSettings getSettings() { return settings; } // for code that wants direct access to the native data

    public static void initializeFromFile(File file) throws XDException {
        Session.atomicPut("AuthManager.init", ".../.auth", DataParser.parse(file));
    }

    public static void resetToFactoryDefaults() throws XDException {
        TLS.deactivate();
        settings = new AuthSettings(); // simple!  just throw away AuthSettings and start over again.
    }

    private static Binding theBinding = new DefaultBinding() {  // singleton binding for connecting "/.auth" to AuthManager.settings
        @Override public void    preread(Data target) throws XDException { AuthManager.preread(target);}  // do everything regardless of name
        @Override public boolean commit(Data target)  throws XDException { return AuthManager.commit(target); }
        @Override public Policy getPolicy()          { return thePolicy; }
    };

    private static Policy thePolicy = new DefaultBindingPolicy() {  // singleton binding for connecting "/.auth" to AuthManager.settings
        @Override public boolean    allowCreate(Data target, String name, String type, Base base) {
            return Rules.isChild(name) || name.equals(Meta.AUTHWRITE);
        }
        @Override public boolean    allowDelete(Data target, String name) {
            return name.equals(Meta.AUTHWRITE);
        }
    };

    public static Binding getBinding() { return theBinding; }

    private static void preread(Data target) {
        target.getContext().getAuthorizer().enterGodMode(); // we are updating existing rooted /.auth so we have to override writability
        try { target.put(settings.toData());}
        catch (XDException e) { throw new XDError("Unexpected exception in AuthManager.preread()",e);}
        finally { target.getContext().getAuthorizer().exitGodMode(); }
    }

    private static boolean commit(Data target) throws XDException {
        settings.fromData(target);
        // now to see if the activation flag got set
        if (settings.tls_activate) {
            // autoreset flag regardless of actual success below
            settings.tls_activate = false;
            // try to activate the TLS port
            TLS.activate(settings);   // will throw exception if problem completing
            // success! - move pending to active and clear pending values
            settings.dev_key = settings.dev_key_pend;
            settings.dev_key_pend = new byte[0];
            settings.dev_cert = settings.dev_cert_pend;
            settings.dev_cert_pend = new byte[0];
            settings.ca_certs = settings.ca_certs_pend;
            settings.ca_certs_pend = new ArrayList<>();
            settings.activated = true;  // mark activated - affects authorization required for future writes
        }
        return true;
    }

}
