// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.common;

import org.ampii.xd.server.HTTP;

public class Errors {
    public static final int OTHER                     = 0;
    public static final int NOT_AUTHENTICATED         = 1;
    public static final int NOT_AUTHORIZED            = 2;
    public static final int PARAM_SYNTAX              = 3;
    public static final int PARAM_NOT_SUPPORTED       = 4;
    public static final int PARAM_VALUE_FORMAT        = 5;
    public static final int PARAM_OUT_OF_RANGE        = 6;
    public static final int LOCALE_NOT_SUPPORTED      = 7;
    public static final int PATH_SYNTAX               = 8;
    public static final int DATA_NOT_FOUND            = 9;
    public static final int METADATA_NOT_FOUND        = 10;
    public static final int ILLEGAL_METADATA          = 11;
    public static final int VALUE_FORMAT              = 12;
    public static final int VALUE_OUT_OF_RANGE        = 13;
    public static final int INDEX_OUT_OF_RANGE        = 14;
    public static final int NOT_WRITABLE              = 15;
    public static final int WRITE_FAILED              = 16;
    public static final int LIST_OF_PATHS_IS_EMPTY    = 17;
    public static final int COUNT_IS_ZERO             = 18;
    public static final int INTERVAL_IS_ZERO          = 19;
    public static final int NO_HISTORY                = 20;
    public static final int NO_DATA_AVAILABLE         = 21;
    // number 22 is not used                          = 22;
    public static final int NOT_AN_ARRAY              = 23;
    public static final int COMMUNICATION_FAILED      = 24;
    // number 25 is not used                          = 25;
    public static final int TLS_CONFIG                = 26;
    public static final int NOT_REPRESENTABLE         = 27;
    public static final int BAD_METHOD                = 28;
    public static final int TOO_LARGE                 = 29;
    public static final int TOO_DEEP                  = 30;
    public static final int CANNOT_CREATE             = 31;
    public static final int CANNOT_DELETE             = 32;
    public static final int AUTH_EXPIRED              = 33;
    public static final int AUTH_INVALID              = 34;
    public static final int MISSING_PARAMETER         = 35;
    public static final int UNSUPPORTED_MEDIA_TYPE    = 36;
    public static final int UNSUPPORTED_DATATYPE      = 37;
    public static final int INVALID_DATATYPE          = 38;
    public static final int INCONSISTENT_VALUES       = 39;
    public static final int EXPIRED_LINK              = 40;
    public static final int NOT_READABLE              = 41;
    public static final int DUPLICATES_NOT_ALLOWED    = 42;
    public static final int UNINITIALIZED             = 43;
    public static final int EXPIRED_CONTEXT           = 44;
    public static final int NOT_ATOMIC                = 45;
    public static final int CANNOT_FOLLOW             = 46;
    // WHEN ADDING NEW... update all the functions below too!

    // End of standard errors

    public static final int INTERNAL_TIMEOUT          = 1024;
    public static final int INTERNAL_ERROR            = 1025;
    public static final int FUNCTION_NAME             = 1026;
    public static final int FUNCTION_TARGET           = 1027;
    public static final int ARG_SYNTAX                = 1028;
    public static final int ARG_NOT_SUPPORTED         = 1029;
    public static final int ARG_VALUE_FORMAT          = 1030;
    public static final int ARG_OUT_OF_RANGE          = 1031;
    public static final int TARGET_DATATYPE           = 1032;
    public static final int CANNOT_HAVE_CHILDREN      = 1033;
    public static final int BAD_LINK_TARGET           = 1034;
    public static final int OAUTH_INVALID_REQUEST     = 1035;
    public static final int OAUTH_INVALID_TOKEN       = 1036;
    public static final int OAUTH_INSUFFICIENT_SCOPE  = 1037;
    public static final int CALLBACK_FAILED           = 1038;
    public static final int CLIENT_ACTION_FAILED      = 1039;
    public static final int CANNOT_HAVE_VALUE         = 1040;
    public static final int TEST_FAILURE              = 1041;
    // WHEN ADDING NEW... update all the functions below, and "AMPII Definitions.xml"


