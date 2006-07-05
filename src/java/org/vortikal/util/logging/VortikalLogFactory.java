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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;


/**
 * Implementation of the commons-logging LogFactory.
 * 
 * <h3>Configuration</h3>
 * 
 * <code>VortikalLogFactory</code> has its own configuration file.  At
 * initialization time, it looks for a file named
 * <code>vortikal.logging.properties</code> in the classpath. This may
 * be overridden by setting the system property
 * <code>org.vortikal.logging.config.file</code>, in which case the
 * file pointed to by that property is used.
 *
 * <h3>Configuration file format</h3>
 *
 * Loggers are organized into hierarchies. Every hierarchy has its own
 * logging level and handler. An example of a hierarchy definition:
 * <code>
 * <pre>
 * toplevel = no.uio
 * toplevel.level = FINEST
 * toplevel.handler = java.util.logging.FileHandler("%h/logs/toplevel.log", 10000, 1, true)
 * </pre>
 * </code>
 *
 * In this example, a hierarchy, <code>toplevel</code> is
 * defined. This hierarchy will intercept any log requests of classes
 * whose package names begin with <code>no.uio</code>.  All log
 * requests in this hierarchy will receive the same logger. The level
 * of this logger is set to <code>FINEST</code>, meaning that all log
 * messages having level <code>FINEST</code> or above will be
 * printed. If a handler is specified, that handler will be used,
 * otherwise the default toplevel handler as specified in the system
 * property file <code>jre/lib/logging.properties</code> is used
 * (usually) a <code>java.util.logging.ConsoleHandler</code>.
 *
 * <h3>Filtering</h3>
 *
 * To get log filtering at a finer level, one can specify more
 * specific hierarchies. For example, to filter out all logging from
 * the class <code>no.uio.my.specific.VerboseClass</code> into its own
 * log file, set up the following hierarchy:
 * <pre>
 * mylogger = no.uio.my.specific.VerboseClass
 * mylogger.level = FINEST
 * mylogger.handler = java.util.logging.FileHandler("%h/logs/trash.log", 10000, 1, true)
 * mylogger.formatter = org.vortikal.util.logging.PatternFormatter("%s")
 * </pre>
 *
 * <h3>Handlers</h3>
 *
 * The handlers specified must be subclasses of the abstract class
 * <code>java.util.logging.Handler</code>, an also be supported by the
 * <code>VortikalLogger</code>. Currently, the only handlers supported
 * are <code>java.util.logging.ConsoleHandler</code>,
 * <code>java.util.logging.FileHandler</code> and
 * <code>org.vortikal.util.logging.FileHandler</code>. The file
 * handlers require the parameters <code>(filename, limit, count,
 * append)</code> to be supplied in the configuration file, e.g.
 * <code>java.util.logging.FileHandler(filename, limit, count,
 * append)</code>.
 *
 * <h3>Formatters</h3>
 *
 * If no formatter is specified for a hierarchy, the default formatter
 * defined by the Java logging API (usually
 * <code>java.util.logging.XMLFormatter</code>) is used. The Vortikal
 * Logging implementation defines its own formatter,
 * <code>org.vortikal.logging.PatternFormatter</code>. This
 * formatter takes a format string as a parameter and generates log
 * messages based on this format. The format string must be supplied
 * in the configuration file, e.g. <code>mylogger.formatter =
 * org.vortikal.util.logging.PatternFormatter("%d %c: %s")</code>.
 * See JavaDoc documentation for this class for an explanation of the format.
 *
 * <h3>Levels</h3>
 *
 * The Vortikal logging configuration supports a subset of the logging
 * levels defined by the Java logging API. The levels are:
 * <ul>
 * <li>ALL (used by the <code>trace()</code> methods)</li>
 * <li>FINEST (used by the <code>trace()</code> methods)</li>
 * <li>FINER (used by the <code>trace()</code> methods)</li>
 * <li>FINE (used by the <code>debug()</code> methods)</li>
 * <li>CONFIG (not currently used by any method)</li>
 * <li>INFO (used by the <code>info()</code> methods)</li>
 * <li>WARNING (used by the <code>warning()</code> methods)</li>
 * <li>SEVERE (used by the <code>error()</code> and
 * <code>fatal()</code> methods)</li>
 * </ul>
 */
public class VortikalLogFactory extends org.apache.commons.logging.LogFactory {

    private static boolean debug =
        ("true".equals(System.getProperty("org.vortikal.logging.debug")));

