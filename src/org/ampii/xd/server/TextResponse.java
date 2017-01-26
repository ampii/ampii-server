// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

//

/**
 * An HTTP response containing plain unformatted text.
 *
 * @author drobin
 */
public class TextResponse extends Response {

    public TextResponse(String status, String body)  {
        this.status = status;
        this.contentType = "text/plain";
        this.body = body;
    }

}
