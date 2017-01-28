// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.common;

import org.ampii.xd.data.Data;
import org.ampii.xd.data.Context;

/**
 * XDError is just an Error wrapper for an XDException that is used to throw unexpected internal errors.
 *
 * @author daverobin
 */
public class XDError extends Error {

  public XDException exception;

  public XDError(String message, Object... extras)                          { exception = new XDException(Errors.INTERNAL_ERROR,null,  null, null, message,extras); }
  public XDError(Data target, String message, Object... extras)             { exception = new XDException(Errors.INTERNAL_ERROR,target,null, null, message,extras); }
  public XDError(Data target, Data given, String message, Object... extras) { exception = new XDException(Errors.INTERNAL_ERROR,target,given,null, message,extras); }
  public XDError(Data target, Context context, String message, Object... extras) { exception = new XDException(Errors.INTERNAL_ERROR,target,null,context,message,extras); }
  public XDError(Data target, Data given, Context context, String message, Object... extras) { exception = new XDException(Errors.INTERNAL_ERROR,target,given,context,message,extras); }

  public String toString()   { return exception.toString(); }
  public String getMessage() { return exception.getLocalizedMessage(); }

}
