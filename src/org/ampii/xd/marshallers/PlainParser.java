// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.marshallers;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.definitions.DefinitionCollector;
import java.io.Reader;
import java.net.URL;

/**
 * This is pretty simple, but it was jealous of XMLGenerator and JSONGenerator, so it gets its own formal class.
 *
 * @author drobin
 */
public class PlainParser extends Parser {

    // this is comically overkill for this task, but it's nicely symmetric with the other parsers
    public Data parse(Reader reader, URL sourceURL, String sourceName, int options, DefinitionCollector definitionCollector) throws XDException {
        begin(reader, sourceURL, sourceName, 0);
        Data info = makeParsedData("{plaintext}");
        info.setValue(consumeUntilEnd());  // consume the whole body literally. no quoting or escaping is used for alt=plain
        finish();
        return info;
    }

}
