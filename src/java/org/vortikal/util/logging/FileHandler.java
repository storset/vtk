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


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A rotating file log handler. The handler logs to the file specified
 * in the <code>pattern</code> construcor parameter, in which the
 * character sequence <code>%t</code> is expanded to the name of the
 * currently executing thread. This can be useful in situations where
 * only a few distinct threads exist in the system, or where threads
 * are grouped by setting their names to some common value. 
 *
 * <p> If expanding <code>%t</code> to the current thread is not
 * sufficient, this class also supports name-rewriting functionality
 * using regular expressions. When the string <code>%tm{A}{B}</code>
 * exists in the file name pattern, <code>A</code> is interpreted as a
 * regexp and the current thread's name is matched against it. If the
 * regexp does not match, the whole sequence is replaced with the
 * current thread's name. Otherwise it is replaced with the sequence
 * <code>B</code>. Substitution variables <code>$1, $2</code>,
 * etc. corresponding to the substitution groups in <code>A</code> are
 * allowed in the string <code>B</code>.
 *
 * <p> For example, if threads are named as follows: <code>foo-1,
 * foo-2, ... foo-N</code>, and the messages from these threads should
 * be placed in the file <code>/bar/baaz/foo.log</code>, the
 * following pattern could be used:
 * <code>/bar/baaz/%tm{(foo)-\d+}{$1}.log</code>.
 */
public class FileHandler extends Handler {
    private static Pattern threadMatchPattern = Pattern.compile(
        ".*%tm\\{([^\\}]+)\\}\\{([^\\}]+)\\}.*.*");

    
    private String pattern;
    private int limit;
    private int count;
    private boolean append;
    private Formatter formatter = new java.util.logging.SimpleFormatter();
    private Level level = Level.INFO;
    

//     private Map streams = new HashMap();
    private Map streamHandlers = new HashMap();
    


    /**
     * Creates a new <code>FileHandler</code> instance.
     *
     * @param pattern the file name to log to. If the sequence
     * <code>%t</code> occurs in the pattern, it is expanded to the
     * name of the current thread.
     * @param limit the maximum number of bytes before rotating
     * occurs. A value of zero or a negative value prevents rotating.
     * @param count the number of log files to use
     * @param append whether files are truncated on first write
     */
    public FileHandler(String pattern, int limit, int count, boolean append) {

        try {
            this.pattern = pattern;
            this.limit = limit;
            if (count <= 0) {
                throw new IllegalArgumentException(
                    "Count must be a positive integer (was " + count + ")");
            }
            this.count = count;
            this.append = append;

            openStreamHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    


    public synchronized void publish(LogRecord record) {

        String fileName = expandPattern(pattern);

        StreamHandler handler = (StreamHandler) streamHandlers.get(fileName);
        if (handler == null) {

            try {
                openStreamHandler();
                handler = (StreamHandler) streamHandlers.get(fileName);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        
        File file = new File(fileName);
        if (limit > 0 && file.length() > limit) {
            rotateLogFiles();
            handler = (StreamHandler) streamHandlers.get(fileName);
        }

	if (!isLoggable(record, fileName)) {
	    return;
	}

        
//        super.publish(record);
        handler.publish(record);
	handler.flush();
    }



    /**
     * Opens and returns a log stream.
     *
     * @exception IOException if an error occurs
     */
    private synchronized void openStreamHandler() throws IOException {
        String fileName = expandPattern(pattern);
        File file = new File(fileName);
        FileOutputStream out = new FileOutputStream(file, this.append);
        StreamHandler handler = new StreamHandler(new BufferedOutputStream (out), formatter);
        handler.setLevel(level);
        streamHandlers.put(expandPattern(pattern), handler);
    }
    


    private synchronized void rotateLogFiles() {
        String currentFileName = expandPattern(pattern);

        /* Rotate the old files */
        for (int i = count; i > 1; i--) {
            File file = new File(currentFileName + "." + (i - 1));
            if (file.exists()) {
                boolean ok = file.renameTo(new File(currentFileName + "." + i));
                if (!ok) {
                    System.err.println("Could not rotate log file " +
                                       file.getName());
                }
            }
        }
        StreamHandler handler = (StreamHandler) streamHandlers.get(currentFileName);
        handler.flush();
        handler.close();
        streamHandlers.remove(currentFileName);

        File currentFile = new File(currentFileName);
        if (!currentFile.renameTo(new File(currentFileName + ".1"))) {
            System.err.println("Could not rotate log file " +
                               currentFile.getName());
        }
        
        try {
            openStreamHandler();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private String expandPattern(String pattern) {
        try {

            Matcher m = threadMatchPattern.matcher(pattern);

            if (m.matches()) {
                String threadName = Thread.currentThread().getName();
                String regex = m.group(1);

                Pattern p = Pattern.compile(regex);
                Matcher m2 = p.matcher(threadName);


                if (m2.matches()) {

                    String replacement = m.group(2);

                    /* Substitute occurrances of $1, $2, etc. in the
                     * replacement string with the corresponding group in
                     * the regexp: */

                    for (int i = 1; i <= m2.groupCount(); i++) {
                        replacement = replacement.replaceAll(
                            "\\$" + i, m2.group(i));
                    }

                    String expandedPattern = pattern.replaceAll(
                        "%tm\\{[^\\}]+\\}\\{[^\\}]+\\}", replacement);
                    return expandedPattern;

                }

                String expandedPattern = pattern.replaceAll(
                        "%tm\\{[^\\}]+\\}\\{[^\\}]+\\}", 
                        Thread.currentThread().getName());
                return expandedPattern;

            }
        
            return pattern.replaceAll("%t", Thread.currentThread().getName());
            
        } catch (Throwable t) {
            t.printStackTrace();
            return pattern;
        }
    }

    public void setFormatter(Formatter newFormatter) throws SecurityException {
        this.formatter = newFormatter;
        super.setFormatter(newFormatter);
        for (Iterator i = streamHandlers.keySet().iterator(); i.hasNext();) {
            StreamHandler handler = (StreamHandler) streamHandlers.get(i.next());
            handler.setFormatter(newFormatter);
        }
    }

    public void setLevel(Level newLevel) throws SecurityException {
        this.level = newLevel;
        super.setLevel(newLevel);
        for (Iterator i = streamHandlers.keySet().iterator(); i.hasNext();) {
            StreamHandler handler = (StreamHandler) streamHandlers.get(i.next());
            handler.setLevel(newLevel);
        }
        
    }
    
    public void close() {
        for (Iterator i = streamHandlers.keySet().iterator(); i.hasNext();) {
            StreamHandler handler = (StreamHandler) streamHandlers.get(i.next());
            handler.close();
        }
        
    }

    public void flush() {
        for (Iterator i = streamHandlers.keySet().iterator(); i.hasNext();) {
            StreamHandler handler = (StreamHandler) streamHandlers.get(i.next());
            handler.flush();
        }
        
    }

    public boolean isLoggable(LogRecord record) {
        String fileName = expandPattern(pattern);

        return isLoggable(record, fileName);
    }

    public boolean isLoggable(LogRecord record, String fileName) {

        StreamHandler handler = (StreamHandler) streamHandlers.get(fileName);
        if (handler == null) {

            try {
                openStreamHandler();
                handler = (StreamHandler) streamHandlers.get(fileName);
            } catch (IOException e) {
                System.err.println(
                    "FileHandler: Error: unable to open log stream: " +
                    e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        return handler.isLoggable(record);
    }
    
}