    public static String statusLineForErrorNumber(int error) {
        switch (error) {
            case OTHER:                  return HTTP.HTTP_500_INTERNALERROR;
            case NOT_AUTHENTICATED:      return HTTP.HTTP_401_UNAUTHORIZED;
            case NOT_AUTHORIZED:         return HTTP.HTTP_401_UNAUTHORIZED;
            case PARAM_SYNTAX:           return HTTP.HTTP_400_BADREQUEST;
            case PARAM_NOT_SUPPORTED:    return HTTP.HTTP_403_FORBIDDEN;
            case PARAM_VALUE_FORMAT:     return HTTP.HTTP_400_BADREQUEST;
            case PARAM_OUT_OF_RANGE:     return HTTP.HTTP_403_FORBIDDEN;
            case LOCALE_NOT_SUPPORTED:   return HTTP.HTTP_403_FORBIDDEN;
            case PATH_SYNTAX:            return HTTP.HTTP_400_BADREQUEST;
            case DATA_NOT_FOUND:         return HTTP.HTTP_404_NOTFOUND;
            case METADATA_NOT_FOUND:     return HTTP.HTTP_404_NOTFOUND;
            case ILLEGAL_METADATA:       return HTTP.HTTP_404_NOTFOUND;
            case VALUE_FORMAT:           return HTTP.HTTP_400_BADREQUEST;
            case VALUE_OUT_OF_RANGE:     return HTTP.HTTP_403_FORBIDDEN;
            case INDEX_OUT_OF_RANGE:     return HTTP.HTTP_403_FORBIDDEN;
            case NOT_WRITABLE:           return HTTP.HTTP_403_FORBIDDEN;
            case WRITE_FAILED:           return HTTP.HTTP_403_FORBIDDEN;
            case LIST_OF_PATHS_IS_EMPTY: return HTTP.HTTP_403_FORBIDDEN;
            case COUNT_IS_ZERO:          return HTTP.HTTP_403_FORBIDDEN;
            case INTERVAL_IS_ZERO:       return HTTP.HTTP_403_FORBIDDEN;
            case NO_HISTORY:             return HTTP.HTTP_403_FORBIDDEN;
            case NO_DATA_AVAILABLE:      return HTTP.HTTP_403_FORBIDDEN;
            case NOT_AN_ARRAY:           return HTTP.HTTP_403_FORBIDDEN;
            case COMMUNICATION_FAILED:   return HTTP.HTTP_403_FORBIDDEN;
            case NOT_REPRESENTABLE:      return HTTP.HTTP_403_FORBIDDEN;
            case BAD_METHOD:             return HTTP.HTTP_405_BADMETHOD;
            case TOO_LARGE:              return HTTP.HTTP_403_FORBIDDEN;
            case TOO_DEEP:               return HTTP.HTTP_403_FORBIDDEN;
            case TLS_CONFIG:             return HTTP.HTTP_403_FORBIDDEN;
            case CANNOT_CREATE:          return HTTP.HTTP_403_FORBIDDEN;
            case CANNOT_DELETE:          return HTTP.HTTP_403_FORBIDDEN;
            case AUTH_EXPIRED:           return HTTP.HTTP_403_FORBIDDEN;
            case AUTH_INVALID:           return HTTP.HTTP_403_FORBIDDEN;
            case MISSING_PARAMETER:      return HTTP.HTTP_403_FORBIDDEN;
            case UNSUPPORTED_MEDIA_TYPE: return HTTP.HTTP_403_FORBIDDEN;
            case UNSUPPORTED_DATATYPE:   return HTTP.HTTP_403_FORBIDDEN;
            case INVALID_DATATYPE:       return HTTP.HTTP_403_FORBIDDEN;
            case INCONSISTENT_VALUES:    return HTTP.HTTP_403_FORBIDDEN;
            case EXPIRED_LINK:           return HTTP.HTTP_403_FORBIDDEN;
            case NOT_READABLE:           return HTTP.HTTP_403_FORBIDDEN;
            case DUPLICATES_NOT_ALLOWED: return HTTP.HTTP_403_FORBIDDEN;
            case UNINITIALIZED:          return HTTP.HTTP_403_FORBIDDEN;
            case EXPIRED_CONTEXT:        return HTTP.HTTP_403_FORBIDDEN;
            case NOT_ATOMIC:             return HTTP.HTTP_403_FORBIDDEN;
            case CANNOT_FOLLOW:          return HTTP.HTTP_403_FORBIDDEN;

            // end of standard errors
            case INTERNAL_TIMEOUT:         return HTTP.HTTP_500_INTERNALERROR;
            case INTERNAL_ERROR:           return HTTP.HTTP_500_INTERNALERROR;
            case FUNCTION_NAME:            return HTTP.HTTP_400_BADREQUEST;
            case FUNCTION_TARGET:          return HTTP.HTTP_403_FORBIDDEN;
            case ARG_SYNTAX:               return HTTP.HTTP_400_BADREQUEST;
            case ARG_NOT_SUPPORTED:        return HTTP.HTTP_403_FORBIDDEN;
            case ARG_VALUE_FORMAT:         return HTTP.HTTP_400_BADREQUEST;
            case ARG_OUT_OF_RANGE:         return HTTP.HTTP_403_FORBIDDEN;
            case TARGET_DATATYPE:          return HTTP.HTTP_403_FORBIDDEN;
            case CANNOT_HAVE_CHILDREN:     return HTTP.HTTP_403_FORBIDDEN;
            case BAD_LINK_TARGET:          return HTTP.HTTP_403_FORBIDDEN;
            case OAUTH_INVALID_REQUEST:    return HTTP.HTTP_400_BADREQUEST;
            case OAUTH_INVALID_TOKEN:      return HTTP.HTTP_401_UNAUTHORIZED;
            case OAUTH_INSUFFICIENT_SCOPE: return HTTP.HTTP_403_FORBIDDEN;
            case CALLBACK_FAILED:          return HTTP.HTTP_500_INTERNALERROR; // shouldn't get this in a response
            case CLIENT_ACTION_FAILED:     return HTTP.HTTP_500_INTERNALERROR; // shouldn't get this in a response
            case CANNOT_HAVE_VALUE:        return HTTP.HTTP_403_FORBIDDEN;
            case TEST_FAILURE:             return HTTP.HTTP_403_FORBIDDEN;     // shouldn't get this in a response

            default:                       return HTTP.HTTP_500_INTERNALERROR;
        }
    }

