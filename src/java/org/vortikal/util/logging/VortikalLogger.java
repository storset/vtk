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

import java.util.logging.Handler;
import java.util.logging.Level;

import org.apache.commons.logging.Log;


/**
 * Implementation of the commons-logging <code>Log</code> interface.
 * This class utilizes the standard Java Logging API, allocating
 * loggers of the class <code>java.util.logging.Logger</code> to do
 * the actual work of logging.
 * 
 * <p>Unless configured using the <code>addHandler()</code>,
 * <code>setLevel()</code> and <code>setUseParentHandlers()</code>
 * methods, this logger behave equivalent to a
 * <code>java.util.logging.Logger</code> obtained using
 * <code>java.util.logging.Logger.getAnonymousLogger()</code>.
 *
 * <p>The preferred way to use the VortikalLogger is by specifying the
 * commons-logging <code>LogFactory</code> to
 * <code>org.vortikal.util.logging.VortikalLogFactory</code>, which has
 * its own configuration and will produce <code>VortikalLogger</code>
 * instances.
 */
public class VortikalLogger implements Log {

    private String name;
    private java.util.logging.Logger logger = java.util.logging.Logger.getAnonymousLogger();
    
    
    public VortikalLogger(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
    
    /**
     * Adds a handler to this logger.
     *
     * @param handler a <code>Handler</code> value
     */
    public void addHandler(Handler handler) {
        this.logger.addHandler(handler);
    }
    
    /**
     * Sets this logger's level.
     *
     * @param level a <code>Level</code> value
     */
    public void setLevel(Level level) {
        this.logger.setLevel(level);
    }

    /**
     * Specify whether this logger should also write to its parent.
     *
     * @param useParentHandlers a <code>boolean</code> value
     */
    public void setUseParentHandlers(boolean useParentHandlers) {
        this.logger.setUseParentHandlers(useParentHandlers);
    }
    

    public void trace(Object message) {
        this.logger.logp(java.util.logging.Level.FINER, this.name, "",
                    "" + message);
    }

    public void trace(Object message, Throwable t) {
        this.logger.logp(java.util.logging.Level.FINER, this.name, "",
                    "" + message, t);
    }
    

    public void debug(Object message) {
        this.logger.logp(java.util.logging.Level.FINE, this.name, "",
                    "" + message);
    }


    public void debug(Object message, Throwable t) {
        this.logger.logp(java.util.logging.Level.FINE, this.name, "",
                    "" + message, t);
    }



    public void info(Object message) {
        this.logger.logp(java.util.logging.Level.INFO, this.name, "",
                    "" + message);
    }
    


    public void info(Object message, Throwable t) {
        this.logger.logp(java.util.logging.Level.INFO, this.name, "",
                    "" + message, t);
    }
    


    public void warn(Object message) {
        this.logger.logp(java.util.logging.Level.WARNING, this.name, "",
                    "" + message);
    }
    


    public void warn(Object message, Throwable t) {
        this.logger.logp(java.util.logging.Level.WARNING, this.name, "",
                    "" + message, t);
    }
    


    public void error(Object message) {
        this.logger.logp(java.util.logging.Level.SEVERE, this.name, "",
                    "" + message);
    }
    


    public void error(Object message, Throwable t) {
        this.logger.logp(java.util.logging.Level.SEVERE, this.name, "",
                    "" + message, t);
    }
    


    public void fatal(Object message) {
        this.logger.logp(java.util.logging.Level.SEVERE, this.name, "",
                    "" + message);
    }



    public void fatal(Object message, Throwable t) {
        this.logger.logp(java.util.logging.Level.SEVERE, this.name, "",
                    "" + message, t);
    }


    public boolean isTraceEnabled() {
        return this.logger.isLoggable(java.util.logging.Level.FINEST);
    }
    

    public boolean isDebugEnabled() {
        return this.logger.isLoggable(java.util.logging.Level.FINE);
    }
    

    public boolean isInfoEnabled() {
        return this.logger.isLoggable(java.util.logging.Level.INFO);
    }
    

    public boolean isWarnEnabled() {
        return this.logger.isLoggable(java.util.logging.Level.WARNING);
    }
    

    public boolean isFatalEnabled() {
        return this.logger.isLoggable(java.util.logging.Level.SEVERE);
    }

    public boolean isErrorEnabled() {
        return this.logger.isLoggable(java.util.logging.Level.SEVERE);
    }
}


