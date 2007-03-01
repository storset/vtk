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
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * A store for XML schemas. The schemas are cached as JDOM {@link
 * Document} objects for a configurable period to avoid unnecessary
 * network traffic and XML parsing before being refreshed.
 *
 * <p>The method {@link #refresh()} needs to be called periodically in
 * order for the cache mechanism to work.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>cacheTimeoutSeconds</code> - the number of seconds to
 *   cache schemas before refreshing.
 * </ul>
 */
public class XmlSchemaRegistry {

    private static Log logger = LogFactory.getLog(XmlSchemaRegistry.class);
    private Map cache = new HashMap();
    private long cacheTimeout = 30 * 60 * 1000;
    
    
    public void setCacheTimeoutSeconds(long cacheTimeoutSeconds) {
        this.cacheTimeout = cacheTimeoutSeconds * 1000;
    }
    
    /**
     * Gets an XML schema as a JDOM document from a URL. If a cached
     * copy of the schema is available, that copy is used.
     *
     * @param docType the schema identifier (URL)
     * @return a schema {@link Document}. If no schema could be
     * located, <code>null</code> is returned.
     * @exception JDOMException if the schema is not a valid XML document
     * @exception IOException if an error occurs
     */
    public Document getXMLSchema(String docType)
        throws JDOMException, IOException {
        
        SchemaItem item = (SchemaItem) this.cache.get(docType);

        if (item != null) {
            return item.getDocument();
        }
        
        cacheXMLSchema(docType);
        item = (SchemaItem) this.cache.get(docType);

        if (item == null) {
            throw new IOException("Unable to find XML schema " + docType);
        }
        
        return item.getDocument();
    }
    


    /**
     * Finds all expired schema items and tries to fetch new copies of
     * them.
     */
    public synchronized void refresh() {
        List refreshList = new ArrayList();
        
        for (Iterator i = this.cache.keySet().iterator(); i.hasNext();) {
            String url = (String) i.next();
            SchemaItem item = (SchemaItem) this.cache.get(url);

            long now = new Date().getTime();
            if (item.getTimestamp().getTime() + this.cacheTimeout < now) {

                refreshList.add(url);
            }
        }

        for (Iterator i = refreshList.iterator(); i.hasNext();) {
            String url = (String) i.next();
            try {

                cacheXMLSchema(url);

            } catch (Throwable t) {
                logger.warn("Unable to cache new copy of XML schema " + url, t);
            }
        }
    }
    

    public synchronized void flush() {
        this.cache.clear();
    }
    


    private synchronized void cacheXMLSchema(String docType) throws JDOMException,
        IOException {

        SchemaItem item = (SchemaItem) this.cache.get(docType);
        long now = new Date().getTime();

        if (item == null ||
            (item.getTimestamp().getTime() + this.cacheTimeout < now)) {

            InputStream inStream = (new URL(docType)).openStream();
            Document schemaDoc = new SAXBuilder().build(inStream);
            this.cache.put(docType, new SchemaItem(schemaDoc));
            logger.info("Cached new copy of XML schema " + docType);
        }
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
            this.timestamp = new Date();
        }

        public Document getDocument() {
            return this.doc;
        }

        public Date getTimestamp() {
            return this.timestamp;
        }
    }

}
