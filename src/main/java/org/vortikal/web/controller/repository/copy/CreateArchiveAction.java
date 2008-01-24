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
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.URLUtil;

public class CreateArchiveAction implements CopyAction {

    private Repository repository;
    
    public void process(String uri, String copyUri) throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();

        Resource resource = this.repository.retrieve(token, uri, false);
        if (!resource.isCollection()) {
            throw new RuntimeException("Cannot archive a single Resource, must be a collection");
        }
        Resource dest = this.repository.createDocument(token, copyUri);
        InputStream jar = createArchive(token, resource, dest);
        this.repository.storeContent(token, dest.getURI(), jar);
    }

    
    private InputStream createArchive(String token, Resource r, Resource dest) throws Exception {
        
        File outFile = File.createTempFile("vrtx-archive", "jar");
        FileOutputStream out = new FileOutputStream(outFile);
        BufferedOutputStream bo = new BufferedOutputStream(out);

        int rootLevel = URLUtil.splitUri(r.getURI()).length;
        
        Manifest manifest = createManifest(token, rootLevel, r);
        JarOutputStream jo = new JarOutputStream(bo, manifest);

        addEntry(token, rootLevel, r, jo);

        jo.close();
        bo.close();
        return new FileInputStream(outFile);
    }

    private Manifest createManifest(String token, int rootLevel, Resource r) throws Exception {

        File tmp = File.createTempFile("tmp-manifest", "vrtx");
        PrintWriter out = new PrintWriter(new FileOutputStream(tmp));
        
        out.println("Manifest-Version: 1.0");
        out.println("Created-By: vrtx");
        out.println("X-vrtx-archive-version: 1.0");
        out.println("X-vrtx-archive-encoded: true"); 
        
        addManifestEntry(token, rootLevel, r, out);
        out.flush();
        out.close();

        return new Manifest(new FileInputStream(tmp));
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
        String path = getJarPath(r, fromLevel);
        
        out.println("");
        out.println("Name: " + encodeValue(path));

        List<Property> properties = r.getProperties();
        for (Property property : properties) {
            if (property.getDefinition() == null) {
                continue;
            }
            String prefix = property.getDefinition().getNamespace().getPrefix();
            String entry = "X-vrtx-prop-";
            if (prefix != null) {
                entry += prefix + "_";
            }
            entry += property.getDefinition().getName() + ": ";
            if (property.getDefinition().getType() == PropertyType.Type.DATE) {
                entry += encodeValue(property.getFormattedValue("iso-8601", null));
            } else {
                entry += encodeValue(property.getFormattedValue());
            }
            out.println(entry);
        }
        
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
                if (!empty) out.println(entry);
            }
        }
        
        if (r.isCollection()) {
            Resource[] children = this.repository.listChildren(token, r.getURI(), false);
            for (Resource child: children) {
                addManifestEntry(token, fromLevel, child, out);
            }
        }
    }


    
    private String encodeValue(String s) throws Exception {
        // &esc; --> &esc;&esc;
        // \r\n --> &esc;rn
        // \r --> &esc;r
        // \n --> &esc;n

        s = s.replaceAll("&esc;", "&esc;&esc;");
        s = s.replaceAll("\r\n", "&esc;rn");
        s = s.replaceAll("\r", "&esc;r");
        s = s.replaceAll("\n", "&esc;n");

        return s;
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

    
}
