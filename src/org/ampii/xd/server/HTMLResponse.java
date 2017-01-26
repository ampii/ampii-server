// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

/**
 * An HTTP response containing HTML formatted text
 */
public class HTMLResponse extends Response {

    public HTMLResponse(String status, String body)  {
        this.status = status;
        this.contentType = "text/html";
        this.body = body;
    }


}
