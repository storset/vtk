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

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Set;

import org.vortikal.repository.PropertySet;
import org.vortikal.repositoryimpl.ResourceImpl;


/**
 * Simple data access abstraction API.
 */
public interface DataAccessor {


    /**
     * "Sanity check" method, called after
     * initialization. Implementations should return false if the
     * data accessor is unable to operate properly (i.e. tables missing,
     * etc.)
     */
    public boolean validate() throws IOException;


    /**
     * Loads a single resource
     */
    public ResourceImpl load(String uri) throws IOException;


    /**
     * Loads the children of a given resource
     */
    public ResourceImpl[] loadChildren(ResourceImpl parent) throws IOException;


    /**
     * Stores a single resource
     */
    public void store(ResourceImpl r) throws IOException;


    /**
     * Stores the ACL of a resource
     */
    public void storeACL(ResourceImpl r) throws IOException;


    /**
     * Deletes a single resource (and any children)
     */
    public void delete(ResourceImpl resource) throws IOException;


    /**
     * Opens an input stream for reading from a resource
     */
    public InputStream getInputStream(String uri)
        throws IOException;


    /**
     * Writes content for a resource
     */
    public void storeContent(String uri, InputStream stream)
        throws IOException;


//     public ContentHandle writeTemporaryContent(String uri, InputStream stream) throws IOException;
    
//     public class ContentHandle 
//     {
//     }


    /**
     * Lists all descendants of a collection resource, sorted by URI
     */
    public String[] listSubTree(ResourceImpl parent) throws IOException;


    /**
     * Deletes all expired locks (should be called periodically)
     */
    public void deleteExpiredLocks() throws IOException;


    /**
     * Used externally to report a resource modification
     */
    public void addChangeLogEntry(int loggerID, int loggerType,
                                  String uri, String operation, int resourceId,
                                  boolean collection, Date timestamp,
                                  boolean recurse) throws IOException;

    /**
     * Atomically copies a resource to a new destination.
     * @param resource the resource to copy from
     * @param dest the resource to copy into (becomes the parent of
     * the copied resource after the copy operation)
     * @param destURI the destination path
     * @param copyACLs whether to copy ACLs from the existing
     * resource, or to inherit from the new parent resource
     * @param fixedProperties a set of properties to set on the new
     * resource(s) instead of copying from the existing
     * @param newResource the newly created resource
     */
    public void copy(ResourceImpl resource, ResourceImpl dest,
                     String destURI, boolean copyACLs,
                     PropertySet fixedProperties,
                     PropertySet newResource) throws IOException;


    //public void move(Resource resource, String destURI);


    /**
     * Finds any locks on a resource, or on resources in the URI
     * hierarchy defined by that resource.
     */
    public String[] discoverLocks(String uri) throws IOException;

    /**
     * Finds any ACLs on a resource, or on resources in the URI
     * hierarchy defined by that resource.
     */
    public String[] discoverACLs(String uri) throws IOException;
    
    /**
     * Discover all distinct groups currently present in the database.
     * 
     * @return A <code>Set</code> of <code>Principal</code> objects representing
     *         the groups.
     */
    public Set discoverGroups() throws IOException;

}