    public static int statusCodeForErrorNumber(int error) {
        switch (error) {
            case OTHER:                  return 500;
            case NOT_AUTHENTICATED:      return 401;
            case NOT_AUTHORIZED:         return 401;
            case PARAM_SYNTAX:           return 400;
            case PARAM_NOT_SUPPORTED:    return 403;
            case PARAM_VALUE_FORMAT:     return 400;
            case PARAM_OUT_OF_RANGE:     return 403;
            case LOCALE_NOT_SUPPORTED:   return 403;
            case PATH_SYNTAX:            return 400;
            case DATA_NOT_FOUND:         return 404;
            case METADATA_NOT_FOUND:     return 404;
            case ILLEGAL_METADATA:       return 404;
            case VALUE_FORMAT:           return 400;
            case VALUE_OUT_OF_RANGE:     return 403;
            case INDEX_OUT_OF_RANGE:     return 403;
            case NOT_WRITABLE:           return 403;
            case WRITE_FAILED:           return 403;
            case LIST_OF_PATHS_IS_EMPTY: return 403;
            case COUNT_IS_ZERO:          return 403;
            case INTERVAL_IS_ZERO:       return 403;
            case NO_HISTORY:             return 403;
            case NO_DATA_AVAILABLE:      return 403;
            case NOT_AN_ARRAY:           return 403;
            case COMMUNICATION_FAILED:   return 403;
            case NOT_REPRESENTABLE:      return 403;
            case BAD_METHOD:             return 405;
            case TOO_LARGE:              return 403;
            case TOO_DEEP:               return 403;
            case TLS_CONFIG:             return 403;
            case CANNOT_CREATE:          return 403;
            case CANNOT_DELETE:          return 403;
            case AUTH_EXPIRED:           return 403;
            case AUTH_INVALID:           return 403;
            case MISSING_PARAMETER:      return 403;
            case UNSUPPORTED_MEDIA_TYPE: return 403;
            case UNSUPPORTED_DATATYPE:   return 403;
            case INVALID_DATATYPE:       return 403;
            case INCONSISTENT_VALUES:    return 403;
            case EXPIRED_LINK:           return 403;
            case NOT_READABLE:           return 403;
            case DUPLICATES_NOT_ALLOWED: return 403;
            case UNINITIALIZED:          return 403;
            case EXPIRED_CONTEXT:        return 403;
            case NOT_ATOMIC:             return 403;
            case CANNOT_FOLLOW:          return 403;

            // end of standard errors
            case INTERNAL_TIMEOUT:         return 500;
            case INTERNAL_ERROR:           return 500;
            case FUNCTION_NAME:            return 400;
            case FUNCTION_TARGET:          return 403;
            case ARG_SYNTAX:               return 400;
            case ARG_NOT_SUPPORTED:        return 403;
            case ARG_VALUE_FORMAT:         return 400;
            case ARG_OUT_OF_RANGE:         return 403;
            case TARGET_DATATYPE:          return 403;
            case CANNOT_HAVE_CHILDREN:     return 403;
            case BAD_LINK_TARGET:          return 403;
            case OAUTH_INVALID_REQUEST:    return 400;
            case OAUTH_INVALID_TOKEN:      return 401;
            case OAUTH_INSUFFICIENT_SCOPE: return 403;
            case CALLBACK_FAILED:          return 500; // shouldn't get this in a response
            case CLIENT_ACTION_FAILED:     return 500; // shouldn't get this in a response
            case CANNOT_HAVE_VALUE:        return 403;
            case TEST_FAILURE:             return 403; // shouldn't get this in a response

            default:                       return 500;
        }
    }

