package org.vortikal.web.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.vortikal.context.BaseContext;

public class DAVLoggingRequestFilter extends AbstractRequestFilter {

    @Override
    public HttpServletRequest filterRequest(HttpServletRequest request) {
        BaseContext ctx = BaseContext.getContext();

        // Add self to thread local context
        DavLoggingRequestWrapper w = new DavLoggingRequestWrapper(request);
        ctx.setAttribute(DavLoggingRequestWrapper.class.getName(), w);

        return w;
    }


    public class DavLoggingRequestWrapper extends HttpServletRequestWrapper {

        private LoggingInputStreamWrapper streamWrapper;

        public DavLoggingRequestWrapper(HttpServletRequest request) {
            super(request);
            try {
                this.streamWrapper = new LoggingInputStreamWrapper(request.getInputStream());
            } catch (IOException io) {
            }
        }

        public ServletInputStream getInputStream() {
            return this.streamWrapper;
        }

        LoggingInputStreamWrapper getLoggingInputStreamWrapper() {
            return this.streamWrapper;
        }

    }

    class LoggingInputStreamWrapper extends ServletInputStream {

        private ByteArrayOutputStream streamBuffer;
        private InputStream wrappedStream;

        LoggingInputStreamWrapper(InputStream wrappedStream) {
            this.wrappedStream = wrappedStream;
            this.streamBuffer = new ByteArrayOutputStream();
        }

        byte[] getLoggedInputBytes() {
            return this.streamBuffer.toByteArray();
        }

        @Override
        public int read() throws IOException {
            int b = this.wrappedStream.read();
            if (b > -1) {
                streamBuffer.write(b);
            }
            return b;
        }
    }
}
