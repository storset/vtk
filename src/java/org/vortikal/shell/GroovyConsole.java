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

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;

import java.io.InputStream;
import java.io.PrintStream;




/**
 * Shell implementation using Groovy as its interpreter.
 */
public class GroovyConsole extends AbstractConsole {


    private GroovyShell shell = null;

    
    protected void init() {
        Binding binding = new Binding();
        this.shell = new GroovyShell(binding);
    }
    

    public void bind(String name, Object o) {
        try {
            shell.getContext().setVariable(name, o);
        } catch (GroovyRuntimeException e) {
            logger.warn("Binding error", e);
        }
    }
    


    protected void evalInitFile(InputStream inStream, PrintStream out) {
        try {
            shell.evaluate(inStream);
        } catch (Throwable t) {
            logger.warn("Evaluation error", t);
        }
    }
    

    public void eval(String line, PrintStream out) {
        try {
            if (line != null && !"".equals(line.trim())) {
                Object o = shell.evaluate(line);
                out.println("\n[" + o + "]");
            }
        } catch (GroovyRuntimeException e) {
            out.println(e.getMessage());
            logger.warn("Runtime error", e);

        } catch (Throwable t) {
            out.println(t.getMessage());
            logger.warn("Evaluation error", t);
        }
    }
}
