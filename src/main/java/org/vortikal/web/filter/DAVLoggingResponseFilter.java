/* Copyright (c) 2013 University of Oslo, Norway
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
package org.vortikal.web.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.context.BaseContext;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.filter.DAVLoggingRequestFilter.DavLoggingRequestWrapper;
import org.vortikal.web.servlet.HeaderAwareResponseWrapper;

public class DAVLoggingResponseFilter extends AbstractResponseFilter {

    private final Log logger = LogFactory.getLog(DAVLoggingResponseFilter.class);
    private int maxLogBytesBody = 2048;
        
    @Override
    public HttpServletResponse filter(HttpServletRequest request, HttpServletResponse response) {
  
        BaseContext ctx = BaseContext.getContext();

        // Add self to thread local context
        DavLoggingResponseWrapper responseWrapper = new DavLoggingResponseWrapper(response);
        ctx.setAttribute(DavLoggingResponseWrapper.class.getName(), responseWrapper);

        return responseWrapper;
    }

    // Called by OutputStreamCopyWrapper when stream gets the first close() call.
    void log() {
        BaseContext ctx = BaseContext.getContext();
        DavLoggingRequestWrapper reqWrap = (DavLoggingRequestWrapper) ctx.getAttribute(DavLoggingRequestWrapper.class
                .getName());
        DavLoggingResponseWrapper resWrap = (DavLoggingResponseWrapper) ctx.getAttribute(DavLoggingResponseWrapper.class
                .getName());
        
        if (reqWrap == null || resWrap == null) {
            throw new IllegalStateException("Could not find request or response wrapper in thread local context");
        }

        
        StringBuilder logbuf = new StringBuilder();

        // Request
        logbuf.append("\n--- REQUEST:\n");
        logbuf.append(reqWrap.getMethod()).append(" ").append(reqWrap.getRequestURI());
        logbuf.append(reqWrap.getQueryString() != null ? reqWrap.getQueryString() : "").append('\n');
        addHeadersForLogging(reqWrap, logbuf);
        logbuf.append('\n');
        byte[] body = reqWrap.getInputStreamWrapper().getInputBytes();
        if (body.length > 0) {
            addBytesForLogging(body, reqWrap.getHeader("Content-Type"), logbuf);
        }
        
        // Response
        logbuf.append("\n--- RESPONSE:\n");
        logbuf.append(resWrap.getStatus()).append('\n');
        addHeadersForLogging(resWrap, logbuf);
        logbuf.append('\n');
        body = resWrap.getOutputStreamWrapper().getOutputBytes();
        if (body.length > 0) {
            addBytesForLogging(body, resWrap.getHeaderValue("Content-Type").toString(), logbuf);
        }
        
        logbuf.append("\n--- END\n");
        
        logger.info(logbuf.toString());
    }
    
    
    private void addHeadersForLogging(DavLoggingRequestWrapper requestWrapper, StringBuilder logBuffer) {
        Enumeration<String> headerNames = requestWrapper.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!headerName.equalsIgnoreCase("Authorization")) {
                logBuffer.append(headerName).append(": ").append(requestWrapper.getHeader(headerName)).append('\n');
            } else {
                logBuffer.append(headerName).append(": ****\n");
            }
        }
    }
    
    private void addHeadersForLogging(DavLoggingResponseWrapper responseWrapper, StringBuilder logBuffer) {
        for (Iterator<String> it= responseWrapper.getHeaderNames(); it.hasNext(); ) {
            String header = it.next();
            List<?> values = responseWrapper.getHeaderValues(header);
            for (Object value: values) {
                logBuffer.append(header).append(": ").append(value).append('\n');
            }
        }
    }
    
    private void addBytesForLogging(byte[] data, String contentType, StringBuilder logBuffer) {
        int logBytes = Math.min(maxLogBytesBody, data.length);
        String rawString = getRawAsciiString(data, 0, logBytes, isProbablyBinary(contentType));
        logBuffer.append(rawString);
        if (data.length > logBytes) {
            logBuffer.append(" [").append(data.length - logBytes).append(" more bytes truncated ...]");
        }
    }
    
    private String getRawAsciiString(byte[] b, int offset, int length, boolean replaceUnprintable) {
        char[] rawChars = new char[b.length];
        int j = 0;
        for (int i = offset; i < length; i++) {
            char c = (char)b[i];
            if (replaceUnprintable && (c < 0x20 || c > 0x7F)) {
                c = '.';
            }
            rawChars[j++] = c;
        }
        return new String(rawChars, 0, j);
    }

    private boolean isProbablyBinary(String contentTypeHeaderValue) {
        if (contentTypeHeaderValue == null) return true;
        return !contentTypeHeaderValue.startsWith("text/");
    }

    private class DavLoggingResponseWrapper extends HeaderAwareResponseWrapper {

        private OutputStreamCopyWrapper streamWrapper;

        public DavLoggingResponseWrapper(HttpServletResponse response) {
            super(response);
            try {
                this.streamWrapper = new OutputStreamCopyWrapper(response.getOutputStream());
            } catch (IOException io) {
            }
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return this.streamWrapper;
        }

        OutputStreamCopyWrapper getOutputStreamWrapper() {
            return this.streamWrapper;
        }
    }

    private class OutputStreamCopyWrapper extends ServletOutputStream {

        private ByteArrayOutputStream streamCopyBuffer;
        private OutputStream wrappedStream;

        OutputStreamCopyWrapper(OutputStream wrappedStream) {
            this.wrappedStream = wrappedStream;
            this.streamCopyBuffer = new ByteArrayOutputStream();
        }

        @Override
        public void write(int b) throws IOException {
            this.wrappedStream.write(b);
            if (b > -1) {
                streamCopyBuffer.write(b);
            }
        }

        @Override
        public void flush() throws IOException {
            this.wrappedStream.flush();
        }

        @Override
        public void close() throws IOException {
            this.wrappedStream.close();
            log();
        }
        
        public byte[] getOutputBytes() {
            return this.streamCopyBuffer.toByteArray();
        }
    }
    
    /**
     * Set max number of raw bytes to log for body of request and response.
     * Default value is 2048 bytes.
     *
     * @param maxLogBytesBody
     */
    public void setMaxLogBytesBody(int maxLogBytesBody) {
        if (maxLogBytesBody < 1) {
            throw new IllegalArgumentException("maxLogBytesBody be > 0");
        }
        this.maxLogBytesBody = maxLogBytesBody;
    }
   
}