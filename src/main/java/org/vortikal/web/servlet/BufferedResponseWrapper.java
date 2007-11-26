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
package org.vortikal.web.servlet;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.vortikal.util.io.BoundedOutputStream;



/**
 * A response wrapper that buffers the content written to it. Status
 * code and headers are passed through to the wrapped response. An
 * optional limit can be set on the buffer size.
 */
public class BufferedResponseWrapper extends HttpServletResponseWrapper {

    private long maxBufferSize = -1;
    private ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
    private String contentType = null;
    private String characterEncoding = null;    
    private boolean isCommitted = false;

    private static final Pattern CONTENT_TYPE_VALUE_PATTERN = 
                                    Pattern.compile(".+/.+;.*charset.*=.+");

    /**
     * Creates a buffered response wrapper with no limit on the buffer size.
     * @param resp the wrapped response
     */
    public BufferedResponseWrapper(HttpServletResponse resp) {
        super(resp);
    }

    /**
     * Creates a buffered response wrapper with a limited buffer size.
     * @param resp the wrapped response
     * @param maxBufferSize the buffer size limit
     */
    public BufferedResponseWrapper(HttpServletResponse resp, long maxBufferSize) {
        super(resp);
        this.maxBufferSize = maxBufferSize;
    }


    /**
     * Gets the written content as a byte buffer.
     */
    public byte[] getContentBuffer() {
        return this.bufferStream.toByteArray();
    }


    /**
     * Gets the written content as a string, converted using the
     * character encoding of this response. If that character encoding
     * is unspecified, <code>utf-8</code> is used.
     */
    public String getContentString() throws Exception {
        if (this.characterEncoding != null) {
            return new String(this.bufferStream.toByteArray(), this.characterEncoding);
        }

        return new String(this.bufferStream.toByteArray(), "utf-8");
    }
    

    public String getContentType() {
        if (this.characterEncoding != null) {
            return this.contentType + ";charset=" + this.characterEncoding;
        }
        return this.contentType;
    }
    

    public void setContentType(String contentType) {
        super.setContentType(contentType);
        processContentTypeHeader(contentType);
    }


    public void addHeader(String name, String value) {
        super.addHeader(name, value);
        if ("Content-Type".equals(name)) {
            processContentTypeHeader(value);
        }
    }


    public void setHeader(String name, String value) {
        super.setHeader(name, value);
        if ("Content-Type".equals(name)) {
            processContentTypeHeader(value);
        }
    }


    public String getCharacterEncoding() {
        if (this.characterEncoding != null) {
            return this.characterEncoding;
        }
        return super.getCharacterEncoding();
    }
    

    public boolean isCommitted() {
        return this.isCommitted || super.isCommitted();
    }

    public ServletOutputStream getOutputStream() {
        if (this.isCommitted) {
            throw new IllegalStateException(
                "getWriter() has already been called on this response.");
        }
        
        OutputStream outputStream = this.bufferStream;
        if (this.maxBufferSize > 0) {
            outputStream = new BoundedOutputStream(outputStream, this.maxBufferSize);
        }

        this.isCommitted = true;
        return new WrappedServletOutputStream(outputStream, this.getCharacterEncoding());
    }


    public PrintWriter getWriter() {
        if (this.isCommitted) {
            throw new IllegalStateException(
                "getOutputStream() has already been called on this response.");
        }

        OutputStream outputStream = this.bufferStream;
        if (this.maxBufferSize > 0) {
            outputStream = new BoundedOutputStream(outputStream, this.maxBufferSize);
        }

        this.isCommitted = true;
        return new WrappedServletOutputStreamWriter(
            new WrappedServletOutputStream(
                outputStream, this.getCharacterEncoding()));
    }
    

    public HttpServletResponse getHttpServletResponse() {
        return (HttpServletResponse) getResponse();
    } 



    private void processContentTypeHeader(String value) {
        if (CONTENT_TYPE_VALUE_PATTERN.matcher(value).matches()) {

            String contentType = value.substring(
                0, value.indexOf(";")).trim();
            String characterEncoding = value.substring(
                value.indexOf("=") + 1).trim();

            if (characterEncoding.startsWith("\"")
                && characterEncoding.endsWith("\"")) {

                characterEncoding = characterEncoding.substring(
                    1, characterEncoding.length() - 1).trim();
            }
            this.contentType = contentType;
            this.characterEncoding = characterEncoding;

        } else {
            this.contentType = value;
        }
    }


}
