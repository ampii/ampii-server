// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.data.basetypes;

import org.ampii.xd.common.XDException;

/**
 * ParsedData adds parsing info (line, column, source name) to {@link PolyData}.
 * It is only for parsed data; it is not a base type and is not transmitted or stored in the datastore.
 *
 * @author drobin
 */
public class ParsedData extends PolyData {

    String sourceName;
    int    line;
    int    column;

    public String parsedFrom() {
        return sourceName + ", line "+line + ", column "+column;
    }

    public ParsedData(String name, String sourceName, int line, int column, Object... initializers) throws XDException {
        super(name, initializers);
        this.sourceName = sourceName != null? sourceName: "{internal}";
        this.line       = line;
        this.column     = column;
    }

}
