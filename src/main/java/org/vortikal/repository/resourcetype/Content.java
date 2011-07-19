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
package org.vortikal.repository.resourcetype;

import java.io.IOException;
import java.io.InputStream;


/**
 * Interface for accessing resource content during property
 * evaluation. Allows pluggable (and reusable) representations of the
 * resource content, e.g. JDOM Document, ByteBuffer, etc.
 */
public interface Content {
    
    /**
     * Gets the content representation specified by a given class.
     *
     * @param clazz the class of the desired content representation
     * @return the content representation, or <code>null</code> if no
     * such representation is available.
     * @exception Exception if an error occurs
     */
    public Object getContentRepresentation(Class<?> clazz) throws Exception;
    

    /**
     * Gets the content of a resource as a stream. Equivalent to
     * calling <code>getContentRepresentation(InputStream.class)</code>
     *
     * @return an <code>InputStream</code>
     * @exception IOException if an error occurs
     */
    public InputStream getContentInputStream() throws IOException;
    


    /**
     * Gets the length of the resource's content stream measured in
     * bytes.
     *
     * @return a the length of the content stream in bytes.
     * @exception IOException if an error occurs
     */
    public long getContentLength() throws IOException;

}
