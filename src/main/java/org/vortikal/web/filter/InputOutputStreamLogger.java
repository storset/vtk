package org.vortikal.web.filter;

import java.io.ByteArrayOutputStream;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.web.filter.DAVLoggingRequestFilter.DavLoggingRequestWrapper;
import org.vortikal.web.filter.DAVLoggingRequestFilter.LoggingInputStreamWrapper;

public class InputOutputStreamLogger {

    private static Log log = LogFactory.getLog(InputOutputStreamLogger.class);

    public static void util(DavLoggingRequestWrapper reqWrap, ByteArrayOutputStream respStream) {
        if (reqWrap != null) {
            log.info("REQUEST:");
            Enumeration<String> headerNames = reqWrap.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();

                if (!headerName.equals("Authorization")) {
                    log.info(headerName + " : " + reqWrap.getHeader(headerName));
                }
            }
            LoggingInputStreamWrapper inputStreamWrapper = reqWrap.getLoggingInputStreamWrapper();
            log.info(new String(inputStreamWrapper.getLoggedInputBytes()));
        }

        if (respStream != null) {
            log.info("RESPONSE:");
            log.info(respStream.toString());
        }
    }
}
