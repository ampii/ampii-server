// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.security;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.OctetStringData;
import org.ampii.xd.data.basetypes.StringData;
import org.ampii.xd.definitions.Instances;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.*;

/**
 * The native storage for the /.auth info, managed by {@link AuthManager}
 */
public class AuthSettings {

    public byte[] dev_uuid = uuidToBytes(Application.deviceUUID != null? Application.deviceUUID : UUID.nameUUIDFromBytes(Application.hostName.getBytes())); // if unassigned by command line arg, make one based on hostname
    public List<byte[]> ca_certs = new ArrayList<>();
    public List<byte[]> ca_certs_pend = new ArrayList<>();
    public byte[] dev_cert = new byte[0];
    public byte[] dev_cert_pend = new byte[0];
    public byte[] dev_key = new byte[0];
    public byte[] dev_key_pend = new byte[0];
    public boolean tls_activate = false;
    public boolean int__enable = true;
    public String int__user = ".";
    public String int__pass = ".";
    public String int__id = ".";
    public String int__secret = ".";
    public int int__config__token_dur = 86400;
    public byte[] int__config__privkey = new byte[0];
    public byte[] int__config__pubkey = new byte[0];
    public String int__config__keyalg = "RS256";
    public String ext__pri_uri = "";
    public byte[] ext__pri_cert = new byte[0];
    public byte[] ext__pri_pubkey = new byte[0];
    public String ext__sec_uri = "";
    public byte[] ext__sec_cert = new byte[0];
    public byte[] ext__sec_pubkey = new byte[0];
    public List<byte[]> group_uuids = new ArrayList<>();
    public Map<String, String> scope_desc = new HashMap<>();

    public boolean activated;  // controls writability of certs and internal auth server stuff

    public AuthSettings() {
        // the above initializers are mostly what needed for factory defaults, but the internal token key is a little more complicated
        // This might get overwritten later, but we have to ensure that we have a valid value in case it doesn't get changed.
        try {
            if (Application.useEllipticTokenKey) {
                KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
                ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("secp256r1");
                g.initialize(ecGenSpec, new SecureRandom());
                KeyPair pair = g.generateKeyPair();
                int__config__keyalg = "ES256";
                int__config__privkey = pair.getPrivate().getEncoded();
                int__config__pubkey = pair.getPublic().getEncoded();
            } else {
                KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
                g.initialize(2048); // Set the desired key length
                KeyPair pair = g.generateKeyPair();
                int__config__keyalg = "RS256";
                int__config__privkey = pair.getPrivate().getEncoded();
                int__config__pubkey = pair.getPublic().getEncoded();
            }
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new XDError("AuthNativeData key generation error", e);
        }
    }

    /* the toData()/fromData() make and accept this:
    <Composition name="0-BACnetWsAuth">
        <OctetString name="dev-uuid" comment="writability is an implementation choice"/>
        <List name="ca-certs"      memberType="OctetString"/>
        <List name="ca-certs-pend" memberType="OctetString" writable="true" authWrite="" comment="authWrite changes to 'auth' after TLS activation" />
        <OctetString name="dev-cert" />
        <OctetString name="dev-cert-pend" writable="true"                   authWrite="" comment="authWrite changes to 'auth' after TLS activation"/>
        <OctetString name="dev-key"                       readable="false" />
        <OctetString name="dev-key-pend"  writable="true" readable="false"  authWrite="" comment="authWrite changes to 'auth' after TLS activation"/>
        <Boolean name="tls-activate" value="false" writable="true"          authWrite="" comment="authWrite changes to 'auth' after TLS activation"/>
        <Composition name="int" >
            <Boolean name="enable" writable="true" authWrite="auth"/>
            <String name="user"    writable="true" authWrite="auth" readable="false"/>
            <String name="pass"    writable="true" authWrite="auth" readable="false"/>
            <String name="id"      writable="true" authWrite="auth" readable="false"/>
            <String name="secret"  writable="true" authWrite="auth" readable="false"/>
            <Composition name="config" type="org.ampii.types.InternalAuthConfig" partial="true">
                <Unsigned    name="token-dur" value="86400" />
                <OctetString name="privkey" value="...something..."/>
                <OctetString name="pubkey"  value="...something..."/>
                <String      name="keyalg"  value="RS256"/>
            </Composition>
        </Composition>
        <Composition name="ext" >
            <String      name="pri-uri"    writable="true" authWrite="auth"/>
            <OctetString name="pri-cert"   writable="true" authWrite="auth"/>
            <OctetString name="pri-pubkey" writable="true" authWrite="auth"/>
            <String      name="sec-uri"    writable="true" authWrite="auth"/>
            <OctetString name="sec-cert"   writable="true" authWrite="auth" />
            <OctetString name="sec-pubkey" writable="true" authWrite="auth"/>
        </Composition>
        <List name="group-uuids"           writable="true" authWrite="auth" />
        <Collection name="scope-desc"/>
    </Composition>
    */

