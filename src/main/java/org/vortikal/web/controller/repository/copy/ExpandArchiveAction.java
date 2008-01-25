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
import java.util.jar.Manifest;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Acl;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.security.Principal;
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
        JarInputStream jarIn = new JarInputStream(new BufferedInputStream(source));
        Manifest manifest = jarIn.getManifest();
        Attributes mainAttributes = manifest.getMainAttributes();

        String archiveVersion = mainAttributes.getValue("X-vrtx-archive-version"); 
        if (archiveVersion != null && !"1.0".equals(archiveVersion)) {
            throw new RuntimeException("Incompatible archive version: " + archiveVersion);
        }
            
        boolean decodeValues = 
        "true".equals(mainAttributes.getValue("X-vrtx-archive-encoded"));

        JarEntry entry;
        Set<String> dirCache = new HashSet<String>();
        String[] basePath = URLUtil.splitUriIncrementally(base);
        for (int i = 0; i < basePath.length - 1; i++) {
            dirCache.add(basePath[i]);
        }         

        while((entry = jarIn.getNextJarEntry()) != null) {
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
                writeFile(token, resourceURI, jarIn);
            }
            storePropsAndPermissions(token, entry, resourceURI, decodeValues);
        }
        jarIn.close();
    }

    
    private void storePropsAndPermissions(String token, JarEntry entry, String resourceURI, boolean decode) throws Exception {
        Attributes attributes = entry.getAttributes();
        if (attributes == null) {
            return;
        }

        Resource resource = this.repository.retrieve(token, resourceURI, false);
        boolean propsModified = false;
        boolean aclModified = false;
        
        for (Object key : attributes.keySet()) {
            
            String name = key.toString();
            if (name.startsWith("X-vrtx-prop-")) {
                if (setProperty(resource, name, attributes, decode)) {
                    propsModified = true;
                }
            } else if (name.startsWith("X-vrtx-acl")) {
                if (setAclEntry(resource, name, attributes, decode)) {
                    aclModified = true;
                }
            }
        }
        if (propsModified) {
            this.repository.store(token, resource);
        }
        if (aclModified) {
            this.repository.storeACL(token, resource);
        }
    }

    private boolean setProperty(Resource resource, String name, Attributes attributes, boolean decode) throws Exception {
        String valueString = attributes.getValue(name);
        if (decode) {
            valueString = decodeValue(valueString);
        }
        PropertyTypeDefinition propDef = parsePropDef(valueString);
        if (propDef == null) return false;
        String rawValue = parseRawValue(valueString);
        if (rawValue == null || "".equals(rawValue)) return false;

        Property prop = resource.getProperty(propDef);
        if (prop == null) {
            prop = resource.createProperty(propDef);
        }
        ValueFormatter valueFormatter = propDef.getValueFormatter();
        String format = null;
        if (propDef.getType() == PropertyType.Type.DATE || propDef.getType() == PropertyType.Type.TIMESTAMP) {
            format = "yyyy-MM-dd'T'HH:mm:ssZZ";
        }

        if (propDef.isMultiple()) {
            List<Value> values = new ArrayList<Value>();
            String[] splitValues = rawValue.split(",");
            for (String val : splitValues) {
                values.add(valueFormatter.stringToValue(val.trim(), format, null));
            }
            prop.setValues(values.toArray(new Value[values.size()]));
        } else {
            prop.setValue(valueFormatter.stringToValue(rawValue.trim(), format, null));
        }
        return true;
    }
    
    private PropertyTypeDefinition parsePropDef(String valueString) {
        if (!valueString.startsWith("prefix:")) {
            return null;
        }
            
        String prefix = null;
        int idx = "prefix:".length();
        if (valueString.charAt(idx) == ' ') {
            idx++;
        } else {
            int i = idx;
            while (valueString.charAt(i) != ' ') {
                i++;
            }
            prefix = valueString.substring("prefix:".length(), i);
            idx = i + 1;
        }

        String name = null;

        if (!valueString.substring(idx).startsWith("name:")) {
            return null;
        }
        idx += "name:".length();
        int nameEndIdx = idx;
        while (valueString.charAt(nameEndIdx) != ' ') {
            nameEndIdx++;
        }
        name = valueString.substring(idx, nameEndIdx);
        return this.resourceTypeTree.getPropertyDefinitionByPrefix(prefix, name);
    }

    private String parseRawValue(String valueString) {
        // Assume correctly formatted string
        int idx = "prefix:".length();
        while (valueString.charAt(idx) != ' ') idx++;
        idx++;
        idx += "name:".length();
        while (valueString.charAt(idx) != ' ') idx++;
        idx++;
        return valueString.substring(idx);
    }
    

    private boolean setAclEntry(Resource resource, String name, Attributes attributes, boolean decode) throws Exception {
        String actionName = name.substring("X-vrtx-acl-".length());
        RepositoryAction action = Privilege.getActionByName(actionName);

        
        String values = attributes.getValue(name);
        if (decode) {
            values = decodeValue(values);
        }
        String[] list = values.split(",");

        Acl acl = resource.getAcl();
        acl.clear();
        for (String value: list) {
            String principalName = value.substring(2);
            Principal p = null;
            char type = values.charAt(0);
            switch (type) {
            case 'p':
                p = Principal.getPseudoPrincipal(principalName);
                break;
            case 'u':
                p = new Principal(principalName, Principal.Type.USER);
                break;
            case 'g':
                p = new Principal(principalName, Principal.Type.GROUP);
                break;
            }
            if (p != null ) {
                resource.setInheritedAcl(false);
                acl.addEntry(action, p);
            }
        }
        return !resource.isInheritedAcl();
    }
    
    private void writeFile(String token, String uri, ZipInputStream is) throws Exception {
        this.repository.createDocument(token, uri);
        this.repository.storeContent(token, uri, new PartialZipStream(is));
    }
    

    private String decodeValue(String s) {
        s = s.replaceAll("_esc_n_", "\n");
        s = s.replaceAll("_esc_r_", "\r");
        s = s.replaceAll("_esc_u_", "_");
        return s;
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

    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    
}

