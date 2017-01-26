// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

/**
 *  An HTTP response containing XML data.
 *
 *  @author drobin
 */
public class XMLResponse extends Response {

    public XMLResponse(String status, String body)  {
        this.status = status;
        this.contentType = "application/xml;charset=UTF-8";
        this.body = body;
    }
}
