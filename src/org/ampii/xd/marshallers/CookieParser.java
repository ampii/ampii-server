// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.marshallers;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.basetypes.CollectionData;
import org.ampii.xd.data.basetypes.StringData;
import java.io.*;

/**
 * Helper class for consuming HTTP cookies (used by {@link org.ampii.xd.ui.Playground} for web session management)
 */
public class CookieParser extends Parser {

    public Data parse(String string) throws XDException {
        begin(new StringReader(string), null, "cookie", 0);
        Data result = consumeCookie();
        finish();
        return result;
    }

    private Data consumeCookie() throws XDException {
        Data cookie = new CollectionData("cookie");
        while (peekNextNonwhite() != 0) {
            String name = consumeUntil("=;");
            String value = "";  // value is optional
            if (peekNextNonwhite() == '=') { expect('='); value = consumeUntil(";"); }
            cookie.addLocal(new StringData(name, value));
            skipWhitespace();
            if (peekNextNonwhite() == ';') expect(';');
        }
        return cookie;
    }

}
