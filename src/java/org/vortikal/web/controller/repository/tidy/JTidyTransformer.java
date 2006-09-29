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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.repository.Resource;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.util.text.TextUtils;
import org.vortikal.web.controller.repository.copy.Filter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Configuration;
import org.w3c.tidy.Tidy;

public class JTidyTransformer implements Filter { 
    
    private static Log logger = LogFactory.getLog(JTidyTransformer.class);

    private static boolean tidyMark = false;
    private static boolean makeClean = true;
    // private static boolean onlyErrors = true;
    private static boolean showWarnings = false;
    private static boolean quiet = true;
    private static boolean xhtml = true;
    private static String doctype = "transitional"; // "loose" will also be equivalent
    


    public InputStream transformOLD(InputStream inStream, Resource resource) {
        
        try {
            Tidy tidy = new Tidy();
                        
            // Setting up Tidy (default) output
            tidy.setInputStreamName(inStream.getClass().getName());
            tidy.setTidyMark(tidyMark);
            tidy.setMakeClean(makeClean);
            tidy.setShowWarnings(showWarnings);
            // tidy.setOnlyErrors(onlyErrors); // If set TRUE, then only error
                                               // messages are written to the
                                               // OutputStream (i.e. no file
                                               // content is written)
            tidy.setQuiet(quiet);
            tidy.setXHTML(xhtml);
            tidy.setDocType(doctype); 

            // XXX: evaluate this handling
            // Default to utf-8
            tidy.setCharEncoding(Configuration.UTF8);
            // Trying to change to iso-8859-1 if relevant, and delete user set char encoding
            try {
                String encoding = resource.getCharacterEncoding();
                if (Charset.forName("ISO-8859-1").equals(Charset.forName(encoding)))
                        tidy.setCharEncoding(Configuration.LATIN1);
                else if (!Charset.forName("UTF-8").equals(Charset.forName(encoding)))
                    resource.setUserSpecifiedCharacterEncoding(null);
            } catch (Exception e) {
                // XXX: Ignore for now...
            }
            
            ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
            
            tidy.parse(inStream, outBuffer);
            byte[] byteArrayBuffer = outBuffer.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteArrayBuffer);
            
            outBuffer.close();
            bais.reset(); // must reset buffer pointer to [0]

            return bais;

        } catch (IOException e) {
            logger.error("Caught exception", e);
            return new ByteArrayInputStream(null);
        }
    }

    public InputStream transform(InputStream inStream, Resource resource) {
        
        try {
            Tidy tidy = new Tidy();
                        
            // Setting up Tidy (default) output
            tidy.setInputStreamName(inStream.getClass().getName());
            tidy.setTidyMark(tidyMark);
            tidy.setMakeClean(makeClean);
            tidy.setShowWarnings(showWarnings);
            // tidy.setOnlyErrors(onlyErrors); // If set TRUE, then only error
                                               // messages are written to the
                                               // OutputStream (i.e. no file
                                               // content is written)
            tidy.setQuiet(quiet);
            tidy.setXHTML(xhtml);
            tidy.setDocType(doctype); 
            tidy.setCharEncoding(Configuration.UTF8);

            byte[] buffer = StreamUtil.readInputStream(inStream);
            String s = new String(buffer, resource.getCharacterEncoding());
            InputStream newStream = StreamUtil.stringToStream(s, "utf-8");

            Document document = tidy.parseDOM(newStream, null);
            alterContentTypeMetaElement(document);
            resource.setUserSpecifiedCharacterEncoding(null);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            tidy.pprint(document, outputStream);
            
            byte[] byteArrayBuffer = outputStream.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteArrayBuffer);
            
            outputStream.close();
            bais.reset(); // must reset buffer pointer to [0]
            

            return bais;

        } catch (IOException e) {
            logger.error("Caught exception", e);
            return new ByteArrayInputStream(null);
        }
    }

    


    private void alterContentTypeMetaElement(Document doc) {
        String encoding = null;

        NodeList headNodes = doc.getElementsByTagName("head");
        Node head = headNodes.item(0);
        Node remove = null;

        NodeList metaElements = doc.getElementsByTagName("meta");
        for (int i = 0; i < metaElements.getLength(); i++) {
            Node meta = metaElements.item(i);

            if (meta.hasAttributes()) {
                NamedNodeMap attrMap = meta.getAttributes();
                Node httpEquivAttr = attrMap.getNamedItem("http-equiv");

                if (httpEquivAttr != null) {
                    String httpEquiv = httpEquivAttr.getNodeValue();

                    if (httpEquiv!= null && "Content-Type".toLowerCase().equals(httpEquiv.toLowerCase())) {
                        remove = meta;
                        break;
                    }
                }
            }
        }
        if (remove != null) {
            remove.getParentNode().removeChild(remove);
        }
        Element meta = doc.createElement("meta");
        meta.setAttribute("http-equiv", "Content-Type;charset=utf-8");
        head.appendChild(meta);
    }


}
