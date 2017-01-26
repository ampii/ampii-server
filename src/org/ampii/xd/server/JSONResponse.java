// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

import org.ampii.xd.marshallers.JSONGenerator;

/**
 * An HTTP response containing JSON data
 */
public class JSONResponse extends Response {

    public JSONResponse(String status, String body)  {
        this.status = status;
        this.contentType = "application/json;charset=UTF-8";
        this.body = body;
    }

    public JSONResponse(String status, Object... pairs) {
        contentType = "application/json";
        this.status = status;
        body = JSONGenerator.generatePairs(pairs);
    }


}
