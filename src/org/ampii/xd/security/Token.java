// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.security;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Holds the data needed for the tokens that are exchanged using OAuth
 */
public class Token {
    private  byte   auth_serv_id = 0;   // internal auth server
    private  int    duration = 0;       //  if left as zero, will get default value from /.auth/int/config/token-dur
    private  String audience = TLS.getDeviceAudienceAsUUID().toString();
    private  short  user_id = 0;
    private  byte   user_roll = 1;
    private  String scopes = "";

    public Token()  {}

    public byte   getAuthServerID()                  { return auth_serv_id; }
    public void   getAuthServerID(byte auth_serv_id) { this.auth_serv_id = auth_serv_id; }
    public int    getDuration()                      { return duration; }
    public void   setDuration(int duration)          { this.duration = duration; }
    public String getAudience()                      { return audience; }
    public void   setAudience(String audience)       { this.audience = audience; }
    public short  getUserID()                        { return user_id; }
    public void   setUserID(short user_id)           { this.user_id = user_id; }
    public byte   getUserRoll()                      { return user_roll; }
    public void   getUserRoll(byte role_id)          { this.user_roll = role_id; }
    public String getScopes()                        { return scopes; }
    public void   setScopes(String scopes)           { this.scopes = scopes; }

    public String encode() throws XDException {
        try {
            AuthSettings auth = AuthManager.getSettings();
            if (!auth.activated) throw new XDException(Errors.TLS_CONFIG,"Can't make access token without an active TLS");

            if (duration == 0) duration = auth.int__config__token_dur;
            String     alg     = auth.int__config__keyalg;
            KeySpec    keyspec = new PKCS8EncodedKeySpec(auth.int__config__privkey);
            PrivateKey key     = KeyFactory.getInstance(alg.equals("RS256")?"RSA":"EC").generatePrivate(keyspec);
            String header = "{\"typ\":\"JWT\",\"alg\":\""+alg+"\"}";
            String claims = "{" +
                            "\"ver\":1,"+
                            "\"iss\":\""+auth_serv_id+"\","+
                            "\"aud\":\""+audience+ "\","+
                            "\"exp\":"+(System.currentTimeMillis()/1000L+duration)+","+
                            "\"sub\":\""+user_id+" "+ user_roll +"\","+
                            "\"scope\":\""+scopes+"\""+
                            "}";
            return JWT.generateToken(header, claims, alg, key);
        } catch (Exception e) { throw new XDError("could not sign token",e); }
    }

    public void decode(String tokenString) throws XDException {
        try {
            AuthSettings auth = AuthManager.getSettings();

            Data jwt = JWT.parseToken(tokenString);

            Data header = jwt.get("header");
            String typ  = header.stringValueOf("typ", "JWT");
            String alg  = header.stringValueOf("alg", "");
            if (!typ.equals("JWT")) throw new XDException(Errors.OAUTH_INVALID_TOKEN,"Header \"typ\" not equal to \"JWT\"");
            if (!alg.equals("ES256")&&!alg.equals("RS256")) throw new XDException(Errors.OAUTH_INVALID_TOKEN,"Header \"alg\" missing or not equal to either \"ES256\" or \"RS256\"");

            Data claims = jwt.get("claims");
            long ver    = claims.longValueOf("ver", -1);
            long iss    = claims.longValueOf("iss", -1);
            String aud  = claims.stringValueOf("aud", "");
            scopes      = claims.stringValueOf("scope", "");
            long exp    = claims.longValueOf("exp", -1);
            String sub  = claims.stringValueOf("sub", "0 0");

            if (ver!=1) throw new XDException(Errors.OAUTH_INVALID_TOKEN,"Claim \"ver\" missing or not equal to 1");
            auth_serv_id = (byte)iss; // we'll check the value later

            if (aud.isEmpty()) throw new XDException(Errors.OAUTH_INVALID_TOKEN,"Claim \"aud\" missing or not a single string member");
            audience = null; // using 'audience' member variable as 'found' flag here
            if (aud.equals(TLS.getDeviceAudienceAsUUID().toString())) audience = aud;
            if (audience == null) { // if it was not for me directly, then check if I'm in any groups
                List<byte[]> groups = auth.group_uuids;
                for (byte[] group : groups) {
                    if (TLS.getUUIDFromByteArray(group).toString().equals(aud)) { audience = aud; break; }
                }
            }
            if (audience == null) throw new XDException(Errors.AUTH_INVALID,"Claim \"aud\" does not match this device or any of its groups");

            if (exp == -1) throw new XDException(Errors.OAUTH_INVALID_TOKEN,"Token has no 'exp' parameter");
            if (exp < System.currentTimeMillis()/1000L) throw new XDException(Errors.AUTH_INVALID,"Token has expired");

            try {
                Scanner scanner = new Scanner(sub);
                user_id = Byte.decode(scanner.next());
                user_roll = Byte.decode(scanner.next());
            } catch (NumberFormatException e) { throw new XDException(Errors.OAUTH_INVALID_TOKEN, "Token 'sub' parameter is not a pair of space-separated numbers"); }

            // finally, check signature... but first we have to determine who signed it
            byte[] keybytes;
            if (auth_serv_id == 0) { // if internal, then check alg too (the only way it wouldn't match is if we've been reconfigured with different alg)
                keybytes = auth.int__config__pubkey;
                if (!auth.int__config__keyalg.equals(alg)) throw new XDException(Errors.OAUTH_INVALID_TOKEN,"Invalid 'alg' for internal server");
            }
            else if (auth_serv_id == 1) keybytes = auth.ext__pri_pubkey;
            else if (auth_serv_id == 2) keybytes = auth.ext__sec_pubkey;
            else throw new XDException(Errors.OAUTH_INVALID_TOKEN,"Claim \"iss\" missing or not equal to \"0\", \"1\", or \"2\"");
            PublicKey key = KeyFactory.getInstance(alg.equals("ES256")?"EC":"RSA").generatePublic(new X509EncodedKeySpec(keybytes));
            if (!JWT.checkSignature(tokenString,alg,key)) throw new XDException(Errors.OAUTH_INVALID_TOKEN,"Token signature validation failed");
        }
        catch (NoSuchAlgorithmException e) { throw new XDException(Errors.INTERNAL_ERROR, "Unsupported algorithm?", e); }
        catch (InvalidKeySpecException e)  { throw new XDException(Errors.VALUE_FORMAT, "Invalid public key for auth server #"+auth_serv_id, e); }
    }

}