    public Data    toData() throws XDException {

        Data data = Instances.makeInstance("0-BACnetWsAuth");

        data.getLocal("dev-uuid").setLocalValue(dev_uuid);

        Data caCertsList = data.getLocal("ca-certs");
        for (byte[] cert : ca_certs) caCertsList.post(new OctetStringData("", cert));

        Data caCertsPendList = data.getLocal("ca-certs-pend");
        for (byte[] cert : ca_certs_pend) caCertsPendList.post(new OctetStringData("", cert));

        data.get("dev-cert").setLocalValue(dev_cert);
        data.get("dev-cert-pend").setLocalValue(dev_cert_pend);
        data.get("dev-key").setLocalValue(dev_key);
        data.get("dev-key-pend").setLocalValue(dev_key_pend);
        data.get("tls-activate").setLocalValue(tls_activate);

        // set the authorization requirements based on activation
        data.get("ca-certs-pend").addLocal(new StringData("$authWrite", activated ? "auth" : ""));
        data.get("dev-cert-pend").addLocal(new StringData("$authWrite", activated ? "auth" : ""));
        data.get("dev-key-pend").addLocal(new StringData("$authWrite", activated ? "auth" : ""));
        data.get("tls-activate").addLocal(new StringData("$authWrite", activated ? "auth" : ""));

        // with the "/.auth/int" composition
        Data intComposition = data.getLocal("int");
        intComposition.get("enable").setLocalValue(int__enable);
        intComposition.get("user").setLocalValue(int__user);
        intComposition.get("pass").setLocalValue(int__pass);
        intComposition.get("id").setLocalValue(int__id);
        intComposition.get("secret").setLocalValue(int__secret);

        // set the authorization requirements based on activation
        intComposition.get("enable").addLocal(new StringData("$authWrite", activated ? "auth" : ""));
        intComposition.get("user").addLocal(new StringData("$authWrite", activated ? "auth" : ""));
        intComposition.get("pass").addLocal(new StringData("$authWrite", activated ? "auth" : ""));
        intComposition.get("id").addLocal(new StringData("$authWrite", activated ? "auth" : ""));
        intComposition.get("secret").addLocal(new StringData("$authWrite", activated ? "auth" : ""));

        // with the "/.auth/int/config" composition
        Data intConfigComposition = intComposition.getLocal("config").put(Instances.makeInstance("org.ampii.types.InternalAuthConfig")); // replace default ANY placeholder
        intConfigComposition.getLocal("token-dur").setLocalValue(int__config__token_dur);
        intConfigComposition.getLocal("privkey").setLocalValue(int__config__privkey);
        intConfigComposition.getLocal("pubkey").setLocalValue(int__config__pubkey);
        intConfigComposition.getLocal("keyalg").setLocalValue(int__config__keyalg);

        // with the "/.auth/ext" list
        Data extList = data.get("ext");
        extList.getLocal("pri-uri").setLocalValue(ext__pri_uri);
        extList.getLocal("pri-cert").setLocalValue(ext__pri_cert);
        extList.getLocal("pri-pubkey").setLocalValue(ext__pri_pubkey);
        extList.getLocal("sec-uri").setLocalValue(ext__sec_uri);
        extList.getLocal("sec-cert").setLocalValue(ext__sec_cert);
        extList.getLocal("sec-pubkey").setLocalValue(ext__sec_pubkey);

        Data groupUUIDsList = data.getLocal("group-uuids");
        for (byte[] uuid : group_uuids) groupUUIDsList.post(new OctetStringData("", uuid));

        Data scopeDescCollection = data.getLocal("scope-desc");
        for (Map.Entry<String, String> entry : scope_desc.entrySet())
            scopeDescCollection.addLocal(new StringData(entry.getKey(), entry.getValue()));

        return data;
    }

