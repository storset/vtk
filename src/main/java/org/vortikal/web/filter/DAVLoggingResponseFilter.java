package org.vortikal.web.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.context.BaseContext;
import org.vortikal.web.filter.DAVLoggingRequestFilter.DavLoggingRequestWrapper;

public class DAVLoggingResponseFilter extends AbstractResponseFilter {

    private static Log log = LogFactory.getLog(DAVLoggingResponseFilter.class);

    @Override
    public HttpServletResponse filter(HttpServletRequest request, HttpServletResponse response) {
  
        BaseContext ctx = BaseContext.getContext();

        // Add self to thread local context
        DavLoggingResponseWrapper w = new DavLoggingResponseWrapper(response);
        ctx.setAttribute(getClass().getName(), w);

        return w;
    }
  
    public class DavLoggingResponseWrapper extends HttpServletResponseWrapper {

        private LoggingOutputStreamWrapper streamWrapper;

        public DavLoggingResponseWrapper(HttpServletResponse response) {
            super(response);
            try {
                this.streamWrapper = new LoggingOutputStreamWrapper(response.getOutputStream());
            } catch (IOException io) {
            }
        }

        public ServletOutputStream getOutputStream() {
            return this.streamWrapper;
        }

        LoggingOutputStreamWrapper getLoggingOutputStreamWrapper() {
            return this.streamWrapper;
        }

    }

    private class LoggingOutputStreamWrapper extends ServletOutputStream {

        private ByteArrayOutputStream streamBuffer;
        private OutputStream wrappedStream;

        LoggingOutputStreamWrapper(OutputStream wrappedStream) {
            this.wrappedStream = wrappedStream;
            this.streamBuffer = new ByteArrayOutputStream();
        }

        @Override
        public void write(int b) throws IOException {
            this.wrappedStream.write(b);
            if (b > -1) {
                streamBuffer.write(b);
            }
        }

        @Override
        public void close() throws IOException {
            BaseContext ctx = BaseContext.getContext();
            DavLoggingRequestWrapper dreq = (DavLoggingRequestWrapper) ctx.getAttribute(DavLoggingRequestWrapper.class
                    .getName());

            InputOutputStreamLogger.util(dreq, streamBuffer);
        }
    }
}