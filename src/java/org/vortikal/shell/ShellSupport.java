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
package org.vortikal.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;



/**
 * Utility class for adding line-based shell/scripting support to the
 * framework. This is useful in many cases, i.e. for "live" bean
 * management, or for debugging. This class is a convenient superclass
 * for creating new scripting interfaces.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li>initScripts - a list of resources to evaluate as scripts
 *       during startup
 *   <li>commandReader - a <code>CommandReader</code> to use for
 *       reading input
 *   <li>outputter - a <code>PrintStream</code> to write output to
 *   <li>runEvalLoop - a <code>boolean</code> indicating whether to
 *       run a read - eval - print loop, or just the init scripts.
 * </ul>
 * 
 * TODO: add configurable custom (string) bindings that are passed to
 * the shell on startup.
 * @version $Id$
 */
public abstract class ShellSupport
  implements ApplicationContextAware, InitializingBean, DisposableBean, BeanNameAware, ResourceLoaderAware {

    protected Log logger = LogFactory.getLog(this.getClass());
    private String beanName = null;
    private ApplicationContext context = null;
    private String[] initFiles = null;
    private ManagerThread managerThread = null;
    private CommandReader reader = new ConsoleReader();
    private PrintStream outputter = System.out;
    private ResourceLoader resourceLoader = null;
    private boolean runEvalLoop = true;


    /**
     * Init method, called after properties are set, to allow custom
     * initialization.
     */
    protected abstract void init();
    

    /**
     * Allows for evaluation of an init file at startup. Called after
     * the <code>init()</code> method.
     *
     * @param inStream an <code>InputStream</code> containing the code
     * to evaluate.
     */
    protected abstract void evalInitFile(InputStream inStream, PrintStream out);


    /**
     * Binds a variable in the shell (if it is supported).
     *
     * @param name the name of the variable
     * @param o the object to bind to the name
     */
    protected abstract void bind(String name, Object o);


    /**
     * Evaluate a single line of input.
     *
     * @param line the line to evaluate.
     * @param out the PrintStream to write output to
     */
    protected abstract void eval(String line, PrintStream out);
    

    public final void setBeanName(String name) {
        this.beanName = name;
    }


    public final void setInitFiles(String[] initFiles) {
        this.initFiles = initFiles;
    }


    public final void setApplicationContext(ApplicationContext ctx) {
        this.context = ctx;
    }
    

    public final void setCommandReader(CommandReader reader) {
        this.reader = reader;
    }
    
    
    public final void setOutputter(PrintStream out) {
        this.outputter = out;
    }
    
    
    public final void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    public final void setRunEvalLoop(boolean runEvalLoop) {
        this.runEvalLoop = runEvalLoop;
    }
    

    public final void afterPropertiesSet() throws IOException {

        this.init();

        if (logger.isDebugEnabled())
            logger.debug("Binding: 'context'");
        this.bind("context", this.context);
        if (logger.isDebugEnabled())
            logger.debug("Binding: 'resourceLoader'");
        this.bind("resourceLoader", this.resourceLoader);
        if (logger.isDebugEnabled())
            logger.debug("Binding: 'logger'");
        this.bind("resourceLoader", this.logger);

        if (this.initFiles != null) {
            for (int i = 0; i < initFiles.length; i++) {
                Resource resource = this.resourceLoader.getResource(initFiles[i]);
                InputStream stream = null;
                try {
                    stream = resource.getInputStream();
                    if (logger.isDebugEnabled())
                        logger.debug("Evaluating init file " + resource);
                    this.evalInitFile(stream, outputter);
                
                } catch (IOException e) {
                    logger.warn("Cannot resolve init file path '" +
                                initFiles[i] + "'", e);
                } finally {
                    try {
                        if (stream != null) stream.close();
                    } catch (IOException e) {
                        logger.warn("Error closing input stream for resource '" +
                                    initFiles[i] + "'", e);
                    }
                }
            }

        }

        if (this.runEvalLoop) {
            this.managerThread = new ManagerThread(
                this.getClass().getName() + "[id: " + this.beanName + "]");
            this.managerThread.start();
        }
    }
    


    public final void destroy() {
        if (this.managerThread != null) {
            this.managerThread.kill();
        }
    }




    private class ManagerThread extends Thread {

        private boolean alive = true;
        
        public ManagerThread(String name) {
            super(name);
        }
        
        public void kill() {
            logger.info("Shutting down thread " + this.getName());
            alive = false;
            this.interrupt();
        }
        
        public void run() {
            
            while (alive) {
                try {
                    String line = reader.readLine(outputter);
                    if (alive) {
                        eval(line, outputter);
                    }
                } catch (Throwable t) {
                    outputter.println("Error: " + t.getMessage());
                    t.printStackTrace(outputter);
                }
            }
            outputter.println("Exiting");
        }
    }
}
