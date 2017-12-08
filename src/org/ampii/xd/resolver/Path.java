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

    public static boolean isFilePath(String path) throws XDException {
        // if server has no file prefix, everything is a file, except for "{dataPrefix}" (the data root) or "{dataPrefix}/..."
        // but if there *is* a file prefix, files must be either "{filePrefix}" or "{filePrefix}/..."
        if (Application.filePrefix.isEmpty())
            return !(path.equals(Application.dataPrefix) || path.startsWith(Application.dataPrefix + "/"));
        else
            return (path.equals(Application.filePrefix) || path.startsWith(Application.filePrefix + "/"));
    }

    public static String removeFilePrefix(String path) throws XDException {
        // if server has no file prefix, there's nothing to do
        // but if there *is* a file prefix, we turn "{filePrefix}" into "/" and "{filePrefix}/..." into "/..."
        if (!Application.filePrefix.isEmpty()) {
            if (path.equals(Application.filePrefix)) // "{prefix}" becomes "/"
                path = "/";
            else if (path.startsWith(Application.filePrefix + "/")) // "{prefix}/..." becomes   "/..."
                path = path.substring(Application.filePrefix.length());
        }
        return path;
    }
    public static String makeWebrootFilePath(String path) throws XDException {
        return Application.baseDir + "/" + Application.webroot + path;   // path must start with "/", basedir and webroot do not end with "/"
    }

    public static boolean isDataPath(String path) throws XDException {
        // if server has no data prefix, everything is data, except for "{filePrefix}" or "{filePrefix}/..."
        // but if there *is* a data prefix, data must be at either "{dataPrefix}" (the root) or "{dataPrefix}/..."
        if (Application.dataPrefix.isEmpty()) // server has no data prefix so data owns the root, except for "{filePrefix}" or "{filePrefix}/..."
            return !(path.equals(Application.filePrefix) || path.startsWith(Application.filePrefix + "/"));
        else
            return  (path.equals(Application.dataPrefix) || path.startsWith(Application.dataPrefix + "/"));
    }

    // // This is not used since the handling of prefixes is doen in the eval method.
    // // this makes creation of things like "self" and "next" safer
    // public static String removeDataPrefix(String path) throws XDException {
    //    // if server has no data prefix, there's nothing to do
    //    // but if there *is* a data prefix, we turn "/{prefix}" into "/" and "/{prefix}/..." into "/..."
    //    if (!Application.dataPrefix.isEmpty()) {
    //        if (path.equals("/" + Application.dataPrefix)) // "/{prefix}" becomes "/"
    //            path = "/"+path.substring(Application.dataPrefix.length()+1);
    //        else if (path.startsWith("/" + Application.dataPrefix + "/")) // "/{prefix}/..." becomes   "/..."
    //            path = "/"+path.substring(Application.dataPrefix.length()+2);
    //    }
    //    return path;
    //}

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
