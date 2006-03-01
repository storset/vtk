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
package org.vortikal.repositoryimpl;

import java.util.ArrayList;
import java.util.Arrays;

import org.vortikal.repositoryimpl.dao.DataAccessor;
import org.vortikal.security.PrincipalManager;


public class Collection extends Resource implements Cloneable {
    private String[] childURIs = null;

    public Collection(String uri, String owner, String contentModifiedBy,
        String propertiesModifiedBy, ACL acl, boolean inheritedACL, LockImpl lock,
        DataAccessor dao, PrincipalManager principalManager, String[] childURIs) {
        super(uri, owner, contentModifiedBy, propertiesModifiedBy, acl,
            inheritedACL, lock, dao, principalManager);
        this.childURIs = childURIs;
        setContentType("application/x-vortex-collection");
    }

    public Object clone() throws CloneNotSupportedException {
        ACL acl = (this.acl == null) ? null : (ACL) this.acl.clone();
        LockImpl lock = (this.lock == null) ? null : (LockImpl) this.lock.clone();
        String[] clonedChildURIs = new String[childURIs.length];

        System.arraycopy(childURIs, 0, clonedChildURIs, 0, childURIs.length);

        return new Collection(uri, owner, contentModifiedBy,
            propertiesModifiedBy, acl, inheritedACL, lock, dao, principalManager,
            clonedChildURIs);
    }


    public void setChildURIs(String[] childURIs) {
        this.childURIs = childURIs;
    }

    public String[] getChildURIs() {
        return this.childURIs;
    }
    

    /**
     * Adds a URI to the child URI list.
     *
     * @param childURI a <code>String</code> value
     */
    public void addChildURI(String childURI) {
        synchronized (childURIs) {
            ArrayList l = new ArrayList();

            l.addAll(Arrays.asList(childURIs));
            l.add(childURI);

            String[] newChildren = (String[]) l.toArray(new String[] {  });

            this.childURIs = newChildren;
        }
    }


}
