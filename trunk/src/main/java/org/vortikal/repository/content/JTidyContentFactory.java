/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repository.content;

import java.io.OutputStream;
import java.io.PrintWriter;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;


/**
 * Content factory for <code>org.w3c.dom.Document</code> objects built
 * using <a href="http://jtidy.sourceforge.net/">JTidy</a>.
 */
public class JTidyContentFactory implements ContentFactory {

    public Class<?>[] getRepresentationClasses() {
        return new Class[] {Document.class, Tidy.class};
    }
    
    private static PrintWriter NULL_WRITER = new PrintWriter(new NullOutputStream());

    public Object getContentRepresentation(Class<?> clazz,  InputStreamWrapper content) throws Exception {
        Tidy tidy = new Tidy();
        tidy.setQuiet(true);
        tidy.setOnlyErrors(true);
        tidy.setShowWarnings(false);
        tidy.setErrout(NULL_WRITER);
        
        try {
            Document document = tidy.parseDOM(content.getInputStream(), null);

            if (clazz == Document.class) {
                return document;
            } else if (clazz == Tidy.class) {
                return tidy;
            } else {
                throw new UnsupportedContentRepresentationException(
                    "Class " + clazz.getName() + " not supported by this content factory");
            }
        } finally {
            // Not sure whether JTidy closes the file correctly on
            // errors
            content.getInputStream().close();
        }
    }
    
    private static class NullOutputStream extends OutputStream {

        public void close() { }

        public void flush() { }

        public void write(byte[] b) { }

        public void write(byte[] b, int off, int len) { }

        public void write(int b) { }
    }

    
}
