// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.marshallers;

import org.ampii.xd.common.Log;
import org.ampii.xd.data.Context;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Base class for XML and JSON generators (there isn't much in common, but what <i>is</i> common is here).
 */
public class  Generator {

    private Writer  writer;  // private forces use of begin(writer,context,authorizer);

    Context     context;
    int         indentLevel;
    boolean     first;
    ArrayList<Boolean> newline;

    void begin(Writer writer, Context context) {
        this.writer = writer;
        this.context = context;
        newline = new ArrayList<>();
        setNewline(false);
        indentLevel = 0;
        context.cur_depth = 0;
        first = true;
    }
    void finish() {
        try { writer.flush(); }
        catch (IOException e) { Log.logSevere("Generator IOException: " + e.getLocalizedMessage()); }
    }

    void emitWithIndent(String text) {
        for (int j=0; j< indentLevel; j++) emit("   ");
        emit(text);
    }
    void emit(String text) {
        try { writer.write(text); }
        catch (IOException e)  { Log.logSevere("Generator IOException: " + e.getLocalizedMessage()); }
    }

    void emitWithOptionalIndent(String text, boolean indent) {
        if (indent) emitWithIndent(text);
        else emit(text);
    }

    void setNewline(boolean need) {
        if (indentLevel +1>newline.size()) newline.add(need);
        else newline.set(indentLevel, need);
    }

    boolean checkNewline()  {
        return newline.get(indentLevel);
    }

}
