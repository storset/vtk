/* Copyright (c) 2013, University of Oslo, Norway
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

package org.vortikal.repository.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.sf.json.JSONObject;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.store.ContentStore;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.video.rest.VideoRef;

/**
 * Special implementation of {@link Content} for videoref resource type.
 */
public class VideoRefContent implements Content {

    private final Resource resource;
    private final Content defaultContent;
    private final ContentStore defaultContentStore;
    private final String videoRefJson;
    private final VideoRef videoRef;
    
    public VideoRefContent(Resource resource, Content defaultContent,
            ContentStore defaultContentStore) throws IOException {
        this.resource = resource;
        this.defaultContent = defaultContent;
        this.defaultContentStore = defaultContentStore;
        this.videoRefJson = StreamUtil.streamToString(
                this.defaultContentStore.getInputStream(this.resource.getURI()), "utf-8");
        this.videoRef = VideoRef.fromJsonString(this.videoRefJson).build();
    }
    
    @Override
    public Object getContentRepresentation(Class clazz) throws Exception {
        if (clazz == net.sf.json.JSONObject.class) {
            // Return VideoRef JSON content
            return JSONObject.fromObject(this.videoRefJson);
        }
        if (clazz == java.io.InputStream.class) {
            return getContentInputStream();
        }

        // TODO Possibly delegate to defaultContent.
        //      (would in that case need to override the defaultContentStore used by defaultContent)
//        return this.defaultContent.getContentRepresentation(clazz);
        throw new UnsupportedContentRepresentation("Unsupported: " + clazz);
    }

    @Override
    public InputStream getContentInputStream() throws IOException {
        // Return actual video stream
        if (this.videoRef.sourceVideo() != null) {
            return new FileInputStream(new File(this.videoRef.sourceVideo().path()));
        }

        // TODO possibly fallback to stored and temporary upload-file path
        throw new IOException("Source video stream not found and no fallback in place yet");
    }

    @Override
    public long getContentLength() throws IOException {
        if (this.videoRef.sourceVideo() != null) {
            return this.videoRef.sourceVideo().size();
        }

        // TODO possibly fallback to stored and temporary upload-file path
        throw new IOException("Source video stream not found and no fallback in place yet");
    }
    
}