    private Properties properties = new Properties();
    private Map loggers = new HashMap();
    private Map loggerDescriptors = new HashMap();
    

    public VortikalLogFactory() {
        configure();
    }
    
    

    /**
     * Describe <code>setAttribute</code> method here.
     *
     * @param name a <code>String</code> value
     * @param object an <code>Object</code> value
     */
    public void setAttribute(String name, Object object) {
        this.properties.put(name, object);
    }


    /**
     * Describe <code>getInstance</code> method here.
     *
     * @return a <code>Log</code>
     * @exception LogConfigurationException if an error occurs
     */
    public Log getInstance(Class clazz) throws LogConfigurationException {
        return getInstance(clazz.getName());
    }


    /**
     * Describe <code>getInstance</code> method here.
     *
     * @param name a <code>String</code> value
     * @return a <code>Log</code>
     * @exception LogConfigurationException if an error occurs
     */
    public Log getInstance(String name) throws LogConfigurationException {

        synchronized(this.loggers) {
            VortikalLogger logger = (VortikalLogger) this.loggers.get(name);
            if (logger != null) {
                return logger;
            }
        }
        
        synchronized(this.loggers) {
            VortikalLogger logger = new VortikalLogger(name);
            initLogger(logger);
            this.loggers.put(name, logger);
            return logger;
        }
    }


    private void initLogger(VortikalLogger logger) throws LogConfigurationException {
        LoggerDescriptor descriptor = lookupDescriptor(logger.getName());

        if (descriptor != null) {

            Handler handler = null;

            handler = getHandler(
                descriptor.getHandler(), descriptor.getHandlerParams());

            if (descriptor.getFormatter() != null) {

                handler.setFormatter(descriptor.getFormatter());
            }

            handler.setLevel(descriptor.getLevel());
            logger.addHandler(handler);
            logger.setLevel(descriptor.getLevel());
            logger.setUseParentHandlers(false);
        }
    }
    



    /**
     * Describe <code>release</code> method here.
     *
     */
    public void release() {

    }

    
    /**
     * Describe <code>getAttribute</code> method here.
     *
     * @param string a <code>String</code> value
     * @return an <code>Object</code>
     */
    public Object getAttribute(String string) {
        return null;
    }


    /**
     * Describe <code>removeAttribute</code> method here.
     *
     * @param string a <code>String</code> value
     */
    public void removeAttribute(String string) {

    }


    /**
     * Describe <code>getAttributeNames</code> method here.
     *
     * @return a <code>String[]</code>
     */
    public String[] getAttributeNames() {
        return null;
    }



    // Internal helper methods


