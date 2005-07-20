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
package org.vortikal.util.web;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;


/**
 * Various Http utility methods.
 *
 */
public class HttpUtil {



    /* HTTP status codes defined by WebDAV */
    public static final int SC_PROCESSING = 102;
    public static final int SC_MULTI_STATUS = 207;
    public static final int SC_UNPROCESSABLE_ENTITY = 418;
    public static final int SC_INSUFFICIENT_SPACE_ON_RESOURCE = 419;
    public static final int SC_METHOD_FAILURE = 420;
    public static final int SC_LOCKED = 423;
    public static final int SC_FAILED_DEPENDENCY = 424;
    public static final int SC_INSUFFICIENT_STORAGE = 507;


    private static final Map statusMessages;
    static {
        statusMessages = new HashMap();
        statusMessages.put(new Integer(HttpServletResponse.SC_ACCEPTED), "Accepted");
        statusMessages.put(new Integer(HttpServletResponse.SC_BAD_GATEWAY), "Bad Gateway");
        statusMessages.put(new Integer(HttpServletResponse.SC_BAD_REQUEST), "Bad Request");
        statusMessages.put(new Integer(HttpServletResponse.SC_CONFLICT), "Conflict");
        statusMessages.put(new Integer(HttpServletResponse.SC_CONTINUE), "Continue");
        statusMessages.put(new Integer(HttpServletResponse.SC_CREATED), "Created");
        statusMessages.put(new Integer(HttpServletResponse.SC_EXPECTATION_FAILED), "Expectation Failed");
        statusMessages.put(new Integer(HttpServletResponse.SC_FORBIDDEN), "Forbidden");
        statusMessages.put(new Integer(HttpServletResponse.SC_GATEWAY_TIMEOUT), "Gateway Timeout");
        statusMessages.put(new Integer(HttpServletResponse.SC_GONE), "Gone");
        statusMessages.put(new Integer(HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED), "HTTP Version Not Supported");
        statusMessages.put(new Integer(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), "Internal Server Error");
        statusMessages.put(new Integer(HttpServletResponse.SC_LENGTH_REQUIRED), "Length Required");
        statusMessages.put(new Integer(HttpServletResponse.SC_METHOD_NOT_ALLOWED), "Method Not Allowed");
        statusMessages.put(new Integer(HttpServletResponse.SC_MOVED_PERMANENTLY), "Moved Permanently");
        statusMessages.put(new Integer(HttpServletResponse.SC_MOVED_TEMPORARILY), "Moved Temporarily");
        statusMessages.put(new Integer(HttpServletResponse.SC_MULTIPLE_CHOICES), "Multiple Choices");
        statusMessages.put(new Integer(HttpServletResponse.SC_NO_CONTENT), "No Content");
        statusMessages.put(new Integer(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION), "Non Authorative Information");
        statusMessages.put(new Integer(HttpServletResponse.SC_NOT_ACCEPTABLE), "Not Acceptible");
        statusMessages.put(new Integer(HttpServletResponse.SC_NOT_FOUND), "Not Found");
        statusMessages.put(new Integer(HttpServletResponse.SC_NOT_MODIFIED), "Not Modified");
        statusMessages.put(new Integer(HttpServletResponse.SC_OK), "OK");
        statusMessages.put(new Integer(HttpServletResponse.SC_PARTIAL_CONTENT), "Partial Content");
        statusMessages.put(new Integer(HttpServletResponse.SC_PAYMENT_REQUIRED), "Payment Required");
        statusMessages.put(new Integer(HttpServletResponse.SC_PRECONDITION_FAILED), "Precondition Failed");
        statusMessages.put(new Integer(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED), "Proxy Authentication Required");
        statusMessages.put(new Integer(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE), "Request Entity Too Large");
        statusMessages.put(new Integer(HttpServletResponse.SC_REQUEST_TIMEOUT), "Request Timeout");
        statusMessages.put(new Integer(HttpServletResponse.SC_REQUEST_URI_TOO_LONG), "Request URI Too Long");
        statusMessages.put(new Integer(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE), "Requested Range Not Satisfiable");
        statusMessages.put(new Integer(HttpServletResponse.SC_RESET_CONTENT), "Reset Content");
        statusMessages.put(new Integer(HttpServletResponse.SC_SEE_OTHER), "See Other");
        statusMessages.put(new Integer(HttpServletResponse.SC_SWITCHING_PROTOCOLS), "Switching Protocols");
        statusMessages.put(new Integer(HttpServletResponse.SC_TEMPORARY_REDIRECT), "Temporary Redirect");
        statusMessages.put(new Integer(HttpServletResponse.SC_UNAUTHORIZED), "Unauthorized");
        statusMessages.put(new Integer(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE), "Unsupported Media Type");
        statusMessages.put(new Integer(HttpServletResponse.SC_USE_PROXY), "Use Proxy");

        /* Include some WebDAV status codes: */
        statusMessages.put(new Integer(SC_MULTI_STATUS), "Multi-Status");
        statusMessages.put(new Integer(SC_PROCESSING), "Processing");
        statusMessages.put(new Integer(SC_UNPROCESSABLE_ENTITY), "Unprocessable Entity");
        statusMessages.put(new Integer(SC_INSUFFICIENT_SPACE_ON_RESOURCE), "Insufficient Space On Resource");
        statusMessages.put(new Integer(SC_METHOD_FAILURE), "Method Failure");
        statusMessages.put(new Integer(SC_LOCKED), "Locked");
        statusMessages.put(new Integer(SC_FAILED_DEPENDENCY), "Failed Dependency");
        statusMessages.put(new Integer(SC_INSUFFICIENT_STORAGE), "Insufficient Storage");
    }


