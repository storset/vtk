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
package org.vortikal.util.logging;

import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;


/**
 * A configurable logrecord formatter.  This class formats log records
 * based on patterns supplied at instantiation time.
 *
 * <p><b>Pattern format</b><br>
 *
 * Log records are written based on the patterns supplied, with
 * certain special tokens expanded to various runtime properties:
 * <ul>
 *  <li><code>%l</code> expands to the current log level
 *  <li><code>%s</code> the log message
 *  <li><code>%d</code> the current time
 *  <li><code>%c</code> the full class name of the logging client
 *  <li><code>%C</code> the unqualified class name of the logging client
 *  <li><code>%t</code> the exception thrown (if any)
 *  <li><code>%T</code> the current thread's name
 * </ul>
 */
public class PatternFormatter extends Formatter {

    private final static String LEVEL = "%l";
    private final static String MESSAGE = "%s";
    private final static String DATE = "%d";
    private final static String CLASSNAME = "%c";
    private final static String CLASSNAME_SHORT = "%C";
    private final static String THROWN = "%t";
    private final static String THREADNAME = "%T";
    

    private Vector format = new Vector();

    private final static String NEWLINE = System.getProperty("line.separator");

    /**
     * Creates a new <code>PatternFormatter</code> instance.
     * The format used corresponds to <code>%d %l: %s</code>
     *
     */
    public PatternFormatter() {
        super();
        this.format.add(DATE);
        this.format.add(" ");
        this.format.add(LEVEL);
        this.format.add(": ");
        this.format.add(MESSAGE);
    }
    
    public PatternFormatter(String pattern) {
        super();
        this.format = parsePattern(stripQuotes(pattern));
    }
    

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        for (Enumeration e = this.format.elements(); e.hasMoreElements();) {
            buffer.append(e.nextElement());
        }
        return buffer.toString();
    }
    
        

    public String format(LogRecord record) {
        StringBuffer buffer = new StringBuffer();

        for (Enumeration e = this.format.elements(); e.hasMoreElements();) {

            String formattingElement = (String) e.nextElement();

            if (formattingElement.equals(LEVEL)) {
                buffer.append(record.getLevel());
                continue;
            }

            if (formattingElement.equals(MESSAGE)) {
                buffer.append(record.getMessage());
                continue;
            }
            
            if (formattingElement.equals(DATE)) {
                buffer.append(new java.util.Date(record.getMillis()));
                continue;
            }
            
            if (formattingElement.equals(CLASSNAME)) {
                buffer.append(record.getSourceClassName());
                continue;
            }
            
            if (formattingElement.equals(CLASSNAME_SHORT)) {
                buffer.append(getShortClassName(record.getSourceClassName()));
                continue;
            }
            
            if (formattingElement.equals(THREADNAME)) {
                buffer.append(Thread.currentThread().getName());
                continue;
            }
            
            if (formattingElement.equals(THROWN)) {
                Throwable t = record.getThrown();
                if (t != null) {
                    
                    formatThrowable(buffer, t);
                    Throwable cause = t.getCause();
                    while (cause != null) {
                        buffer.append("Caused by: ").append(cause).append(NEWLINE);
                        formatThrowable(buffer, cause);
                        cause = cause.getCause();
                    }
                }
                continue;
            }

            /* The formatting element is plain text: */
            buffer.append(formattingElement);
        }
        
        buffer.append(NEWLINE);
        return buffer.toString();
    }
    
    


    private void formatThrowable(StringBuffer buffer, Throwable t) {
        buffer.append(t + NEWLINE);
        buffer.append("Message: ").append(t.getMessage()).append(NEWLINE);
        buffer.append("Stacktrace:").append(NEWLINE);
        StackTraceElement[] stackTrace = t.getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            buffer.append(stackTrace[i]).append(NEWLINE);
        }
    }

    

    private Vector parsePattern(String pattern) {
        Vector format = new Vector();
        int pos = 0;

        while (true) {
            
            if (pos >= pattern.length()) {
                break;
            }

            String nextToken = findNearestToken(pattern, pos);

            if (nextToken == null) {

                /* There are no special tokens left in the string,
                 * just return the rest as plain text: */

                format.add(pattern.substring(pos, pattern.length()));
                break;
                
            }
             
            /*
             * There might be regular text between here and the start of the
             * next special token, so include it:
             */

            int tokenPos = pattern.indexOf(nextToken, pos);
            if (tokenPos > pos) {
                format.add(pattern.substring(pos, tokenPos));
            }

            format.add(nextToken);
            pos = tokenPos + nextToken.length();
        }
        return format;
    }
    

    private String findNearestToken(String pattern, int startPos) {
        String token = null;
        int testPos = startPos;

        testPos = pattern.indexOf(LEVEL, startPos);
        int nearest = testPos;
        if (testPos >= 0 && ((testPos <= nearest) || (nearest == -1)) ) {
            nearest = testPos;
            token = LEVEL;
        }

        testPos = pattern.indexOf(MESSAGE, startPos);
        if (testPos >= 0 && ((testPos <= nearest) || (nearest == -1)) ) {
            nearest = testPos;
            token = MESSAGE;
        }
        
        testPos = pattern.indexOf(DATE, startPos);
        if (testPos >= 0 && ((testPos <= nearest) || (nearest == -1)) ) {
            nearest = testPos;
            token = DATE;
        }
        
        testPos = pattern.indexOf(CLASSNAME, startPos);
        if (testPos >= 0 && ((testPos <= nearest) || (nearest == -1)) ) {
            nearest = testPos;
            token = CLASSNAME;
        }

        testPos = pattern.indexOf(CLASSNAME_SHORT, startPos);
        if (testPos >= 0 && ((testPos <= nearest) || (nearest == -1)) ) {
            nearest = testPos;
            token = CLASSNAME_SHORT;
        }
        
        testPos = pattern.indexOf(THROWN, startPos);
        if (testPos >= 0 && ((testPos <= nearest) || (nearest == -1)) ) {
            nearest = testPos;
            token = THROWN;
        }

        testPos = pattern.indexOf(THREADNAME, startPos);
        if (testPos >= 0 && ((testPos <= nearest) || (nearest == -1)) ) {
            nearest = testPos;
            token = THREADNAME;
        }
        
        return token;
    }
    


    private String stripQuotes(String string) {
        String s = string;
        if (s.startsWith("\"")) {
            s = s.substring(1, s.length());
        }
        if (s.endsWith("\"")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }


    private String getShortClassName(String className) {
        if (className.indexOf(".") == -1) {
            return className;
        }

        return className.substring(className.lastIndexOf(".") + 1,
                                   className.length());
    }
    

}

