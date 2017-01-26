// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

/**
 * A general alt=media response containing a media item's contents (either binary or textual)
 */
public class MediaResponse extends Response {

    public MediaResponse(String status, String mediaType, Object body)  {
        this.status = status;
        this.contentType = mediaType;
        this.body = body;
    }

}
