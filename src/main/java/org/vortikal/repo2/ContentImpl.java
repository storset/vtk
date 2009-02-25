/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.repo2;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.vortikal.repository.ContentStream;
import org.vortikal.repository.content.ContentRepresentationRegistry;
import org.vortikal.repository.resourcetype.Content;

public class ContentImpl implements Content {

    private NodeID nodeID;
    private ContentStore contentStore;
    private ContentRepresentationRegistry contentRegistry;
    private Map<Class<?>, Object> representations;
    
    public ContentImpl(NodeID nodeID, ContentStore contentStore,
                       ContentRepresentationRegistry contentRegistry) {
        this.representations = new HashMap<Class<?>, Object>();
        this.contentStore = contentStore;
        this.contentRegistry = contentRegistry;
        this.nodeID = nodeID;
    }
    
    public Object getContentRepresentation(Class<?> clazz) throws Exception {
        // Make sure we have original content from inputstream 
        // before we do anything else. 
        
        // We don't cache InputStream representations
        if (clazz == java.io.InputStream.class) {
            ContentStream cs = this.contentStore.retrieve(this.nodeID);
            return cs.getStream();
        }
        
        Object representation = this.representations.get(clazz);
        if (representation == null) {
            // Lazy load representation
            
            InputStream inputStream = null;
            try {
                ContentStream cs = this.contentStore.retrieve(this.nodeID);
                inputStream = cs.getStream();
                representation = this.contentRegistry.createRepresentation(clazz, inputStream);
                
            } finally {
//                 if (inputStream != null) {
//                     inputStream.close();
//                 }
            }
            // Don't cache large buffers:
            if (clazz == byte[].class) {
                int length = ((byte[]) representation).length;
                if (length <= 1000000) {
                    this.representations.put(clazz, representation);
                }
            } else {
                this.representations.put(clazz, representation);
            }
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
    
    public long getContentLength() throws Exception {
        ContentStream cs = this.contentStore.retrieve(this.nodeID);
        return cs.getLength();
    }
    
}
