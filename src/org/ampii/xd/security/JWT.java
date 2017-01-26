// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.security;

import org.ampii.xd.common.Errors;
import org.ampii.xd.common.XDError;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.basetypes.CollectionData;
import org.ampii.xd.marshallers.DataParser;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.*;

/**
 * Implements methods to produce/consume JSON Web Tokens, used by the Token class for OAuth authorization
 */
public class JWT {

    public static String generateToken(String header, String claims, String alg, PrivateKey key) throws XDException {
        String body = base64UrlEncode(header)+"."+base64UrlEncode(claims);
        byte[] signature = makeSignature(body.getBytes(), alg, key);
        return body+"."+base64UrlEncode(signature);
    }

    public static Data parseToken(String token) throws XDException {
        Data jwt = new CollectionData("jwt");
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new XDException(Errors.OAUTH_INVALID_TOKEN, "Authorization token does not have three parts");

        String headerJSON = base64UrlDecodeToString(parts[0]);
        Data header = DataParser.parse(headerJSON);
        header.setName("header");
        jwt.addLocal(header);

        String claimsJSON = base64UrlDecodeToString(parts[1]);
        Data claims = DataParser.parse(claimsJSON);
        claims.setName("claims");
        jwt.addLocal(claims);

        return jwt;
    }


    public static boolean checkSignature(String token, String alg, PublicKey key) throws XDException {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new XDException(Errors.OAUTH_INVALID_TOKEN, "Authorization Token does not have three parts");
        byte[] sig = base64UrlDecodeToBytes(parts[2]);
        if (alg.equals("ES256"))sig = convertECSigFromJwtToDer(sig); // the input to Sun's validator needs to be in DER format and needs to be converted from simple concatenation
        try {
            Signature signatureChecker = Signature.getInstance(alg.equals("ES256")?"SHA256withECDSA":"SHA256withRSA");
            signatureChecker.initVerify(key);
            signatureChecker.update((parts[0]+"."+parts[1]).getBytes());
            return signatureChecker.verify(sig);
        }
        catch (NoSuchAlgorithmException e) {throw new XDError("Algorithm not supported?");}
        catch (InvalidKeyException e) {throw new XDException(Errors.OAUTH_INVALID_TOKEN,"Public key for authorization token validation is invalid");}
        catch (SignatureException e) {throw new XDException(Errors.OAUTH_INVALID_TOKEN,"Couldn't validate authorization token signature");}
    }

    private static byte[] makeSignature(byte[] body, String alg, PrivateKey key) throws XDException {
        try {
            if (!(alg.equals("ES256")||alg.equals("RS256"))) throw new XDException(Errors.INCONSISTENT_VALUES,"unsupported token alg '"+alg+"'");
            Signature signatureMaker = Signature.getInstance(alg.equals("ES256")?"SHA256withECDSA":"SHA256withRSA");
            signatureMaker.initSign(key);
            signatureMaker.update(body);
            byte[] sig = signatureMaker.sign();
            if (alg.equals("ES256")) sig = convertECSigFromDerToJwt(sig); // the output of Sun's signature is in DER format and needs to be converted to simple concatenation
            return sig;
        }
        catch (InvalidKeyException e) { throw new XDException(Errors.VALUE_FORMAT,"Invalid private key format", e); }
        catch (NoSuchAlgorithmException e) { throw new XDError("Missing Implementation of 'SHA256withECDSA'", e); }
        catch (SignatureException e) { throw new XDException(Errors.VALUE_FORMAT,"Problem creating signature", e); }
    }

