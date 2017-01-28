// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.marshallers;

import org.ampii.xd.common.XDException;
import org.ampii.xd.data.*;
import org.ampii.xd.data.Context;
import java.io.Writer;

/**
 * Outputs values in plain text for alt=plain.
 *
 * @author daverobin
 */
public class PlainGenerator  extends Generator {

    public void generate(Writer writer, Data data) throws XDException {
        Context context = data.getContext();
        begin(writer, context);
        if (data.canHaveValue()) {
            if (data.booleanValueOf(Meta.UNSPECIFIEDVALUE, false)) { // if it has a "fully unspecified" value, it gets special processing
                switch (data.getBase()) {
                    case DATE:     emit("----/--/--");
                    case DATETIME: emit("----/--/--T--:--:--Z");
                    case TIME:     emit("--:--:--Z");
                    default:       emit("? missing value.");
                }
            }
            else {
                emit(data.getContextualizedValue());
            }
    }
        else emit("? non-primitive value not available as plain text.");
        finish();
    }

}
