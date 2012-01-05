/* Copyright (c) 2012, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.vortikal.repository.media;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.store.Metadata;

public class MediaMetadataManagerImpl implements MediaMetadataManager {

    private String repositoryDataDirectory;

    /* Hasty implementation for testing purposes - to be moved to vortex */
    public MediaImage getThumbnail(Path path, int width, String resourceType, boolean scaleUp) {

        try {
            URL url = new URL("http://localhost:6666" + repositoryDataDirectory + path.toString()
                    + "?action=thumbnail&fileType=" + resourceType + "&width=" + width);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(1000 * 120); // cool?
            String contentType = connection.getContentType();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                return new MediaImageImpl(contentType, in);

        } catch (Exception e) {
            // return null;

            // TODO: log
        }
        return null;
    }

    @Override
    public Metadata getMetadata(Path path, String fileType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MediaImage getVideoScreenshot(Path path, int maxWidth, int maxHeight, int seconds) {
        // TODO Auto-generated method stub
        return null;
    }

    
    @Required
    public void setRepositoryDataDirectory(String repositoryDataDirectory) {
        this.repositoryDataDirectory = repositoryDataDirectory;
    }
}
