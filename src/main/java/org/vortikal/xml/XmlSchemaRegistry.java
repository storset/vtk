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

import org.jdom.Document;
import org.vortikal.util.cache.ContentCache;

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

    private ContentCache schemaCache;
    
    public void setSchemaCache(ContentCache schemaCache) {
        this.schemaCache = schemaCache;
    }

    /**
     * Gets an XML schema as a JDOM document from a URL. If a cached
     * copy of the schema is available, that copy is used.
     *
     * @param docType the schema identifier (URL)
     * @return a schema {@link Document}. If no schema could be
     * located, an exception is thrown.
     * @exception Exception if an error occurs
     */
    public Document getXMLSchema(String docType) throws Exception {
        return (Document) this.schemaCache.get(docType);
    }

}
