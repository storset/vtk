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
package org.vortikal.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 */
public class XmlSchemaRegistry {

    private static Log logger = LogFactory.getLog(XmlSchemaRegistry.class);
    private Map schemas = new HashMap();
    private long cacheTimeout = 10 * 60 * 1000;
    
    
    public void setCacheTimeoutSeconds(long cacheTimeoutSeconds) {
        this.cacheTimeout = cacheTimeoutSeconds * 1000;
    }
    
    public void flush() {
        schemas.clear();
    }
    


    /**
     * Gets an XML schema as a JDOM document from a URL. If a cached
     * copy of the schema is available and that copy is less than 10
     * minutes old, it is returned. 
     *
     * @param docType a <code>String</code> value
     * @return a schema <code>Document</code>. If no schema could be
     * located, <code>null</code> is returned.
     * @exception JDOMException if an error occurs
     * @exception MalformedURLException if an error occurs
     */
    public Document getXMLSchema(String docType) throws JDOMException,
        MalformedURLException, IOException {
        SchemaItem item = (SchemaItem) schemas.get(docType);
        long now = new Date().getTime();

        if (item == null ||
            (item.getTimestamp().getTime() + cacheTimeout < now)) {

            try {
                InputStream inStream = (new URL(docType)).openStream();
                Document schemaDoc = new SAXBuilder().build(inStream);
                schemas.put(docType, new SchemaItem(schemaDoc));
                logger.info("Cached new copy of XML schema " + docType);

            } catch (IOException e) {
                logger.warn("Error trying to cache new copy of XML " +
                            "schema " + docType, e);
                if (item == null) {
                    IOException ex = new IOException("Error retrieving XML schema " + docType);
                    ex.setStackTrace(e.getStackTrace());
                    throw ex;
                }
                logger.warn("Using cached version of schema " + docType);
            }
        }

        item = (SchemaItem) schemas.get(docType);
        return item.getDocument();
    }

    
    /**
     * Class for holding a cached XML schema and a corresponding
     * timestamp.
     */
    private class SchemaItem {
        private Document doc;
        private Date timestamp;

        public SchemaItem(Document doc) {
            this.doc = doc;
            timestamp = new Date();
        }

        public Document getDocument() {
            return doc;
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }

    
}
