// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.functions;

import org.ampii.xd.application.hooks.FunctionHooks;
import org.ampii.xd.common.XDException;
import org.ampii.xd.common.Errors;
import org.ampii.xd.data.Data;
import org.ampii.xd.data.Context;

import java.net.URLDecoder;

/**
 * Dispatch handler for URI functions like .../exists() and .../contains().
 * <p>
 * This class has a hard-coded lists of built-in function names, but external code can inject new functions by
 * using {@link FunctionHooks}.
 *
 * @author daverobin
 */
public class Functions {
    public static Data invoke(Data target, String function, String argString, Context context) throws XDException {
        // first check for application-specific extensions
        Data result = FunctionHooks.invoke(target,function,argString,context);
        if (result != null) return result;
        // this lookup is trivial and brute force for now... it can get more sophisticated if we get a lot of these
        switch(function) {
            case "contains":        return ContainsFunction.invoke(target, argString);
            case "startsWith":      return StartsWithFunction.invoke(target, argString);
            case "endsWith":        return EndsWithFunction.invoke(target, argString);
            case "historyPeriodic": return HistoryPeriodicFunction.invoke(target, argString);
            case "tagged":          return TaggedFunction.invoke(target, argString);
            case "exists":          return ExistsFunction.invoke(target, argString, context);
            case "remote":          return RemoteFunction.invoke(target, argString, context);
            default:                throw new XDException(Errors.FUNCTION_NAME,"Invalid function name: "+function);
        }
    }

    public static void parseArgs(String argString, Data... argData) throws XDException {
        String[] splitArgs = argString.split(",");
        int      position = 0;
        boolean  doingPositional = true;
        for (String arg : splitArgs) {
            if (arg.contains("=")) {
                doingPositional = false;
                String[] pair = arg.split("=");
                if (pair.length!=2) throw new XDException(Errors.ARG_SYNTAX,"In function call, malformed argument: '"+arg+"'");
                String argName = unencode(pair[0].trim());
                String argValue = unencode(pair[1].trim());
                boolean found = false;
                for (int i=0; i< argData.length; i++) {
                    if (argData[i].getName().equals(argName)) {
                        argData[i].setValue(argValue);
                        found = true;
                        break;
                    }
                }
                if (!found) throw new XDException(Errors.ARG_NOT_SUPPORTED,"In function call, named argument '"+argName+"' not valid");
            }
            else if (doingPositional) {
                if (position >= argData.length) throw new XDException(Errors.ARG_SYNTAX,"Too many arguments for function");
                argData[position++].setValue(unencode(arg.trim()));
            }
            else throw new XDException(Errors.ARG_SYNTAX,"In function call, found positional argument '" + arg + "' after named argument(s)");
        }
    }

    private static String unencode(String string) throws XDException {
        try { return URLDecoder.decode(string,"UTF-8"); }
        catch (Exception e) { throw new XDException(Errors.ARG_SYNTAX,"bad % encoding in argument"); }
    }
}