    public static byte[] base64UrlDecodeToBytes(String in) throws XDException {
        // To decode a string into an octet sequence:
        // (1) For the given string, replace "-" (dash) characters with "+" and replace "_" (underscore) characters with "/"
        // (2) If the string length modulo 4 is:
        //     (a) 0: do nothing, no padding needed
        //     (b) 2: add two "=" characters to end
        //     (c) 3: add one "=" character to end
        //     (d) 1: reject as invalid.
        // (3) Convert resulting string with standard base64 decoder to generate output octets.
        in = in.replace('-','+').replace('_','/');
        switch (in.length()%4) {
            case 0: break;
            case 2: in = in+"=="; break;
            case 3: in = in+"=";  break;
            case 1: throw new XDException(Errors.OAUTH_INVALID_TOKEN,"Base64Url encoding is incorrect length");
        }
        return DatatypeConverter.parseBase64Binary(in);
    }
    public static String base64UrlDecodeToString(String in) throws XDException {
        try { return new String(base64UrlDecodeToBytes(in),"UTF-8"); }
        catch (UnsupportedEncodingException e) { return "";} // don't worry, UTF-8 is supported!
    }

    public static String base64UrlEncode(String in) throws XDException {
        try { return base64UrlEncode(in.getBytes("UTF-8")); }
        catch (UnsupportedEncodingException e) {return ""; } // don't worry, UTF-8 is supported!
    }

    public static String base64UrlEncode(byte[] in) throws XDException {
        // To encode an octet sequence to a string:
        // (1) start with a standard base64 encoding of the input octets.
        // (2) remove trailing "=" characters
        // (3) replace "+" characters with "-" (dash) characters
        // (4) replace "/" characters with "_" (underscore) characters
        String out = DatatypeConverter.printBase64Binary(in);
        int i = out.indexOf('='); if (i != -1) out = out.substring(0,i);
        return out.replace('+','-').replace('/', '_');
    }


    // EC signatures from the java providers are a DER encoding in this format:
    //    ECDSASignature ::= SEQUENCE {
    //        R   INTEGER,
    //        S   INTEGER
    //    }
    // Example:
    //   48     tag SEQUENCE (always 0x30)
    //   69     length SEQUENCE (will either be 68, 69, or 70 based on length of R and S)
    //   2      tag INTEGER
    //   33     length INTEGER R (will either be 32 or 33)
    //   0      optional zero padding byte if first octet is negative (makes length 33)
    //   -20    first octet of R will be at offset 4 or 5 depending on presence of pad
    //   ...    31 more octets of R
    //   2      tag INTEGER
    //   32     length INTEGER S - will either be 32 or 33 (this is at offset 4+lengthR+1)
    //   14     first octet of S
    //   ...    31 more octets of S
    //
    // These need to be converted to/from a simple concatenation of R and S used by JWT

    public static byte[] convertECSigFromDerToJwt(byte der[]) {
        int lengthR = der[3];
        int lengthS = der[4+lengthR+1];
        int offsetR = (lengthR == 33) ? 5 : 4;
        int offsetS = (lengthS == 33) ? 4+lengthR+3 : 4+lengthR+2;
        byte sigBytes[] = new byte[64];
        System.arraycopy(der, offsetR, sigBytes,  0, 32);
        System.arraycopy(der, offsetS, sigBytes, 32, 32);
        return sigBytes;
    }

    public static byte[] convertECSigFromJwtToDer(byte sig[]) {
        int lengthR = sig[0] <0 ? 33 : 32;  // if the first byte of R is negative, then we need a padding byte
        int lengthS = sig[32]<0 ? 33 : 32;  // if the first byte of S is negative, then we need a padding byte
        byte der[] = new byte[2+2+lengthR+2+lengthS]; // size = SEQ tag/len + INT tag/len + R (32 or 33) + INT tag/len + S (32 or 33)
        int offset = 0;
        der[offset++] = 48;                                // SEQ tag = 0x30
        der[offset++] = (byte)(2 + lengthR + 2 + lengthS); // SEQ len = size of: INT tag/len + R + INT tag/len + S
        der[offset++] = 2;                                 // INT tag = 0x02
        der[offset++] = (byte)lengthR;                     // INT length
        if (lengthR == 33) der[offset++] = 0;              // padding if needed
        System.arraycopy(sig, 0, der, offset, 32);
        offset += 32;
        der[offset++] = 2;                                 // INT tag = 0x02
        der[offset++] = (byte)lengthS;                     // INT length
        if (lengthS == 33) der[offset++] = 0;              // padding if needed
        System.arraycopy(sig, 32, der, offset, 32);
        return der;
    }

}