    /**
     * Formats a HTTP date suitable for headers such as "Last-Modified".
     *
     * @param date a <code>Date</code> value
     * @return a date string
     */
    public static String getHttpDateString(Date date) {

        SimpleDateFormat formatter =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                                 java.util.Locale.US);
        formatter.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        return formatter.format(date);
    }


    
    /**
     * Gets the MIME type part from a header possibly containing a
     * 'charset' parameter.
     *
     * @param headerValue the value part of a HTTP 'Content-Type:' header.
     * @return a <code>String</code>
     */
    public static String getMimeTypeFromContentTypeHeader(String headerValue) {
        if (headerValue == null) {
            throw new NullPointerException("headerValue is null");
        }
        if (!headerValue.matches("\\s*\\w+/\\w+(;charset=[^\\s]+)?")) {
            throw new IllegalArgumentException("Header value '" + headerValue +
                                               "' is not a valid 'Content-Type' value");
        }
        if (headerValue.indexOf(";") == -1) {
            return headerValue.trim();
        }
        return headerValue.substring(0, headerValue.indexOf(";")).trim();
    }      
    


    /**
     * Gets the status message for a HTTP status code. For example,
     * <code>getStatusMessage(HttpServletResponse.SC_NOT_FOUND)</code>
     * would return <code>Not Found</code>.
     *
     * @param status an <code>int</code> value
     * @return a <code>String</code>
     */
    public static String getStatusMessage(int status) {
        Integer key = new Integer(status);
        if (!statusMessages.containsKey(key)) {
            throw new IllegalArgumentException("Unknown status code: " + status);
        }
        return (String) statusMessages.get(key);
    }
    

    /**
     * Extracts a field from an HTTP header.
     *
     * @param header the HTTP header
     * @param name the name of the field wanted
     * @return the value of the field, or <code>null</code> if not
     * found
     */
    public static String extractHeaderField(String header, String name) {

        StringTokenizer tokenizer = 
            new StringTokenizer(header.substring("Digest: ".length() -1 ), ",");

        while (tokenizer.hasMoreTokens()) {

            String token = tokenizer.nextToken().trim();

            if (token.startsWith(name + "=\"")) {
                int startPos = token.indexOf("\"") + 1;
                int endPos = token.indexOf("\"", startPos);

                if (startPos > 0 && endPos > startPos) {
                    return token.substring(startPos, endPos);
                }
            } else if (token.startsWith(name + "=")) {
                int startPos = token.indexOf("=") + 1;
                if (startPos > 0) {
                    return token.substring(startPos);
                }
            }   
        }
        return null;
    }


}