    public static String textForErrorNumber(int error) {
        switch (error) {
            case OTHER:                    return "Unspecified error";
            case NOT_AUTHENTICATED:        return "Not authenticated";
            case NOT_AUTHORIZED:           return "Not authorized";
            case PARAM_SYNTAX:             return "Bad parameter syntax";
            case PARAM_NOT_SUPPORTED:      return "Parameter not supported";
            case PARAM_VALUE_FORMAT:       return "Bad parameter value format";
            case PARAM_OUT_OF_RANGE:       return "Parameter out of range";
            case LOCALE_NOT_SUPPORTED:     return "Locale Not Supported";
            case PATH_SYNTAX:              return "Bad path syntax";
            case DATA_NOT_FOUND:           return "Data not found";
            case METADATA_NOT_FOUND:       return "Metadata not found";
            case ILLEGAL_METADATA:         return "Illegal metadata";
            case VALUE_FORMAT:             return "Bad value format";
            case VALUE_OUT_OF_RANGE:       return "Value out of range";
            case INDEX_OUT_OF_RANGE:       return "Index out of range";
            case NOT_WRITABLE:             return "Not writable";
            case WRITE_FAILED:             return "Write failed";
            case LIST_OF_PATHS_IS_EMPTY:   return "List of paths is empty";
            case COUNT_IS_ZERO:            return "Requested count is zero";
            case INTERVAL_IS_ZERO:         return "Requested interval is zero";
            case NO_HISTORY:               return "No history";
            case NO_DATA_AVAILABLE:        return "No data available";
            case NOT_AN_ARRAY:             return "Not an array";
            case COMMUNICATION_FAILED:     return "Comm with the remote device failed";
            case NOT_REPRESENTABLE:        return "Data not representable in requested format";
            case BAD_METHOD:               return "Method not allowed for this data";
            case TOO_LARGE:                return "Data is too big to return";
            case TOO_DEEP:                 return "Data is too big to return";
            case TLS_CONFIG:               return "TLS certs and keys are bad, missing, or not consistent";
            case CANNOT_CREATE:            return "Creation of data or metadata not possible";
            case CANNOT_DELETE:            return "Deletion of data or metadata not possible";
            case AUTH_EXPIRED:             return "Authorization Expired";
            case AUTH_INVALID:             return "Authorization Invalid";
            case MISSING_PARAMETER:        return "Missing Parameter";
            case UNSUPPORTED_MEDIA_TYPE:   return "Unsupported Mediat Type";
            case UNSUPPORTED_DATATYPE:     return "Unsupported Data Type";
            case INVALID_DATATYPE:         return "Invalid Data Type";
            case INCONSISTENT_VALUES:      return "Inconsistent Values";
            case EXPIRED_LINK:             return "Expired Link";
            case NOT_READABLE:             return "Not Readable";
            case DUPLICATES_NOT_ALLOWED:   return "Duplicates Not Allowed";
            case UNINITIALIZED:            return "Uninitialized";
            case EXPIRED_CONTEXT:          return "Expired Context";
            case NOT_ATOMIC:               return "Not Atomic";
            case CANNOT_FOLLOW:            return "Cannot Follow";
            // end of standard errors
            case INTERNAL_TIMEOUT:         return "Internal Timeout";
            case INTERNAL_ERROR:           return "Internal Error";
            case FUNCTION_NAME:            return "Invalid function name";
            case FUNCTION_TARGET:          return "Invalid target for function";
            case ARG_SYNTAX:               return "Bad function argument syntax";
            case ARG_NOT_SUPPORTED:        return "Function argument not supported";
            case ARG_VALUE_FORMAT:         return "Bad function argument format";
            case ARG_OUT_OF_RANGE:         return "Function argument out of range";
            case TARGET_DATATYPE:          return "Inappropriate target datatype";
            case CANNOT_HAVE_CHILDREN:     return "Cannot have children";
            case BAD_LINK_TARGET:          return "Bad or unsupported Link target";
            case OAUTH_INVALID_REQUEST:    return "OAuth request is malformed";
            case OAUTH_INVALID_TOKEN:      return "OAuth token is expired, malformed, or invalid";
            case OAUTH_INSUFFICIENT_SCOPE: return "OAuth token does not have sufficient scope for the data";
            case CALLBACK_FAILED:          return "Callback Failed";
            case CLIENT_ACTION_FAILED:     return "Client Action Failed";
            case CANNOT_HAVE_VALUE:        return "Cannot have Value";
            case TEST_FAILURE:             return "Test Failure";

            default:                       return "Unknown Error";
        }
    }

}
