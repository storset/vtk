/* Copyright (c) 2004, 2007, 2008, University of Oslo, Norway
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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.vortikal.util.io.BoundedOutputStream;
import org.vortikal.util.io.StreamUtil;



/**
 * A HttpServletResponse implementation that buffers the content and
 * headers. An optional limit can be set on the buffer size.
 * 
 * <p>FIXME: What to do with statusMessage?
 * <br>FIXME: Allow several headers having the same name?
 */
public class BufferedResponse implements StatusAwareHttpServletResponse {

    private static final String DEFAULT_CHAR_ENCODING = "utf-8";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String CONTENT_TYPE = "Content-Type";

    private static final Pattern CONTENT_TYPE_HEADER_PATTERN
                                    = Pattern.compile(".+/.+;.*charset.*=.+");

    private long maxBufferSize = -1;
    private int status = 200;
    private ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
    private Map<String, Object> headers = new HashMap<String, Object>(); 
    private List<Cookie> cookies = new ArrayList<Cookie>();
    private int bufferSize = 1000;
    private String contentType = null;
    private int contentLength = -1;
    private Locale locale = Locale.getDefault();
    private String characterEncoding = null;
    private boolean committed = false;
    

    /**
     * Creates a buffered response with no limit on the buffer size.
     */
    public BufferedResponse() {
    }
    

    /**
     * Creates a buffered response with a limited buffer size.
     *
     * @param maxBufferSize the maximum number of bytes that can be
     * buffered in this response wrapper. A negative number means no
     * limit.
     */
    public BufferedResponse(long maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }
    
    public String getContentType() {
        if (this.characterEncoding != null) {
            return this.contentType + ";charset=" + this.characterEncoding;
        }
        return this.contentType;
    }
    
    public byte[] getContentBuffer() {
        return this.bufferStream.toByteArray();
    }

    public String getContentString() throws Exception {
        if (this.characterEncoding != null) {
            return new String(this.bufferStream.toByteArray(), this.characterEncoding);
        }

        return new String(this.bufferStream.toByteArray(), DEFAULT_CHAR_ENCODING);
    }

    public int getStatus() {
        return this.status;
    }
    
    public int getContentLength() {
        if (this.contentLength >= 0) {
            return this.contentLength;
        }
        
        return this.bufferStream.size();
    }
    

    public String getCharacterEncoding() {
        if (this.characterEncoding == null) {
            return "iso-8859-1";
        }
        return this.characterEncoding;
    }

    public PrintWriter getWriter() {
        return new WrappedServletOutputStreamWriter(new WrappedServletOutputStream(
                                            this.bufferStream, this.getCharacterEncoding()));
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * @deprecated Use reset() instead
     */
    public void resetBuffer() {
        this.bufferStream.reset();
    }
    
    public boolean isCommitted() {
        return this.committed;
    }

    public void reset() {
        if (this.committed) {
            throw new IllegalStateException(
                "Cannot call reset(): Response has already been committed");
        }

        this.characterEncoding = null;
        this.contentLength = -1;
        this.bufferStream.reset();
    }

    public void flushBuffer() {
        this.committed = true;
    }

    public ServletOutputStream getOutputStream() {
        OutputStream outputStream = this.bufferStream;
        if (this.maxBufferSize > 0) {
            outputStream = new BoundedOutputStream(outputStream, this.maxBufferSize);
        }
        ServletOutputStream servletStream = new WrappedServletOutputStream(outputStream,
                                                this.characterEncoding);
        return servletStream;
    }

    public void setContentType(String contentType) {
        processContentTypeHeader(contentType);
    }

    /**
     * A negative contentLength implies checking the buffer for actual size.
     * @see javax.servlet.ServletResponse#setContentLength(int)
     */
    public void setContentLength(int contentLength) {
        if (contentLength >= 0)
            this.contentLength = contentLength;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public int getBufferSize() {
        return this.bufferSize;
    }

    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }
    
    public List<Cookie> getCookies() {
        return this.cookies;
    }

    public boolean containsHeader(String header) {
        return this.headers.containsKey(header);
    }

    public String encodeURL(String url) {
        return url;
    }

    public String encodeRedirectURL(String url) {
        return url;
    }

    public String encodeUrl(String url) {
        return url;
    }

    public String encodeRedirectUrl(String url) {
        return url;
    }

    public void sendError(int status, String statusMessage) {
        this.status = status;
        this.committed = true;
    }

    public void sendError(int status) {
        this.status = status;
        this.committed = true;
    }

    public void sendRedirect(String url) {
        this.status = HttpServletResponse.SC_MOVED_TEMPORARILY;
        this.headers.put("Location", url);
        this.committed = true;
    }

    public void setDateHeader(String header, long date) {
        this.headers.put(header, new Date(date));
    }

    public void addDateHeader(String header, long date) {
        this.headers.put(header, new Date(date));
    }

    public void setHeader(String header, String value) {
        header = header.trim();
        value = value.trim();
        applyHeaderSideEffects(header, value);
        this.headers.put(header, value);
    }

    public void addHeader(String header, String value) {
        header = header.trim();
        applyHeaderSideEffects(header, value);
        this.headers.put(header, value);
    }

    public void setIntHeader(String header, int value) {
        header = header.trim();
        applyHeaderSideEffects(header, String.valueOf(value));
        this.headers.put(header, new Integer(value));
    }

    public void addIntHeader(String header, int value) {
        header = header.trim();
        applyHeaderSideEffects(header, String.valueOf(value));
        this.headers.put(header, new Integer(value));
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }

    public void setStatus(int status, String statusMessage) {
        this.status = status;
    }

    public Map<String, Object> getHeaders() {
        return this.headers;
    }
    

    private void applyHeaderSideEffects(String header, String value) {

        if (CONTENT_TYPE.equalsIgnoreCase(header)) {
            processContentTypeHeader(value);
        } else if (CONTENT_LENGTH.equalsIgnoreCase(header)) {
            try {
                int intValue = Integer.parseInt(value);
                this.contentLength = intValue;
            } catch (Exception e) {
                
            }
        }
    }
    
    private void processContentTypeHeader(String value) {
        if (CONTENT_TYPE_HEADER_PATTERN.matcher(value).matches()) {

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
            this.headers.put(CONTENT_TYPE, contentType);
        } else {
            this.contentType = value;
            this.headers.put(CONTENT_TYPE, value);
        }
    }
    
    /**
     * Write metadata and contents of buffered response to another response.
     * 
     */
    public void writeTo(HttpServletResponse response, boolean closeOutputStream) 
        throws IOException {
        // Write/copy metadata
        response.setContentLength(this.getContentLength());
        response.setContentType(this.getContentType());
        response.setStatus(this.getStatus());
        response.setLocale(this.getLocale());
        
        Map <String, Object> headers = this.getHeaders();
        for (Map.Entry<String, Object> entry: headers.entrySet()) {
            
            String header = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                response.setHeader(header, (String)value);
            } else if (value instanceof Integer) {
                response.setIntHeader(header, ((Integer)value).intValue());
            } else if (value instanceof Date) {
                response.setDateHeader(header, ((Date)value).getTime());
            } else {
                response.setHeader(header, value.toString());
            }
        }
        
        for (Cookie cookie: this.getCookies()) {
            response.addCookie(cookie);
        }

        // Write/copy content
        StreamUtil.dump(this.getContentBuffer(),
                response.getOutputStream(), closeOutputStream);
    }
    
}
