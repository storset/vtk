/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.web.service.provider;

public class ListResourceItem {
    private String uri;
    private String name;
    private String title;
    private boolean collection;
    private boolean hasChildren;
    private boolean readRestricted = false;
    private boolean inheritedAcl = true;
    private String read;
    private String write;
    private String admin;

    public ListResourceItem(String uri, String name, String title, boolean collection, boolean hasChildren) {
        this.uri = uri;
        this.name = name;
        this.title = title;
        this.collection = collection;
        this.hasChildren = hasChildren;
    }
    
    public ListResourceItem(String uri, String name, String title, boolean collection, boolean hasChildren,
            boolean readRestricted, boolean inheritedAcl, String read, String write, String admin) {
        this.uri = uri;
        this.name = name;
        this.title = title;
        this.collection = collection;
        this.hasChildren = hasChildren;
        this.readRestricted = readRestricted;
        this.inheritedAcl = inheritedAcl;
        this.read = read;
        this.write = write;
        this.admin = admin;
    }
    
    public String getUri() {
        return this.uri;
    }

    public String getName() {
        return this.name;
    }

    public String getTitle() {
        return this.title;
    }

    public boolean isCollection() {
        return this.collection;
    }

    public boolean hasChildren() {
        return this.hasChildren;
    }

    public boolean isReadRestricted() {
        return this.readRestricted;
    }

    public boolean isInheritedAcl() {
        return this.inheritedAcl;
    }

    public String getRead() {
        return this.read;
    }

    public String getWrite() {
        return this.write;
    }

    public String getAdmin() {
        return this.admin;
    }

}