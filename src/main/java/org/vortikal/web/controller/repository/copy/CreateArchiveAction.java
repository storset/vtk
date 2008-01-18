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
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;

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
        InputStream jar = createArchive(token, resource.getParent(), resource, dest);
        this.repository.storeContent(token, dest.getURI(), jar);
    }

    
    private InputStream createArchive(String token, String base, Resource r, Resource dest) throws Exception {
        
        File outFile = File.createTempFile("vrtx-archive", "jar");
        FileOutputStream out = new FileOutputStream(outFile);
        BufferedOutputStream bo = new BufferedOutputStream(out);

        Manifest manifest = createManifest(token, base, r);
        JarOutputStream jo = new JarOutputStream(bo, manifest);

        addEntry(token, base, r, jo);

        jo.close();
        bo.close();
        return new FileInputStream(outFile);
    }

    private Manifest createManifest(String token, String base, Resource r) throws Exception {

        File tmp = File.createTempFile("tmp-manifest", "vrtx");
        PrintWriter out = new PrintWriter(new FileOutputStream(tmp));
        
        out.println("Manifest-Version: 1.0");
        out.println("Created-By: vrtx");
        out.println("X-vrtx-archive-version: 1.0");
        
        addManifestEntry(token, base, r, out);
        out.flush();
        out.close();

        return new Manifest(new FileInputStream(tmp));
    }
    

    private void addManifestEntry(String token, String base, Resource r, PrintWriter out) throws Exception {
        String path = r.isCollection() ? r.getURI() + "/" : r.getURI();
        path = path.substring(base.length());
        
        out.println("");
        out.println("Name: " + escapeNewlines(path));
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
            entry += escapeNewlines(property.getFormattedValue());
            out.println(entry);
        }

        if (r.isCollection()) {
            Resource[] children = this.repository.listChildren(token, r.getURI(), false);
            for (Resource child: children) {
                addManifestEntry(token, base, child, out);
            }
        }
    }

    private String escapeNewlines(String s) {
        return s.replaceAll("\\n", "\\\\n");
    }
    
    private void addEntry(String token, String base, Resource r, JarOutputStream jo) throws Exception {

        String path = r.isCollection() ? r.getURI() + "/" : r.getURI();
        path = path.substring(base.length());

        System.out.println("add: " + path);

        JarEntry je = new JarEntry(path);
        jo.putNextEntry(je);

        if (r.isCollection()) {
            Resource[] children = this.repository.listChildren(token, r.getURI(), false);
            for (Resource child: children) {
                addEntry(token, base, child, jo);
            }
        } else {
            InputStream is = this.repository.getInputStream(token, r.getURI(), false);
            BufferedInputStream bi = new BufferedInputStream(is);

            byte[] buf = new byte[1024];
            int anz;
         
            while ((anz = bi.read(buf)) != -1) {
                jo.write(buf, 0, anz);
            }
            bi.close();
        }
    }
    
    
    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    
}
