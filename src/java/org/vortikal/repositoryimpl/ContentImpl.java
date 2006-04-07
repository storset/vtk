/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repositoryimpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repositoryimpl.dao.ContentStore;

/**
 * Implementation of <code>org.vortikal.repository.resourcetype.Content</code>
 * interface.
 * 
 * NOTE: returned representations are <em>not</em> cloned. Modifications from the
 * outside will remain permanent for any given representation.
 * 
 * The <code>java.io.InputStream</code> representation is not cached, a new stream
 * is returned every time. This is the only exception.
 * 
 * Representations are created and cached lazily, when requested.
 * 
 * @author oyviste
 *
 */
public class ContentImpl implements Content {

    private ContentStore contentStore;
    private String uri;
    private Map representations;
    
    public ContentImpl(String uri, ContentStore contentStore) {
        this.representations = new HashMap();
        this.contentStore = contentStore;
        this.uri = uri;
    }
    
    public Object getContentRepresentation(Class clazz) throws Exception {
        // Make sure we have original content from inputstream 
        // before we do anything else. This closes the input stream.
        
        // We don't cache InputStream representations
        if (clazz == java.io.InputStream.class) {
            return this.contentStore.getInputStream(this.uri); 
            
        }
        
        Object representation = representations.get(clazz);
        if (representation == null) {
            // Lazy load representation
            
            InputStream inputStream = this.contentStore.getInputStream(this.uri);
            representation = 
                ContentRepresentationFactory.createRepresentation(clazz, inputStream);
            inputStream.close();
            representations.put(clazz, representation);
        } 
        
        return representation;
    }
    
    public InputStream getContentInputStream() throws IOException {
        try {
            return (InputStream) getContentRepresentation(java.io.InputStream.class);
        } catch (Exception e) {
            throw new IOException("Unable to create input stream representation: " + e.getMessage());
        }
    }
    
    public long getContentLength() throws IOException {
        return this.contentStore.getContentLength(this.uri);
    }
    
    public Class[] getSupportedRepresentations() {
        return ContentRepresentationFactory.SUPPORTED_REPRESENTATIONS;
    }

}
