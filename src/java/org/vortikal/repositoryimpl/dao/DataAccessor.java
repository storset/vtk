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
package org.vortikal.repositoryimpl.dao;

import org.vortikal.repositoryimpl.Collection;
import org.vortikal.repositoryimpl.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Simple data access abstraction API.
 */
public interface DataAccessor {
    /*
     * "Sanity check" method, called after
     * initialization. Implementations should return false if the
     * data accessor is unable to operate properly (i.e. tables missing,
     * etc.)
     */
    public boolean validate() throws IOException;

    /* Called when the data accessor is brought down */
    public void destroy() throws IOException;

    /* Loads a single resource */
    public Resource load(String uri) throws IOException;

    /* Loads a list of resources. */
    public Resource[] load(String[] uris) throws IOException;

    /* Loads the children of a given resource */
    public Resource[] loadChildren(Collection parent) throws IOException;

    /* Stores a single resource */
    public void store(Resource r) throws IOException;

    /* Deletes a single resource (and any children) */
    public void delete(Resource resource) throws IOException;

    /* Opens an input stream for reading from a resource */
    public InputStream getInputStream(Resource resource)
        throws IOException;

    /* Opens an output stream for writing from a resource */
    public OutputStream getOutputStream(Resource resource)
        throws IOException;

    /* Gets the content length (in bytes) of a resource */
    public long getContentLength(Resource resource) throws IOException;

    /* Lists all descendants of a collection resource, sorted by URI */
    public String[] listSubTree(Collection parent) throws IOException;

    /* Lists all expired locks (optimization feature) */
    public String[] listLockExpired() throws IOException;

    /* Deletes the locks for a number of resources */
    public void deleteLocks(Resource[] resources) throws IOException;

    /* Used externally to report a resource modification */
    public void addChangeLogEntry(String loggerID, String recordType,
        String uri, String operation) throws IOException;

    /**
     * Proposed new methods: copy(), move().  These are currently
     * implemented on the domain level, meaning that every resource
     * must first be loaded from the data access level before moving,
     * consuming a *lot* of time when dealing with large/deep
     * collections. */

    //public void move(Resource resource, String destURI);
    //public void copy(Resource resource, String destURI);

    /**
     * A faster way of discovering locks on resources deep down in the
     * hierarchy, without the need to actually load all the resources
     * in the subtree. */
    public String[] discoverLocks(Resource directory) throws IOException;

    /* Stores a list of resources. */

    //public void store(Resource[] resources) throws IOException;
}
