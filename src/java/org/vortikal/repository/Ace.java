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


/**
 * This class encapsulates meta information about an access control
 * element (ACE).
 *
 */
public class Ace implements java.io.Serializable, Cloneable {
    
    private static final long serialVersionUID = 3257003246219572275L;

    private ACLPrincipal principal = null;
    private Privilege[] privileges = new Privilege[] {  };
    private boolean granted = true;
    private boolean protectedACE = false;
    private boolean principalInverted = false;
    private String inheritedFrom = null;

    /**
     * Gets the ACL principal for this ACE.
     *
     * @return an <code>ACLPrincipal</code>
     */
    public ACLPrincipal getPrincipal() {
        return this.principal;
    }

    /**
     * Sets the ACL principal for this ACE.
     *
     */
    public void setPrincipal(ACLPrincipal principal) {
        this.principal = principal;
    }

    /**
     * Gets this ACE's set of privileges.
     *
     * @return a <code>Privilege[]</code>
     */
    public Privilege[] getPrivileges() {
        return this.privileges;
    }

    /**
     * Sets this ACE's set of privileges.
     *
     */
    public void setPrivileges(Privilege[] privileges) {
        this.privileges = privileges;
    }

    /**
     * Indicates whether the privileges of this ACE are granted or
     * denied.
     *
     * @return a <code>boolean</code>
     */
    public boolean isGranted() {
        return this.granted;
    }

    /**
     * Sets whether the privileges of this ACE are granted or
     * denied.
     *
     */
    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public boolean isProtectedACE() {
        return this.protectedACE;
    }

    public void setProtectedACE(boolean protectedACE) {
        this.protectedACE = protectedACE;
    }

    public boolean isPrincipalInverted() {
        return this.principalInverted;
    }

    public void setPrincipalInverted(boolean principalInverted) {
        this.principalInverted = principalInverted;
    }

    public String getInheritedFrom() {
        return this.inheritedFrom;
    }

    public void setInheritedFrom(String inheritedFrom) {
        this.inheritedFrom = inheritedFrom;
    }

    public Object clone() throws CloneNotSupportedException {
        Ace clone = (Ace) super.clone();
        ACLPrincipal principalClone = (ACLPrincipal) this.principal.clone();

        clone.principal = principalClone;

        Privilege[] privs = new Privilege[privileges.length];

        for (int i = 0; i < privileges.length; i++) {
            privs[i] = (Privilege) privileges[i].clone();
        }

        clone.privileges = privs;

        return clone;
    }

    public int hashCode() {
        // FIXME: implement this properly: 
        int hashCode = 0;

        hashCode += principal.hashCode();

        for (int i = 0; i < privileges.length; i++) {
            hashCode += privileges[i].hashCode();
        }

        if (granted) {
            hashCode++;
        }

        if (protectedACE) {
            hashCode++;
        }

        if (principalInverted) {
            hashCode++;
        }

        if (inheritedFrom != null) {
            hashCode += inheritedFrom.hashCode();
        }

        return hashCode;
    }

    public boolean equals(Object o) {
        if ((!(o instanceof Ace)) || (o == null)) {
            return false;
        }

        Ace other = (Ace) o;

        if (((this.principal == null) && (other.principal != null)) ||
                ((this.principal != null) && (other.principal == null))) {
            return false;
        }

        if (!this.principal.equals(other.principal)) {
            return false;
        }

        for (int i = 0; i < this.privileges.length; i++) {
            boolean found = false;

            for (int j = 0; j < other.privileges.length; j++) {
                if (this.privileges[i].equals(other.privileges[j])) {
                    found = true;
                }
            }

            if (!found) {
                return false;
            }
        }

        if (this.granted != other.granted) {
            return false;
        }

        if (this.protectedACE != other.protectedACE) {
            return false;
        }

        if (this.principalInverted != other.principalInverted) {
            return false;
        }

        if (((this.inheritedFrom == null) && (other.inheritedFrom != null)) ||
                ((this.inheritedFrom != null) && (other.inheritedFrom == null))) {
            return false;
        }

        if (!(this.inheritedFrom == null && other.inheritedFrom == null)
            && !(this.inheritedFrom.equals(other.inheritedFrom))) {
            return false;
        }

        return true;
    }
}
