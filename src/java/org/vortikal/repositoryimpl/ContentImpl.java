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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.vortikal.repository.resourcetype.Content;

/**
 * Implementation of <code>org.vortikal.repository.resourcetype.Content</code>
 * interface.
 * 
 * NOTE: returned representations are <em>not</em> cloned. Modifications from the
 * outside will remain permanent for any given representation, and if the binary
 * representation is modified (<code>byte[]</code>), the changes will be reflected in 
 * any subsequent creations of new and un-cached representations.
 * 
 * Representations are created and cached lazily, when requested.
 * 
 * @author oyviste
 *
 */
public class ContentImpl implements Content {

    private InputStream inputStream;
    private Map representations;
    private byte[] content; // Need to keep binary content representation, because
                            // we should not trust that the given input stream
                            // can be reliably reset if new representations are
                            // requested.
    
    public ContentImpl(InputStream inputStream) {
        this.inputStream = inputStream;
        this.representations = new HashMap();
        this.content = null; // Just to be explicit (lazy-initialized).
    }
    
    private void initializeContent() throws IOException {
        if (this.content == null) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buffer = new byte[1000];
            int n;
            while ((n = this.inputStream.read(buffer, 0, buffer.length)) != -1) {
                bout.write(buffer, 0, n);
            }
            
            this.content = bout.toByteArray();
            this.inputStream.close();
        }
    }

    public Object getContentRepresentation(Class clazz) throws Exception {
        // Make sure we have original content from inputstream 
        // before we do anything else. This closes the input stream.
        initializeContent();
        
        Object representation = representations.get(clazz);
        if (representation == null) {
            // Lazy load representation
            representation = 
                ContentRepresentationFactory.createRepresentation(clazz, this.content);
            representations.put(clazz, representation);
        } 
        
        return representation;
    }
    
    public InputStream getContentInputStream() throws IOException {
        // Make sure we have original content from inputstream 
        // before we do anything else. This closes the input stream.
        initializeContent();
        
        return new ByteArrayInputStream(this.content);
    }
    
    public Class[] getSupportedRepresentations() {
        return ContentRepresentationFactory.SUPPORTED_REPRESENTATIONS;
    }

}
