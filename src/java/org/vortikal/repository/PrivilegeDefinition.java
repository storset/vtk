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
 * This class represents the privileges supported on a resource.
 */
public class PrivilegeDefinition implements java.io.Serializable, Cloneable {
    public static final String STANDARD_NAMESPACE = "dav";
    public static final String READ = "read";
    public static final String WRITE = "write";
    public static final String WRITE_PROPERTIES = "write-properties";
    public static final String WRITE_CONTENT = "write-content";
    public static final String UNLOCK = "unlock";
    public static final String READ_ACL = "read-acl";
    public static final String READ_CURRENT_USER_PRIVILEGE_SET = "read-current-user-privilege-set";
    public static final String WRITE_ACL = "write-acl";
    public static final String ALL = "all";
    private String name = null;
    private String namespace = null;
    private boolean abstractACE = false;
    private String description = null;
    private PrivilegeDefinition[] members = new PrivilegeDefinition[] {  };

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Gets the value of abstract
     *
     * @return the value of abstract
     */
    public boolean isAbstractACE() {
        return this.abstractACE;
    }

    /**
     * Sets the value of abstract
     *
     * @param abstractACE Value to assign to this.abstract
     */
    public void setAbstractACE(boolean abstractACE) {
        this.abstractACE = abstractACE;
    }

    /**
     * Gets the value of description
     *
     * @return the value of description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets the value of description
     *
     * @param description Value to assign to this.description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the aggregated privileges of this privilege.
     *
     */
    public PrivilegeDefinition[] getMembers() {
        return this.members;
    }

    /**
     * Sets the aggregated privileges of this privilege.
     *
     * @param members a list of (direct) subprivileges that are
     * aggregated by this privilege
     */
    public void setMembers(PrivilegeDefinition[] members) {
        this.members = members;
    }

    public Object clone() throws CloneNotSupportedException {
        PrivilegeDefinition clone = (PrivilegeDefinition) super.clone();
        PrivilegeDefinition[] cloneMembers = new PrivilegeDefinition[this.members.length];

        for (int i = 0; i < members.length; i++) {
            cloneMembers[i] = (PrivilegeDefinition) members[i].clone();
        }

        clone.members = cloneMembers;

        return clone;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.getClass().getName()).append(": ");
        sb.append("[namespace = ").append(name);
        sb.append(", name = ").append(name);
        sb.append(", abstract = ").append(abstractACE);
        sb.append(", description = ").append(description);
        sb.append(", members = ").append(members);
        sb.append("]");

        return sb.toString();
    }
}
