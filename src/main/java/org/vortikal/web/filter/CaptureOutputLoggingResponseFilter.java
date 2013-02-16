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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.vortikal.context.BaseContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.filter.CaptureInputRequestFilter.CaptureInputRequestWrapper;
import org.vortikal.web.service.Service;
import org.vortikal.web.servlet.HeaderAwareResponseWrapper;

/**
 * HTTP protocol logger/dumper. Logs headers and body by providing a request and
 * response filter in bean context, and then logging the captured data when
 * a {@link ServletRequestHandletEvent } is received.
 * 
 * There is one log message sent per request-response cycle, and log output will
 * consist of multiple lines of data. So configuring a separate log appender/formatter
 * for this class makes sense. It will generally use a logger with the same
 * name as the fully qualified class, but if a specific service is configured, that
 * service name will be appended to the logger id.
 * 
 * Note: in order for logging to occur, you must also configure an instance of
 * {@link CaptureInputRequestFilter} in the bean context.
 * 
 * @see CaptureInputRequestFilter
 */
public class CaptureOutputLoggingResponseFilter extends AbstractResponseFilter 
    implements ApplicationListener<ServletRequestHandledEvent>, InitializingBean {

    private int maxLogBytesBody = 4096;
    private Service service;
    private Log logger;
        
    @Override
    public void afterPropertiesSet() throws Exception {
        String loggerId = CaptureOutputLoggingResponseFilter.class.getName();
        if (this.service != null) {
            loggerId += "." + this.service.getName();
        }
        this.logger = LogFactory.getLog(loggerId);
    }
    
    @Override
    public HttpServletResponse filter(HttpServletRequest request, HttpServletResponse response) {

        if (!logForService(RequestContext.getRequestContext().getService())) {
            return response;
        }

        // Add self to thread local context
        BaseContext ctx = BaseContext.getContext();
        CaptureOutputResponseWrapper responseWrapper = new CaptureOutputResponseWrapper(response);
        ctx.setAttribute(CaptureOutputResponseWrapper.class.getName(), responseWrapper);

        return responseWrapper;
    }

    @Override
    public void onApplicationEvent(ServletRequestHandledEvent event) {
        // Look for wrappers in thread local context and write captured
        // data to log

        BaseContext ctx = BaseContext.getContext();
        CaptureInputRequestWrapper reqWrap = (CaptureInputRequestWrapper) ctx.getAttribute(CaptureInputRequestWrapper.class
                .getName());
        CaptureOutputResponseWrapper resWrap = (CaptureOutputResponseWrapper) ctx.getAttribute(CaptureOutputResponseWrapper.class
                .getName());

        if (reqWrap == null) {
            logger.warn("No request wrapper found in thread local context, check that input capture filter is configured.");
        }
        
        if (reqWrap == null || resWrap == null) {
            return;
        }
        
        StringBuilder logbuf = new StringBuilder();

        // Request
        logbuf.append("\n--- REQUEST:\n");
        logbuf.append(reqWrap.getMethod()).append(" ").append(reqWrap.getRequestURI());
        logbuf.append(reqWrap.getQueryString() != null ? "?" + reqWrap.getQueryString() : "").append('\n');
        addHeadersForLogging(reqWrap, logbuf);
        logbuf.append('\n');
        byte[] body = reqWrap.getInputBytes();
        
        if (body.length > 0) {
            addBytesForLogging(body, reqWrap.getHeader("Content-Type"), logbuf);
        }
        
        // Response
        logbuf.append("\n--- RESPONSE:\n");
        logbuf.append(resWrap.getStatus()).append('\n');
        addHeadersForLogging(resWrap, logbuf);
        logbuf.append('\n');
        body = resWrap.getOutputBytes();

        if (body.length > 0) {
            addBytesForLogging(body, resWrap.getHeaderValue("Content-Type") != null ? resWrap.getHeaderValue("Content-Type").toString() : null, logbuf);
        }
        
        logbuf.append("\n--- END\n");
        
        logger.info(logbuf.toString());
    }
    
    private boolean logForService(Service requestService) {
        if (this.service == null) {
            return true;
        }
        while (requestService != null) {
            if (this.service == requestService) {
                return true;
            }
            requestService = requestService.getParent();
        }
        return false;
    }
    
    private void addHeadersForLogging(CaptureInputRequestWrapper requestWrapper, StringBuilder logBuffer) {
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
    
    private void addHeadersForLogging(CaptureOutputResponseWrapper responseWrapper, StringBuilder logBuffer) {
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
        if (contentTypeHeaderValue.matches("^[^/]+/xml")) {
            return false;
        }
        return !contentTypeHeaderValue.startsWith("text/");
    }



    private class CaptureOutputResponseWrapper extends HeaderAwareResponseWrapper {

        private OutputStreamCopyWrapper streamWrapper;
        private WriterCopyWrapper writerWrapper;

        public CaptureOutputResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (this.writerWrapper == null) {
                this.writerWrapper = new WriterCopyWrapper(super.getWriter());
            }
            return new PrintWriter(this.writerWrapper);
        }
        
        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (this.streamWrapper == null) {
                this.streamWrapper = new OutputStreamCopyWrapper(super.getOutputStream());
            }
            return this.streamWrapper;
        }

        byte[] getOutputBytes() {
            if (this.streamWrapper != null) {
                return this.streamWrapper.getOutputBytes();
            }
            if (this.writerWrapper != null) {
                return this.writerWrapper.getOutputString().getBytes();
            }
            return new byte[0];
        }
    }
    
    private class WriterCopyWrapper extends Writer {

        private Writer wrappedWriter;
        private StringWriter streamCopyBuffer;

        WriterCopyWrapper(Writer wrappedWriter) {
            this.wrappedWriter = wrappedWriter;
            this.streamCopyBuffer = new StringWriter();
        }
        
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            this.wrappedWriter.write(cbuf, off, len);
            this.streamCopyBuffer.write(cbuf, off, len);
        }

        @Override
        public void close() throws IOException {
            this.wrappedWriter.close();
        }
        
        @Override
        public void flush() throws IOException {
            this.wrappedWriter.flush();
        }
        
        String getOutputString() {
            return this.streamCopyBuffer.toString();
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
        public void close() throws IOException {
            this.wrappedStream.close();
        }
        
        public byte[] getOutputBytes() {
            return this.streamCopyBuffer.toByteArray();
        }
    }
    
    /**
     * Set max number of raw bytes to log for body of request and response.
     * Default value is 4096 bytes.
     *
     * @param maxLogBytesBody
     */
    public void setMaxLogBytesBody(int maxLogBytesBody) {
        if (maxLogBytesBody < 1) {
            throw new IllegalArgumentException("maxLogBytesBody must be > 0");
        }
        this.maxLogBytesBody = maxLogBytesBody;
    }
    
    /**
     * Set an explicit service filter for logging. Only requests for this
     * particular service (or any child service) will be logged.
     * @param service 
     */
    public void setService(Service service) {
        this.service = service;
    }
   
}