    private void configure() {

        InputStream stream = null;

        try {
            String configPath =
                System.getProperty(
                    "org.vortikal.logging.config.file");

            if (configPath != null) {
                if (debug) {
                    System.out.println(
                        "VortikalLogFactory: Attempting to read logging " +
                        "configuration from file: " + configPath);
                }

                stream = new FileInputStream(configPath);
            } else {
                
                /* The property has not been set, check if there is a
                 * file "vortikal.logging.properties" in the classpath: */

                if (debug) {
                    System.out.println(
                        "VortikalLogFactory: Attempting toString read logging " +
                        "configuration from classpath resource 'vortikal.logging.properties'");
                }
                stream = this.getClass().getClassLoader().getResourceAsStream(
                    "vortikal.logging.properties");
            }
        } catch (IOException e) {
            System.err.println("VortikalLogFactory: Unable to configure logging: " +
                               e.getMessage());
            e.printStackTrace(System.err);
            return;
        }

        if (stream == null) {
            System.err.println("VortikalLogFactory: Unable to configure logging: " +
                               "No config found. Either set system property " +
                               "'org.vortikal.loggin.config.file' to point to " +
                               "a configuration file, or provide a configuration " +
                               "file in the classpath.");
        }

        if (stream != null) {
            try {
                readConfiguration(stream);
            } catch (IOException e) {
                System.err.println(
                    "VortikalLogFactory: Error reading logging configuration: " +
                    e.getMessage());
                e.printStackTrace(System.err);
            
            } finally {
                try {
                    stream.close();
                } catch (Exception e) {
                    System.err.println(
                        "VortikalLogFactory: Error reading logging configuration: " +
                        e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        }
    }
    


    private void readConfiguration(InputStream stream) throws IOException {


        StringBuffer javaConfig = new StringBuffer();

        this.properties.load(stream);
            
        for (Iterator i = this.properties.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();

            if ( key.indexOf(".") < 0 ) {

                constructLoggerDescriptor(key);
            }
        }

        if (javaConfig.length() > 0) {

            /* Feed the javaConfig buffer to the LogManager for
             * configuration (experimental): */
            
            LogManager.getLogManager().reset();
            LogManager.getLogManager().readConfiguration(
                new ByteArrayInputStream(javaConfig.toString().getBytes()));
        }

    }




    private void constructLoggerDescriptor(String alias) {

        String levelStr = this.properties.getProperty(alias + ".level");
        if (levelStr == null) {
            System.err.println("Missing logger property: " + alias + ".level");
            return;
        }

        Level level = defineLevel(levelStr);
        if (level == null) {
            System.err.println("Invalid logger level: " + levelStr);
            return;
        }

        String handlerStr = this.properties.getProperty(alias + ".handler");
        String handlerClass = getHandlerClass(handlerStr);
        if (handlerClass == null) {
            System.err.println("Unsupported logger: " + handlerStr);
            return;
        }
        String handlerParams = getHandlerParams(
            expandSystemProperties(handlerStr));
        

        String formatterStr = this.properties.getProperty(alias + ".formatter");
        Formatter formatter = null;

        if (formatterStr != null) {

            formatter = defineFormatter(formatterStr);
            if (formatter == null) {
                System.err.println(
                    "Invalid logger formatter: " + formatterStr);
                return;
            }
        }

        LoggerDescriptor descriptor = null;
        if (formatter != null) {
            descriptor = new LoggerDescriptor(level, handlerClass,
                                              handlerParams, formatter); 
        } else {
            descriptor = new LoggerDescriptor(level, handlerClass,
                                              handlerParams);
        }
        
        synchronized(this.loggerDescriptors) {

            this.loggerDescriptors.put(this.properties.getProperty(alias), descriptor);
        }
    }
    


    private LoggerDescriptor lookupDescriptor(String className) {

        String key = className;

        while (true) {

            if (this.loggerDescriptors.containsKey(key)) {
                return (LoggerDescriptor) this.loggerDescriptors.get(key);
            }
            if (key.indexOf(".") < 0) {
                break;
            }

            key = key.substring(0, key.lastIndexOf("."));
        }

        // An empty package name means 'match all' (if configured).
        return (LoggerDescriptor) this.loggerDescriptors.get("");
    }




    private Level defineLevel(String levelStr) {

        Level level = null;

        if (levelStr.equals("SEVERE")) {
            level = Level.SEVERE;
            
        } else if (levelStr.equals("WARNING")) {
            level = Level.WARNING;

        } else if (levelStr.equals("INFO")) {
            level = Level.INFO;

        } else if (levelStr.equals("CONFIG")) {
            level = Level.CONFIG;

        } else if (levelStr.equals("FINE")) {
            level = Level.FINE;

        } else if (levelStr.equals("FINER")) {            
            level = Level.FINER;

        } else if (levelStr.equals("FINEST")) {
            level = Level.FINEST;

        } else if (levelStr.equals("ALL")) {
            level = Level.ALL;
        }
         
        return level;
    }
    



//     private Handler defineHandler(String handlerStr) {

//         String handlerName = handlerStr;
//         String paramList = null;
            
//         if ( (handlerStr.indexOf("(") > 0) &&
//              (handlerStr.indexOf(")") > 0) ) {

//             handlerName = handlerName.substring(
//                 0, handlerName.indexOf("("));

//             paramList = handlerStr.substring(
//                 handlerStr.indexOf("(") + 1,
//                 handlerStr.lastIndexOf(")"));
//         }
            
//         if (isSupportedHandler(handlerName)) {

//             Handler handler = getHandler(handlerName, paramList);
//             return handler;
//         }

//         return null;
//     }
    

    private String getHandlerParams(String handlerStr) {
        String paramList = null;
            
        if ( (handlerStr.indexOf("(") > 0) &&
             (handlerStr.indexOf(")") > 0) ) {

            paramList = handlerStr.substring(
                handlerStr.indexOf("(") + 1,
                handlerStr.lastIndexOf(")"));
        }
        return paramList;
    }
    


    private String getHandlerClass(String handlerStr) {

        String handlerName = handlerStr;

        if ( (handlerStr.indexOf("(") > 0) &&
             (handlerStr.indexOf(")") > 0) ) {

            handlerName = handlerName.substring(
                0, handlerName.indexOf("("));
        }
            
        if (isSupportedHandler(handlerName)) {

            return handlerStr;
        }

        return null;
    }
    

    private Formatter defineFormatter(String formatterStr) {
        String formatterName = formatterStr;
        String paramList = null;
            
        if ( (formatterStr.indexOf("(") > 0) &&
             (formatterStr.indexOf(")") > 0) ) {

            formatterName = formatterStr.substring(
                0, formatterName.indexOf("("));

            paramList = formatterStr.substring(
                formatterStr.indexOf("(") + 1,
                formatterStr.lastIndexOf(")"));
        }

        if (FormatterManager.isSupportedFormatter(formatterName)) {

            Formatter formatter =
                FormatterManager.getFormatter(formatterName, paramList);
            return formatter;
        }

        return null;
    }
        



    private class LoggerDescriptor {
        private Level level;
        private String handler;
        private String handlerParams;
        private Formatter formatter = null;

        LoggerDescriptor(Level level, String handler, String handlerParams) {
            this.level = level;
            this.handler = handler;
            this.handlerParams = handlerParams;
        }

        LoggerDescriptor(Level level, String handler,
                         String handlerParams, Formatter formatter) {
            this.level = level;
            this.handler = handler;
            this.handlerParams = handlerParams;
            this.formatter = formatter;
        }

        public Level getLevel() {
            return this.level;
        }

        public String getHandler() {
            return this.handler;
        }
        
        public String getHandlerParams() {
            return this.handlerParams;
        }
        
        public Formatter getFormatter() {
            return this.formatter;
        }
    }
    

    private boolean isSupportedHandler(String className) {

        if (className.equals("java.util.logging.FileHandler")) {
            return true;
        } 

        if (className.equals("java.util.logging.ConsoleHandler")) {
            return true;
        }

        if (className.equals("org.vortikal.util.logging.FileHandler")) {
            return true;
        }

        return false;
    }


    private Handler getHandler(String className,
                                      String paramList) {
        if (className.equals("java.util.logging.ConsoleHandler")) {
            return new java.util.logging.ConsoleHandler();
        }

        if (className.startsWith("java.util.logging.FileHandler") ||
            className.startsWith("org.vortikal.util.logging.FileHandler")) {
            
            try {

                if (paramList == null || paramList.trim().equals("")) {

                    return new java.util.logging.FileHandler();
                }
                
                StringTokenizer tokenizer = new StringTokenizer(paramList);
                String pattern = null;
                if (tokenizer.hasMoreTokens()) {
                    pattern = stripComma(tokenizer.nextToken());

                    if (pattern.startsWith("\"")) {
                        pattern = pattern.substring(1, pattern.length());
                    }

                    if (pattern.endsWith("\"")) {
                        pattern = pattern.substring(0, pattern.length() - 1);
                    }

                    pattern = expandSystemProperties(pattern);
                }
            
                int limit = 0;
                if (tokenizer.hasMoreTokens()) {
                    limit = Integer.parseInt(stripComma(tokenizer.nextToken()));
                }
            
                int count = 0;
                if (tokenizer.hasMoreTokens()) {
                    count = Integer.parseInt(stripComma(tokenizer.nextToken()));
                }
            
                boolean append = false;
                if (tokenizer.hasMoreTokens()) {
                    append = Boolean.valueOf(
                        stripComma(tokenizer.nextToken())).booleanValue();
                }
                 
                if (className.startsWith("java.util.logging.FileHandler")) {
                    return new java.util.logging.FileHandler(
                        pattern, limit, count, append);
                }
                    
                return new org.vortikal.util.logging.FileHandler(
                    pattern, limit, count, append);


            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }


        return null;
    }
    


    /**
     * Replaces occurrences of <code>"%SYSTEM_PROPERTY{...}"</code>
     * with the actual system property in a string
     *
     * @param s the string to search 
     * @return the original string with the values replaced
     */
    private String expandSystemProperties(String s) {
        Pattern p = Pattern.compile("(%SYSTEM_PROPERTY\\{([^\\}]*)\\})");
        Matcher m = p.matcher(s);
        String result = s;

        while (m.find()) {
            String sysProp = System.getProperty(m.group(2), "");
            result = m.replaceFirst(sysProp);
            m.reset(result);
        }
        return result;
    }
    

    private String stripComma(String string) {

        if (string == null) {
            throw new IllegalArgumentException();
        }

        String s = string;
        
        if (s.charAt(s.length() - 1) == ',') {
            s = s.substring(0, s.length() - 1);
        }

        return s;
    }







}


