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
import java.io.OutputStream;


/**
 * An OutputStream wrapper that has a size limit on the number of bytes
 * passing through it.
 */
public class BoundedOutputStream extends OutputStream {
    
    protected OutputStream out;
    private long limit;
    private long bytesWritten = 0;
    

    /**
     * Creates a new <code>BoundedOutputStream</code>.
     *
     * @param out the wrapped <code>OutputStream</code>
     * @param limit the maximum number of bytes this stream accepts
     * before an <code>SizeLimitException</code> is thrown. If this
     * parameter is negative, the size is unlimited.
     */
    public BoundedOutputStream(OutputStream out, long limit) {
        this.out = out;
        this.limit = limit;
    }
    

    public void close() throws IOException {
        out.close();
    }
    

    public void write(byte[] b) throws SizeLimitException, IOException {

        if (limit > 0 && bytesWritten + b.length >= limit) {
            throw new SizeLimitException(
                "Maximum number of bytes reached: " + limit);
        }
        out.write(b);
        this.bytesWritten += b.length;
    }
    

    public void write(byte[] b, int off, int len)
        throws SizeLimitException, IOException {

        if (limit > 0 && bytesWritten + len >= limit) {
            throw new SizeLimitException(
                "Maximum number of bytes reached: " + limit);
        }
        out.write(b, off, len);
        this.bytesWritten += len;
    }


    public void write(int b) throws SizeLimitException, IOException {

        if (limit > 0 && bytesWritten + 4 >= limit) {
            throw new SizeLimitException(
                "Maximum number of bytes reached: " + limit);
        }
        out.write(b);
        this.bytesWritten += 4;
    }



    public void flush() throws IOException {
        out.flush();
    }
    

    public long getWritten() {
        return this.bytesWritten;
    }
    
}
