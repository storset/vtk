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
import java.nio.ByteBuffer;

import org.jdom.input.SAXBuilder;

/**
 * Create representations from binary content.
 * 
 * @author oyviste
 */
public final class ContentRepresentationFactory {

    public static final Class[] SUPPORTED_REPRESENTATIONS = {
        org.jdom.Document.class,
        String.class,
        java.io.InputStream.class,
        java.nio.ByteBuffer.class,
        byte[].class
    };
    
    public static Object createRepresentation(Class clazz, 
                                              byte[] content) 
        throws Exception {
        
        if (clazz == org.jdom.Document.class) {
            return createJDOMRepresentation(content);
        } else if (clazz == byte[].class) {
            return content;
        } else if (clazz == String.class) {
            return new String(content); // Hmm.. default encoding only ..
        } else if (clazz == java.io.InputStream.class) {
            return new ByteArrayInputStream(content);
        } else if (clazz == java.nio.ByteBuffer.class) {
            return ByteBuffer.wrap(content);
        }
        
        throw new UnsupportedContentRepresentationException("Content type '" + 
                clazz.getName() + "' not supported.");
    }
    
    private static org.jdom.Document createJDOMRepresentation(byte[] content) 
        throws Exception {
        return new SAXBuilder().build(new ByteArrayInputStream(content));
    }
    
}
