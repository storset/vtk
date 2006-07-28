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

package org.vortikal.web.controller.repository.tidy;

import groovy.util.ResourceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.tidy.Configuration;
import org.w3c.tidy.Tidy;

public class JTidyTransformerImpl implements Transformer { 
    
    private static Log logger = LogFactory.getLog(JTidyTransformerImpl.class);

    private static boolean tidyMark = false;
    private static boolean makeClean = true;
    // private static boolean onlyErrors = true;
    private static boolean showWarnings = false;
    private static boolean quiet = true;
    private static boolean xhtml = true;
    
    
    public JTidyTransformerImpl() {
        logger.debug("Instantiating new JTidyTransformerImpl object");
    }

    public InputStream transform(InputStream instream, String transformation)
            throws FileNotFoundException, ResourceException {
        
        if (transformation.equals("htmlToXhtml")) {
            logger.debug("Performing HTML to XHTML transformation on resource");
            return htmlToXhtml(instream);
        //else if (transformation.equals("TRANSFORMATION")) {
        } else {
            logger.warn("Unable to perform JTidy transformation on " +
                        "current resource");
            throw new ResourceException(
                    "Could not transform contents of current resource");
        }
    }

    public InputStream htmlToXhtml(InputStream is)
            throws FileNotFoundException {
        try {
            Tidy tidy = new Tidy();
            
            // Setting up Tidy (default) output
            tidy.setInputStreamName(is.getClass().getName());
            tidy.setTidyMark(tidyMark);
            tidy.setMakeClean(makeClean);
            tidy.setShowWarnings(showWarnings);
            // tidy.setOnlyErrors(onlyErrors); // If set TRUE, then only error
                                               // messages are written to the
                                               // OutputStream (i.e. no file
                                               // content is written)
            tidy.setQuiet(quiet);
            tidy.setXHTML(xhtml);
            tidy.setCharEncoding(Configuration.UTF8);
                        
            ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
            
            tidy.parse(is, outBuffer);
            
            byte[] byteArrayBuffer = outBuffer.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteArrayBuffer);
            
            outBuffer.close();
            bais.reset(); // must reset buffer pointer to [0]

            return bais;

        } catch (Exception e) {
            logger.error("Error: " + e.getMessage());
            return new ByteArrayInputStream(null);
        }
    }
}
