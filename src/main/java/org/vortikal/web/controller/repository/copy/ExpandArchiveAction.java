/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.controller.repository.copy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.util.web.URLUtil;

public class ExpandArchiveAction implements CopyAction {

    private Repository repository;
    private ResourceTypeTree resourceTypeTree;
    
    public void process(String uri, String copyUri) throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();

        Resource resource = this.repository.retrieve(token, uri, false);
        if (resource.isCollection()) {
            throw new RuntimeException("Cannot unzip a collection");
        }
        String base = copyUri;

        InputStream source = this.repository.getInputStream(token, uri, false);
        JarInputStream zis = new JarInputStream(new BufferedInputStream(source));
        JarEntry entry;
        Set<String> dirCache = new HashSet<String>();
        String[] basePath = URLUtil.splitUriIncrementally(base);
        for (int i = 0; i < basePath.length - 1; i++) {
            dirCache.add(basePath[i]);
        }         

        while((entry = zis.getNextJarEntry()) != null) {
            String entryPath = entry.getName();
            String resourceURI = entryPath.startsWith("/") ? 
                    base + entryPath : base + "/" + entryPath; 
            if (resourceURI.endsWith("/")) {
                resourceURI = resourceURI.substring(0, resourceURI.length() - 1);
            }
            
            String dir = entry.isDirectory() ? 
                    resourceURI : URIUtil.getParentURI(resourceURI);
            createDirectoryStructure(token, dir, dirCache);

            if (!entry.isDirectory()) {
                writeFile(token, resourceURI, zis);
            }
            storeProps(token, entry, resourceURI);
        }
        zis.close();
    }

    private void storeProps(String token, JarEntry entry, String resourceURI) throws Exception {
        Attributes attributes = entry.getAttributes();
        if (attributes == null) {
            return;
        }

        Resource resource = this.repository.retrieve(token, resourceURI, false);
        boolean modified = false;
        
        for (Object key : attributes.keySet()) {
            
            String name = key.toString();
            if (name.startsWith("X-vrtx-prop-")) {
                String propName = name.substring("X-vrtx-prop-".length());
                String prefix = null;
                int underscoreIdx = propName.indexOf('_');
                if (underscoreIdx > 0) {
                    prefix = propName.substring(0, underscoreIdx);
                    propName = propName.substring(underscoreIdx + 1);
                }

                PropertyTypeDefinition propDef;
                if (prefix != null) {
                    propDef = this.resourceTypeTree.getPropertyDefinitionByPrefix(prefix, propName);
                } else {
                    Namespace ns = Namespace.DEFAULT_NAMESPACE;
                    propDef = this.resourceTypeTree.getPropertyTypeDefinition(ns, propName);
                }

                if (propDef != null) {
                    String valueString = (String) attributes.get(key);
             
                    Property prop = resource.getProperty(propDef);
                    if (prop == null) {
                        prop = resource.createProperty(propDef);
                    }
                    ValueFormatter valueFormatter = propDef.getValueFormatter();
                    String format = null;
                    if (propDef.getType() == PropertyType.Type.DATE) {
                        format = "iso-8601";
                    }
                    
                    if (propDef.isMultiple()) {

                        List<Value> values = new ArrayList<Value>();
                        String[] splitValues = valueString.split(",");
                        for (String val : splitValues) {
                            values.add(valueFormatter.stringToValue(val.trim(), format, null));
                        }
                        prop.setValues(values.toArray(new Value[values.size()]));
                    } else {
                        prop.setValue(valueFormatter.stringToValue(valueString.trim(), format, null));
                    }
                    modified = true;
                }
            }
        }
        if (modified) {
            this.repository.store(token, resource);
        }
    }
    
    private void writeFile(String token, String uri, ZipInputStream is) throws Exception {
        this.repository.createDocument(token, uri);
        this.repository.storeContent(token, uri, new PartialZipStream(is));
    }
    
    private class PartialZipStream extends InputStream {
        ZipInputStream in;
        PartialZipStream(ZipInputStream in) {
            this.in = in;
        }
        public int available() throws IOException {
            return this.in.available();
        }

        public void close() throws IOException {
        }

        public void mark(int readLimit) {
            this.in.mark(readLimit);
        }

        public void reset() throws IOException {
            this.in.reset();
        }

        public boolean markSupported() {
            return false;
        }
        
        public int read() throws IOException {
            return this.in.read();
        }

        public int read(byte[] b) throws IOException {
            return this.in.read(b);
        }
        
        public int read(byte[] b, int off, int len) throws IOException {
            return this.in.read(b, off, len);
        }

        public long skip(long n) throws IOException {
            return this.in.skip(n);
        }
    }

    private void createDirectoryStructure(String token, String dir, Set<String> dirCache) throws Exception {
        String[] path = URLUtil.splitUriIncrementally(dir);
        for (String elem : path) {
            if (!dirCache.contains(elem)) {
                this.repository.createCollection(token, elem);
                dirCache.add(elem);
            }
        }
    }
    
    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    
}

