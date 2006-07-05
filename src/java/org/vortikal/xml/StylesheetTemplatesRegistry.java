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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;

import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.security.AuthenticationException;



/**
 * Locates, compiles and caches stylsheet templates. Supports URLs (http and
 * file) as well as absolute and relative repository paths.
 * 
 * External url stylesheets cannot be cached
 */
public class StylesheetTemplatesRegistry {

    private Map cachedItems = new HashMap();


    public Templates compile(String stylesheetPath, URIResolver uriResolver,
                             Date compilationTime)
        throws IOException, ResourceNotFoundException, AuthorizationException,
        AuthenticationException, ResourceLockedException, TransformerException,
        TransformerConfigurationException {

        if (stylesheetPath == null || stylesheetPath.trim().equals("")) { 
            throw new IllegalArgumentException("stylesheetPath cannot be empty");
        }

        Templates t = compileInternal(stylesheetPath, uriResolver);
        if (compilationTime == null) {
            return t;
        }
        this.cachedItems.put(stylesheetPath, new Item(t, compilationTime));
        
        return t; 
    }



    public Templates getTemplates(String stylesheetPath) {
        Item item = (Item) this.cachedItems.get(stylesheetPath);
        if (item == null) return null;
        return item.getTemplates();
    }
    


    public Date getLastModified(String stylesheetPath) {
        Item item = (Item) this.cachedItems.get(stylesheetPath);
        if (item == null) return null;
        return item.getLastModified();
    }
    


    public void flush() {
        this.cachedItems.clear();
    }
    


    private Templates compileInternal(String stylesheetPath,
                                      URIResolver uriResolver) throws
        IOException, TransformerException, TransformerConfigurationException {

        TransformerFactory factory = TransformerFactory.newInstance();
        //Source source = stylesheetResolver.getSource(stylesheetPath);
        Source source = uriResolver.resolve(stylesheetPath, null);
        if (uriResolver != null) {
            factory.setURIResolver(uriResolver);
        }
        return factory.newTemplates(source);
    }



//     private class RelativeURIResolver implements URIResolver {

//         private URIResolver parent;
//         private String origin;


//         public RelativeURIResolver(TransformerFactory f, String stylesheetPath) {

//             this.parent = f.getURIResolver();

//             File file = new File(stylesheetPath);
//             this.origin = file.getParent();
//         }


//         public Source resolve(String href, String base)
//             throws TransformerException {

//             if (new File(href).isAbsolute()) {
//                 return parent.resolve(href, base);
//             }

//             try {

//                 URL url = new URL(origin + "/" + href);
//                 return new StreamSource(url.openStream());

//             } catch (Exception e) {

//                 throw new TransformerException(e);
//             }
//         }
//     }


    private class Item {

        private Date lastModified;
        private Templates templates;


        public Item(Templates templates, Date lastModified) {
            this.templates = templates;
            this.lastModified = lastModified;
        }


        public Date getLastModified() {
            return this.lastModified;
        }


        public Templates getTemplates() {
            return this.templates;
        }
    }


}