    public void    fromData(Data auth) throws XDException {

        dev_uuid = auth.byteArrayValueOf("dev-uuid", dev_uuid);

        Data caCertsList = auth.findLocal("ca-certs");
        if (caCertsList != null) {
            ca_certs.clear();
            for (Data cert : caCertsList.getChildren()) ca_certs.add(cert.byteArrayValue());
        }

        Data caCertsPendList = auth.findLocal("ca-certs-pend");
        if (caCertsPendList!=null) {
            ca_certs_pend.clear();
            for (Data cert : caCertsPendList.getChildren()) ca_certs_pend.add(cert.byteArrayValue());
        }

        dev_cert      = auth.byteArrayValueOf("dev-cert", dev_cert);
        dev_cert_pend = auth.byteArrayValueOf("dev-cert-pend", dev_cert_pend);
        dev_key       = auth.byteArrayValueOf("dev-key", dev_key);
        dev_key_pend  = auth.byteArrayValueOf("dev-key-pend", dev_key_pend);
        tls_activate  = auth.booleanValueOf("tls-activate", false);

        Data intComposition = auth.findLocal("int");
        if (intComposition!=null) {
            int__enable = intComposition.booleanValueOf("enable", int__enable);
            int__user   = intComposition.stringValueOf("user", int__pass);
            int__pass   = intComposition.stringValueOf("pass", int__pass);
            int__id     = intComposition.stringValueOf("id", int__id);
            int__secret = intComposition.stringValueOf("secret", int__secret);
            // with the "/.auth/int/config" composition
            Data intConfigComposition = intComposition.get("config");
            if (intConfigComposition!=null) {
                int__config__token_dur = intConfigComposition.intValueOf("token-dur", int__config__token_dur);
                int__config__privkey   = intConfigComposition.byteArrayValueOf("privkey", int__config__privkey);
                int__config__pubkey    = intConfigComposition.byteArrayValueOf("pubkey", int__config__pubkey);
                int__config__keyalg    = intConfigComposition.stringValueOf("keyalg", int__config__keyalg);
            }
        }

        Data extList = auth.findLocal("ext");
        if (extList!=null) {
            ext__pri_uri    = extList.stringValueOf("pri-uri", ext__pri_uri);
            ext__pri_cert   = extList.byteArrayValueOf("pri-cert", ext__pri_cert);
            ext__pri_pubkey = extList.byteArrayValueOf("pri-pubkey", ext__pri_pubkey);
            ext__sec_uri    = extList.stringValueOf("sec-uri", ext__sec_uri);
            ext__sec_cert   = extList.byteArrayValueOf("sec-cert", ext__sec_cert);
            ext__sec_pubkey = extList.byteArrayValueOf("sec-pubkey", ext__sec_pubkey);
        }

        Data groupUUIDsList = auth.find("group-uuids");
        if (groupUUIDsList!=null) {
            group_uuids.clear();
            for (Data uuid : groupUUIDsList.getChildren()) group_uuids.add(uuid.byteArrayValue());
        }

        Data scopeDescCollection = auth.get("scope-desc");
        if (scopeDescCollection!=null) {
            scope_desc.clear();
            for (Data desc : scopeDescCollection.getChildren()) scope_desc.put(desc.getName(), desc.stringValue());
        }
    }

    private byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private UUID uuidFromBytes(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

}
