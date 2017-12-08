// This file is part of the AMPII Project. It is subject to the copyright and license terms in the top-level LICENSE file.
package org.ampii.xd.server;

import org.ampii.xd.application.Application;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

/**
 * An HTTP response containing a file's contents (either binary or textual).
 *
 * @author daverobin
 */
public class FileResponse extends Response {

    public FileResponse(String filePath)  {
        this.status = HTTP.HTTP_200_OK;
        this.fromFile = true;
        File file = new File(filePath);
        if (file.isDirectory()) { // we can't return a directory, so do the normal trick of trying to turn it into index.html
            filePath = filePath + "/index.html";
            file = new File(filePath);
        }
        String filePathLower = filePath.toLowerCase();
        boolean text = true;
        // yes, we could make this data-driven with config files, something like .htaccess, etc.
        // that was tried and was deemed too complicated for this simple server.
        if      (filePathLower.endsWith(".js"))   { text = true;  this.contentType = "application/javascript"; }
        else if (filePathLower.endsWith(".html")) { text = true;  this.contentType = "text/html"; }
        else if (filePathLower.endsWith(".txt"))  { text = true;  this.contentType = "text/plain"; }
        else if (filePathLower.endsWith(".pdf"))  { text = false; this.contentType = "application/pdf"; }
        else if (filePathLower.endsWith(".zip"))  { text = false; this.contentType = "application/zip"; }
        else if (filePathLower.endsWith(".gif"))  { text = false; this.contentType = "image/gif"; }
        else if (filePathLower.endsWith(".jpg"))  { text = false; this.contentType = "image/jpeg"; }
        else                                      { text = false; this.contentType = null; } // rather than aborting, we'll just leave it out and let the client figure it out
        try {
            if (text) this.body = new String(Files.readAllBytes(file.toPath()));
            else      this.body = Files.readAllBytes(file.toPath());
        }
        catch (FileNotFoundException e) {
            this.status = HTTP.HTTP_404_NOTFOUND;
            this.body = "Resource not found.  Were you looking for BACnet/WS data? The prefix for data on this server is '"+Application.dataPrefix+"'." ;
        }
        catch (IOException e) {
            this.status = HTTP.HTTP_404_NOTFOUND;
            this.body = "Resource not found or can't be read.  Were you looking for BACnet/WS data? The prefix for data on this server is '"+Application.dataPrefix+"'." ;
        }
    }

}
