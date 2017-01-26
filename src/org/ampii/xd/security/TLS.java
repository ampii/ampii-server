// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.security;

import org.ampii.xd.common.XDException;
import org.ampii.xd.common.Errors;
import org.ampii.xd.server.Server;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.UUID;

/**
 * Manages the TLS listening port
 */
public class TLS {

    private static Certificate   deviceCert;
    private static RSAPrivateKey privateKey;
    private static byte[]        deviceAudience;

    public static void activate(AuthSettings auth) throws XDException {

        try {
            deviceAudience       = auth.dev_uuid;

            //KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            //KeySpec ks = new PKCS8EncodedKeySpec(dev_key_pend.byteArrayValue());
            //privateKey = (RSAPrivateKey)keyFactory.generatePrivate(ks);

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(auth.dev_key_pend);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateKey) kf.generatePrivate(spec);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            deviceCert = cf.generateCertificate(new ByteArrayInputStream(auth.dev_cert_pend));

            //TODO: check that the ca_certs are valid and that the dev_cert is verifiable with one of the ca_certs

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            keystore.setCertificateEntry("dev-cert", deviceCert);
            keystore.setKeyEntry("dev-key", privateKey, "".toCharArray(), new Certificate[]{deviceCert});
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keystore);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, "".toCharArray());
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            SSLContext.setDefault(ctx);

        }
        catch (KeyStoreException e)         { throw new XDException(Errors.TLS_CONFIG,"Bad keystore",e); }
        catch (UnrecoverableKeyException e) { throw new XDException(Errors.TLS_CONFIG,"Bad key",e); }
        catch (KeyManagementException e)    { throw new XDException(Errors.TLS_CONFIG,"Key management problem",e); }
        catch (IOException e)               { throw new XDException(Errors.TLS_CONFIG,"Could not initialize keystore",e); }
        catch (CertificateException e)      { throw new XDException(Errors.TLS_CONFIG,"Bad certificate",e); }
        catch (InvalidKeySpecException e)   { throw new XDException(Errors.TLS_CONFIG,"Invalid private key format",e); }
        catch (NoSuchAlgorithmException e)  { throw new XDException(Errors.TLS_CONFIG,"Algorithm not available",e); }

        // and finally, actually start the server
        Server.startTls();

    }
    public static void deactivate() throws XDException {
        Server.stopTls();
    }


    public static byte[] getDeviceAudienceAsByteArray()  {
        if (deviceAudience == null) return new byte[16];
        return deviceAudience;
    }
    public static UUID getDeviceAudienceAsUUID() {
        return getUUIDFromByteArray(getDeviceAudienceAsByteArray());
    }

    public static UUID getUUIDFromByteArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

    public static Certificate getDeviceCert() throws XDException {
        if (deviceCert == null) throw new XDException(Errors.INTERNAL_ERROR,"Cannot get device certificate:TLS not activated yet");
        return deviceCert;
    }
    public static RSAPrivateKey getPrivateKey() throws XDException {
        if (privateKey == null) throw new XDException(Errors.INTERNAL_ERROR,"Cannot get device private key: TLS not activated yet");
        return privateKey;
    }

}
