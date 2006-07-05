/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.util.io;

import java.io.IOException;
import java.io.InputStream;



/**
 * An InputStream wrapper that has a size limit on the number of bytes
 * passing through it.
 */
public class BoundedInputStream extends InputStream {
    
    protected InputStream in;
    private long limit;
    private long bytesRead = 0;
    

    /**
     * Creates a new <code>BoundedInputStream</code>.
     *
     * @param in the wrapped <code>InputStream</code>
     * @param limit the maximum number of bytes this stream accepts
     * before an <code>SizeLimitException</code> is thrown. If this
     * parameter is negative, the size is unlimited.
     */
    public BoundedInputStream(InputStream in, long limit) {
        this.in = in;
        this.limit = limit;
    }
    

    public int available() throws IOException {
        return this.in.available();
    }
    

    public void close() throws IOException {
        this.in.close();
    }
    

    public void mark(int readLimit) {
        this.in.mark(readLimit);
    }


    public void reset() throws IOException {
        this.in.reset();
    }


    public boolean markSupported() {
        return false;
    }

    
    public int read() throws SizeLimitException, IOException {
        int result = this.in.read();
        if (result > 0) this.bytesRead++;
        if (this.limit > 0 && this.bytesRead >= this.limit) {
            throw new SizeLimitException(
                "Maximum number of bytes reached: " + this.limit);
        }
        return result;
    }
    

    public int read(byte[] b) throws SizeLimitException, IOException {
        int chunk = this.in.read(b);
        if (chunk > 0) this.bytesRead += chunk;
        if (this.limit > 0 && this.bytesRead >= this.limit) {
            throw new SizeLimitException(
                "Maximum number of bytes reached: " + this.limit);
        }
        return chunk;
    }

    
    public int read(byte[] b, int off, int len)
        throws SizeLimitException, IOException {

        int chunk = this.in.read(b, off, len);
        if (chunk > 0) this.bytesRead += chunk;
        if (this.limit > 0 && this.bytesRead >= this.limit) {
            throw new SizeLimitException(
                "Maximum number of bytes reached: " + this.limit);
        }
        return chunk;
    }

    public long skip(long n) throws IOException {
        return this.in.skip(n);
    }
    

    public long getRead() {
        return this.bytesRead;
    }
    
}
