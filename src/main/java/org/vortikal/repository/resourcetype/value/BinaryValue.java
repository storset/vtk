/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.repository.resourcetype.value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.vortikal.repository.ContentStream;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.PropertyType.Type;

public final class BinaryValue extends Value {

    private byte[] buffer;
    private String contentType;
    private ValueRef valueRef;
    
    public interface ValueRef {
        public String getID();
        public String getContentType();
        public ContentStream getValue();
    }
    
    // called from repo
    public BinaryValue(ValueRef value) {
        this.valueRef = value;
        this.contentType = null;
        this.buffer = null;
    }
    
    // called from prop-evaluator
    public BinaryValue(byte[] buffer, String contentType) {
        this.buffer = buffer;
        this.contentType = contentType;
    }

    public ContentStream getContentStream() {
        if (this.buffer != null) {
            InputStream is = new ByteArrayInputStream(this.buffer);
            return new ContentStream(is, this.buffer.length);
        } else {
            return this.valueRef.getValue();
        }
    }

    public String getContentType() {
        if (this.buffer != null) {
            return this.contentType;
        } else {
            return this.valueRef.getContentType();
        }
    }

    @Override
    public Type getType() {
        return Type.BINARY;
    }

    
    @Override
    public Object clone() {
        // XXX this is not proper cloning (only shallow copying of data buffer):
        if (this.buffer != null) {
            return new BinaryValue(this.buffer, this.contentType);
        } else {
            return new BinaryValue(this.valueRef);
        }
    }

    @Override
    public Object getObjectValue() {
        return getContentStream();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BinaryValue)) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        BinaryValue v = (BinaryValue) obj;
        if (this.buffer == null) {
            if (v.buffer != null) {
                return false;
            }
            return this.valueRef.equals(v.valueRef);
        }
        if (v.buffer == null) {
            return false;
        }
        return this.buffer.length == v.buffer.length;
    }


    @Override
    public int hashCode() {
        return this.buffer.hashCode();
    }


    @Override
    public String getNativeStringRepresentation() {
        return this.valueRef.getID();
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("BinaryValue[");
        if (this.buffer != null) {
            b.append("buffered").append(", type = ").append(this.contentType);
            b.append(", len = ").append(this.buffer.length);
            b.append(", buffer identity = ").append(System.identityHashCode(this.buffer));
        } else {
            b.append("value-ref = ").append(this.valueRef);
        }
        b.append("]");
        return b.toString();
    }
    
    public boolean isBuffered() {
        return this.buffer != null;
    }
}
