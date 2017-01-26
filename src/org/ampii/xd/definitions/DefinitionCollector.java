// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.definitions;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.marshallers.DataParser;
import org.ampii.xd.test.Test;

/**
 * This is given to parsers as a callback for them to process found definitions.
 * <p>
 * If no DefinitionCollector is provided to a parser, and definitions are found, that's an error and it will complain.
 * {@link Definitions#getSystemDefinitionCollector()} can be used if you are wanting to inject definitions directly into the
 * system-wide list.  Or you can make up your own, like the "rejector" {@link DataParser#definitionRejector} or the
 * temporary client-side collector in {@link Test}.
 *
 * @author drobin
 */
public interface DefinitionCollector {
    void addDefinition(Data definition) throws XDException;
    void addTagDefinition(Data definition) throws XDException;
}
