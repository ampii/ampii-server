// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.resolver;

import org.ampii.xd.application.Application;
import org.ampii.xd.common.XDException;
import org.ampii.xd.data.Data;
import org.ampii.xd.server.Server;

/**
 * Static methods for working with URI paths.
 */
public class Path {

    public static String getPathName(Data data)  {
        if (data.getName().equals(Application.rootName)) return Application.dataPrefix.isEmpty()?"/.":Application.dataPrefix;
        return data.getName();
    }

    public static String toURI(Data data) throws XDException { return Server.getHttpBaseServerURI()+toPath(data); }

    public static String toPath(Data data)  { return toPath(data,false); }

    public static String toPath(Data data, boolean annotateFakeParents)  {
        if (data.getName().equals(Application.rootName)) return getPathName(data);
        return toRelativePath(null, data, annotateFakeParents);
    }

    public static String toRelativePath(Data base, Data data)  { return toRelativePath(base, data, false); }

    public static String toRelativePath(Data base, Data data, boolean annotateFakeParents)  {
        if (base == data) return ".";
        StringBuilder builder = new StringBuilder();
        builder.append(getPathName(data));
        if (data.hasParent()) for( ; data.hasParent(); data = data.getParent()) {
            Data parent = data.getParent();
            if (parent == base)
                break;
            if (parent.getName().equals(Application.rootName)) {
                builder.insert(0, Application.dataPrefix + (isFakeSub(data)&&annotateFakeParents?"{/}":"/"));
                break;
            }
            builder.insert(0,getPathName(data.getParent())+(isFakeSub(data)&&annotateFakeParents?"{/}":"/"));
        }
        return builder.toString();
    }

    private static boolean isFakeSub(Data data) { return data.hasParent() && (data.getParent().findLocal(data.getName())==null); }

    public static boolean isFilePath(String path) throws XDException { return !isDataPath(path); }
    public static boolean isDataPath(String path) throws XDException {
        // if there is no data prefix, then data owns the root and files are subjugated to needing a prefix
        // but if there *is* a data prefix, then files own the root and data is subjugated to a prefix
        if (Application.dataPrefix.isEmpty()) return !(path.startsWith(Application.filePrefix + "/"));
        else return path.equals(Application.dataPrefix) || path.startsWith(Application.dataPrefix + "/");
    }

    public static String makeLegalPathName(String candidate) {
        StringBuilder result = new StringBuilder(candidate.length());
        for (int i=0; i<candidate.length(); i++) { // can't be: / \ : ; | < > * ? " [ ] { } ( ) or $ as first char
            char c = candidate.charAt(i);
            if (c < 0x20 || c == '/' || c == '\\' || c == ':' || c == ';' || c == '|' || c == '<' ||
                c == '>' || c == '*' || c == '?' || c == '"'  || c == '[' || c == ']' || c == '{' || c == '}' ||
                c == '(' || c == ')' || (c == '$' && i == 0)) {
                c = '_';
            }
            result.append(c);
        }
        return result.toString();
    }


}
