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
package org.vortikal.repository;

import org.vortikal.repositoryimpl.ACLPrincipal;


/**
 * This class represents the various restrictions put upon instances
 * of access control lists (ACLs).
 */
public class AclRestrictions implements java.io.Serializable/*, Cloneable */{

    private static final long serialVersionUID = 3256720680303997238L;
    
    // ACL restrictions:
    private boolean grantOnly = false;
    private boolean noInvert = false;
    private boolean denyBeforeGrant = false;
    private boolean principalOnlyOneAce = false;
    private ACLPrincipal[] requiredPrincipals = null;

    public boolean getGrantOnly() {
        return this.grantOnly;
    }

    public void setGrantOnly(boolean grantOnly) {
        this.grantOnly = grantOnly;
    }

    public boolean getNoInvert() {
        return this.noInvert;
    }

    public void setNoInvert(boolean noInvert) {
        this.noInvert = noInvert;
    }

    public boolean getDenyBeforeGrant() {
        return this.denyBeforeGrant;
    }

    public void setDenyBeforeGrant(boolean denyBeforeGrant) {
        this.denyBeforeGrant = denyBeforeGrant;
    }

    public boolean getPrincipalOnlyOneAce() {
        return this.principalOnlyOneAce;
    }

    public void setPrincipalOnlyOneAce(boolean principalOnlyOneAce) {
        this.principalOnlyOneAce = principalOnlyOneAce;
    }

    public ACLPrincipal[] getRequiredPrincipals() {
        return this.requiredPrincipals;
    }

    public void setRequiredPrincipals(ACLPrincipal[] requiredPrincipals) {
        this.requiredPrincipals = requiredPrincipals;
    }

//    public Object clone() throws CloneNotSupportedException {
//        AclRestrictions clone = (AclRestrictions) super.clone();
//        ACLPrincipal[] principals = new ACLPrincipal[requiredPrincipals.length];
//
//        for (int i = 0; i < requiredPrincipals.length; i++) {
//            principals[i] = (ACLPrincipal) requiredPrincipals[i].clone();
//        }
//
//        clone.requiredPrincipals = principals;
//
//        return clone;
//    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.getClass().getName()).append(": ");
        sb.append("[grantOnly = ").append(grantOnly);
        sb.append(", noInvert = ").append(noInvert);
        sb.append(", denyBeforeGrant = ").append(denyBeforeGrant);
        sb.append(", principalOnlyOneAce = ").append(principalOnlyOneAce);
        sb.append(", requiredPrincipals = ").append(requiredPrincipals);
        sb.append("]");

        return sb.toString();
    }
}
