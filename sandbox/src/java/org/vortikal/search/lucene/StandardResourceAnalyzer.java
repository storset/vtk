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
package org.vortikal.search.lucene;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.vortikal.repository.Ace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.search.SearchException;
import org.vortikal.util.io.StreamUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;




public class StandardResourceAnalyzer implements ResourceAnalyzer {

    private static Log logger = LogFactory.getLog(StandardResourceAnalyzer.class);
    



    public void processResource(Resource resource, Repository repository,
                                String token, Document doc) {
        doc.add(Field.Keyword("uri", resource.getURI()));

        String folders = LuceneIndexer.getFolders(resource.getURI());
        if (folders != null) {
            doc.add(Field.Keyword("folders", folders));
        }

        doc.add(Field.Keyword("owner", resource.getOwner().getQualifiedName()));
        doc.add(Field.Keyword("modifiedBy", resource.getModifiedBy().getQualifiedName()));

        try {
            Ace[] acl = repository.getACL(token, resource.getURI());
            // FIXME:
            boolean inheritedACL = (acl[0].getInheritedFrom() != null);
            doc.add(Field.Keyword("inheritedACL",
                                  new Boolean(inheritedACL).toString()));
        } catch (IOException e) {
            logger.warn("Errors processing resource " + resource.getURI() +
                        " for indexing. Unable to load ACL", e);
        }
        doc.add(Field.Keyword("creationTime",
                              DateField.timeToString(
                                  resource.getCreationTime().getTime())));
        doc.add(Field.Keyword("lastmodified",
                              DateField.timeToString(
                                  resource.getLastModified().getTime())));
        doc.add(Field.Keyword("name", resource.getName()));
        doc.add(Field.Keyword("displayName", resource.getDisplayName()));
        doc.add(Field.Keyword("contentType", resource.getContentType()));
        
        
        // Add custom properties:
        Property[] properties = resource.getProperties();
        for (int i = 0; i < properties.length; i++) {
            String key = properties[i].getName() + ":" +
                properties[i].getNamespace();
            String value = properties[i].getValue();
            logger.debug("Adding custom property " + key + " = " + value + 
                         " to indexed resource " + resource.getURI());
            doc.add(Field.Keyword(key, value));
        }
        
        try {
            
            if (resource.getContentType() != null
                && resource.getContentType().matches("text/.+")) {
            
                // Add the 100 first characters into a `summary' field:
                logger.debug("Reading input stream using token " + token);
                InputStream is = repository.getInputStream(token, resource.getURI(), false);
                byte[] bytes = StreamUtil.readInputStream(is);
                String fulltext = new String(bytes);
                int len = fulltext.length();
                if (len > 100) {
                    len = 100;
                }
                //doc.add(Field.Text("summary", fulltext.substring(0, len)));
                doc.add(Field.UnIndexed("summary", fulltext.substring(0, len)));

                Reader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(bytes)));
                doc.add(Field.Text("contents", reader));
                //logger.debug("added contents: " + fulltext);
            }

        } catch (IOException e) {
            SearchException se = new SearchException(
                "Creating index for resource " + resource.getURI() +
                " failed: "  + e.getMessage());
            se.fillInStackTrace();
            throw se;
        }
    }

}
