/* Copyright (c) 2004, 2005, 2006, 2007, University of Oslo, Norway
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
package org.vortikal.repository.store;

import java.util.Date;
import java.util.Set;

import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.security.Principal;


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
    public boolean validate() throws DataAccessException;


    /**
     * Loads a single resource
     */
    public ResourceImpl load(Path uri) throws DataAccessException;


    /**
     * Loads the children of a given resource
     */
    public ResourceImpl[] loadChildren(ResourceImpl parent) throws DataAccessException;


    /**
     * Stores a single resource
     */
    public void store(ResourceImpl r) throws DataAccessException;


    /**
     * Stores the ACL of a resource
     */
    public void storeACL(ResourceImpl r) throws DataAccessException;


    /**
     * Deletes a single resource (and any children)
     */
    public void delete(ResourceImpl resource) throws DataAccessException;



    /**
     * Deletes all expired locks (should be called periodically)
     */
    public void deleteExpiredLocks(Date expireDate) throws DataAccessException;


    /**
     * Atomically copies a resource to a new destination.
     * @param resource the resource to copy from
     * @param dest the resource to copy into (becomes the parent of
     * the copied resource after the copy operation)
     * @param newResource the newly created resource: this resource is
     * passed as an argument, as its properties may have changed as a
     * result of the name change operation.
     * @param copyACLs whether to copy ACLs from the existing
     * resource, or to inherit from the new parent resource
     * @param fixedProperties a set of properties to set on the new
     * resource(s) instead of copying from the existing
     */
    public void copy(ResourceImpl resource, ResourceImpl dest,
                     PropertySet newResource, boolean copyACLs,
                     PropertySet fixedProperties) throws DataAccessException;


    /**
     * Atomically moves a resource to a new destination.
     * @param resource the resource to move
     * @param newResource the newly created resource: this resource is
     * passed as an argument, as its properties may have changed as a
     * result of the name change operation.
     */
    public void move(ResourceImpl resource, ResourceImpl newResource)
        throws DataAccessException;


    /**
     * Finds any locks on a resource, or on resources in the URI
     * hierarchy defined by that resource.
     */
    public Path[] discoverLocks(Path uri) throws DataAccessException;

    /**
     * Finds any ACLs on a resource, or on resources in the URI
     * hierarchy defined by that resource.
     */
    public Path[] discoverACLs(Path uri) throws DataAccessException;
    
    /**
     * Discover all distinct groups currently present in the database.
     * 
     * @return A <code>Set</code> of <code>Principal</code> objects representing
     *         the groups.
     */
    public Set<Principal> discoverGroups() throws DataAccessException;

}
