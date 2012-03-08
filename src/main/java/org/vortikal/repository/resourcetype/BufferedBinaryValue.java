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

package org.vortikal.repository.resourcetype;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.store.DataAccessException;
import org.vortikal.repository.store.DataAccessor;
import org.vortikal.util.io.StreamUtil;

/**
 * Immutable binary value buffer.
 */
public final class BufferedBinaryValue implements BinaryValue {

    private final byte[] buffer;
    private final String contentType;

    /**
     * Construct a buffered binary value.
     * @param buffer 
     */
    public BufferedBinaryValue(byte[] buffer, String contentType) {
        if (buffer == null) throw new IllegalArgumentException("buffer cannot be null");
        this.buffer = buffer;
        this.contentType = contentType;
    }
    
    @Override
    public String getContentType() throws DataAccessException {
        return this.contentType;
    }

    @Override
    public ContentStream getContentStream() throws DataAccessException {
        ByteArrayInputStream bis = new ByteArrayInputStream(this.buffer);
        return new ContentStream(bis, this.buffer.length);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("BufferedBinaryValue[");
        b.append("type = ").append(this.contentType);
        b.append(", length = ").append(this.buffer.length);
        b.append("]");
        return b.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BufferedBinaryValue other = (BufferedBinaryValue) obj;
        if (this.buffer != other.buffer) {
            return false;
        }
        if ((this.contentType == null) ? (other.contentType != null) : !this.contentType.equals(other.contentType)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + System.identityHashCode(this.buffer);
        hash = 97 * hash + (this.contentType != null ? this.contentType.hashCode() : 0);
        return hash;
    }
}
