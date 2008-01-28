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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Acl;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.URLUtil;

public class CreateArchiveAction implements CopyAction {

    private Repository repository;
    private File tempDir = new File(System.getProperty("java.io.tmpdir"));
    
    public void process(String uri, String copyUri) throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();

        Resource resource = this.repository.retrieve(token, uri, false);
        if (!resource.isCollection()) {
            throw new RuntimeException("Cannot archive a single Resource, must be a collection");
        }
        File jar = null;
        try {
            jar = createArchive(token, resource);
            Resource dest = this.repository.createDocument(token, copyUri);        
            InputStream in = new FileInputStream(jar);
            this.repository.storeContent(token, dest.getURI(), in);
        } finally {
            if (jar != null) jar.delete();
        }
    }
    
    private File createArchive(String token, Resource r) throws Exception {
        
        File outFile = File.createTempFile("vrtx-archive", "jar", this.tempDir);
        FileOutputStream out = new FileOutputStream(outFile);
        BufferedOutputStream bo = new BufferedOutputStream(out);

        int rootLevel = URLUtil.splitUri(r.getURI()).length;
        
        File tmp = null;
        try {
            tmp = File.createTempFile("tmp-manifest", "vrtx", this.tempDir);
            PrintWriter manifestOut = new PrintWriter(new FileOutputStream(tmp));
            writeManifest(token, rootLevel, r, manifestOut);
            Manifest manifest = new Manifest(new FileInputStream(tmp));
            JarOutputStream jo = new JarOutputStream(bo, manifest);
            addEntry(token, rootLevel, r, jo);
            jo.close();
            bo.close();
            return outFile;
        } finally {
            if (tmp != null) tmp.delete();
        }
    }

    private void writeManifest(String token, int rootLevel, Resource r, PrintWriter out) throws Exception {

        out.println("Manifest-Version: 1.0");
        out.println("Created-By: vrtx");
        out.println("X-vrtx-archive-version: 1.0");
        out.println("X-vrtx-archive-encoded: true"); 
        
        addManifestEntry(token, rootLevel, r, out);
        out.flush();
        out.close();

    }
    
    private String getJarPath(Resource resource, int fromLevel) {
        String path = resource.getURI();
        String[] splitPath = URLUtil.splitUri(path);
        StringBuilder result = new StringBuilder("/");
        for (int i = fromLevel; i < splitPath.length; i++) {
            if (i != 0) result.append(splitPath[i]);
            if (i < splitPath.length - 1 && !"/".equals(splitPath[i])) result.append("/");
        }
        if (resource.isCollection() && !"/".equals(result.toString())) result.append("/"); 
        return result.toString();
    }
    
    
    private void addManifestEntry(String token, int fromLevel, Resource r, PrintWriter out) throws Exception {
        StringBuilder path = new StringBuilder(getJarPath(r, fromLevel));
        encode(path);
        ensure72Bytes(path);
        
        out.println("");
        out.println("Name: " + path);

        addProperties(r, out);        
        addAcl(r, out);
        
        if (r.isCollection()) {
            Resource[] children = this.repository.listChildren(token, r.getURI(), false);
            for (Resource child: children) {
                addManifestEntry(token, fromLevel, child, out);
            }
        }
    }

    private void addProperties(Resource r, PrintWriter out) throws Exception {
        List<Property> properties = r.getProperties();
        int propCounter = 0;
        for (Property property : properties) {
            PropertyTypeDefinition propDef = property.getDefinition();
            Namespace namespace = propDef.getNamespace();
            
            StringBuilder entry = new StringBuilder("X-vrtx-prop-");
            entry.append(propCounter).append(": ");
            entry.append("prefix:");
            if (namespace.getPrefix() != null) {
                entry.append(namespace.getPrefix());
            }
            entry.append(" ");
            entry.append("name:").append(propDef.getName()).append(" ");
            if (propDef.getType() == PropertyType.Type.DATE || propDef.getType() == PropertyType.Type.TIMESTAMP) {
                entry.append(property.getFormattedValue("yyyy-MM-dd'T'HH:mm:ssZZ", null));
            } else {
                entry.append(property.getFormattedValue());
            }
            
            encode(entry);
            ensure72Bytes(entry);
            out.println(entry);
            propCounter++;
        }
    }
    
    private void addAcl(Resource r, PrintWriter out) throws Exception {
        if (!r.isInheritedAcl()) {
            Acl acl = r.getAcl();
            for (RepositoryAction action : acl.getActions()) {
                StringBuilder entry = new StringBuilder("X-vrtx-acl-");
                entry.append(action).append(": ");

                boolean empty = true;

                Principal[] users = acl.listPrivilegedUsers(action);
                for (int i = 0; i < users.length; i++) {
                    Principal user = users[i];
                    if (i > 0) entry.append(",");
                    entry.append("u:").append(user.getQualifiedName());
                    empty = false;
                }
                
                Principal[] groups = acl.listPrivilegedGroups(action);
                for (int i = 0; i < groups.length; i++) {
                    Principal group = groups[i];
                    if (!empty || i > 0) entry.append(",");
                    entry.append("g:").append(group.getQualifiedName());
                    empty = false;
                }
                
                Principal[] pseudos = acl.listPrivilegedPseudoPrincipals(action);
                for (int i = 0; i < pseudos.length; i++) {
                    Principal pseudo = pseudos[i];
                    if (!empty || i > 0) entry.append(",");
                    entry.append("p:").append(pseudo.getQualifiedName());
                    empty = false;
                }
                
                if (!empty) {
                    encode(entry);
                    ensure72Bytes(entry);
                    out.println(entry.toString());
                }
            }
        }
    }

    
    private void ensure72Bytes(StringBuilder s) throws Exception {
        int i = 0;
        int count = 0;
        while (i < s.length()) {
            int delta = s.substring(i, i+1).getBytes("utf-8").length;
            if (count + delta > 72) {
                s.insert(i, "\n ");
                i += 2;
                count = 0;
            } else {
                i++;
                count += delta;
            }
        }
    }
    
    
    /**
     * Flatten "\r" and "\n" (not allowed in manifest entries)
     */
    private void encode(StringBuilder s) {
        // '_' --> '_esc_u_'
        // '\r' --> '_esc_r_'
        // '\n' --> '_esc_n_'
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            switch (c) {
            case '_':
                s.delete(i, i + 1);
                s.insert(i, "_esc_u_");
                i += 7;
                break;
            case '\r':
                s.delete(i, i + 1);
                s.insert(i, "_esc_r_");
                i += 7;
                break;
            case '\n':
                s.delete(i, i + 1);
                s.insert(i, "_esc_n_");
                i += 7;
                break;
            default:
                i++;
                break;
            }
        }
    }
    
    
    private void addEntry(String token, int fromLevel, Resource r, JarOutputStream jarOut) throws Exception {

        String path = getJarPath(r, fromLevel);

        JarEntry je = new JarEntry(path);
        jarOut.putNextEntry(je);
        if (r.isCollection()) {
            Resource[] children = this.repository.listChildren(token, r.getURI(), false);
            for (Resource child: children) {
                addEntry(token, fromLevel, child, jarOut);
            }
        } else {
            InputStream is = this.repository.getInputStream(token, r.getURI(), false);
            BufferedInputStream bi = new BufferedInputStream(is);

            byte[] buf = new byte[1024];
            int n;
            while ((n = bi.read(buf)) != -1) {
                jarOut.write(buf, 0, n);
            }
            bi.close();
        }
    }
    
    
    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setTempDir(String tempDirPath) {
        File tmp = new File(tempDirPath);
        if (!tmp.exists()) {
            throw new IllegalArgumentException("Unable to set tempDir: file " 
                    + tmp + " does not exist");
        }
        if (!tmp.isDirectory()) {
            throw new IllegalArgumentException("Unable to set tempDir: file " 
                    + tmp + " is not a directory");
        }
        this.tempDir = tmp;
    }

    
}
