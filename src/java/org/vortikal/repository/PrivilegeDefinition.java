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
    
    private static final long serialVersionUID = 3257289140717237808L;

    public static final String READ = "read";
    public static final String WRITE = "write";
    public static final String WRITE_ACL = "write-acl";
    public static final String ALL = "all";

    public final static String CUSTOM_PRIVILEGE_READ_PROCESSED = "read-processed";

    public final static PrivilegeDefinition standardPrivilegeDefinition;
    public final static AclRestrictions standardRestrictions;

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

    public boolean isAbstractACE() {
        return this.abstractACE;
    }

    public void setAbstractACE(boolean abstractACE) {
        this.abstractACE = abstractACE;
    }

    public String getDescription() {
        return this.description;
    }

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


    static {
        /*
         * Declare the standard ACL supported privilege tree (will be
         * the same for all resources):
         *
         * [dav:all] (abstract)
         *     |
         *     |---[dav:read]
         *     |       |
         *     |       `---[uio:read-processed]
         *     |
         *     |---[dav:write]
         *     |
         *     `---[dav:write-acl]
         *
         */
        standardPrivilegeDefinition = new PrivilegeDefinition();

        PrivilegeDefinition all = new PrivilegeDefinition();

        all.setName(PrivilegeDefinition.ALL);
        all.setNamespace(Namespace.STANDARD_NAMESPACE);
        all.setAbstractACE(true);

        PrivilegeDefinition read = new PrivilegeDefinition();

        read.setName(PrivilegeDefinition.READ);
        read.setNamespace(Namespace.STANDARD_NAMESPACE);
        read.setAbstractACE(false);

        PrivilegeDefinition readProcessed = new PrivilegeDefinition();

        readProcessed.setName(CUSTOM_PRIVILEGE_READ_PROCESSED);
        readProcessed.setNamespace(Namespace.CUSTOM_NAMESPACE);
        readProcessed.setAbstractACE(false);
        read.setMembers(new PrivilegeDefinition[] { readProcessed });

        PrivilegeDefinition write = new PrivilegeDefinition();

        write.setName(PrivilegeDefinition.WRITE);
        write.setNamespace(Namespace.STANDARD_NAMESPACE);
        write.setAbstractACE(false);

        PrivilegeDefinition writeACL = new PrivilegeDefinition();

        writeACL.setName(PrivilegeDefinition.WRITE_ACL);
        writeACL.setNamespace(Namespace.STANDARD_NAMESPACE);
        writeACL.setAbstractACE(false);

        PrivilegeDefinition[] members = new PrivilegeDefinition[3];

        members[0] = read;
        members[1] = write;
        members[2] = writeACL;

        all.setMembers(members);

        /* Set ACL restrictions: */
        standardRestrictions = new AclRestrictions();
        standardRestrictions.setGrantOnly(true);
        standardRestrictions.setNoInvert(true);
        standardRestrictions.setPrincipalOnlyOneAce(true);

        ACLPrincipal owner = new ACLPrincipal();

        owner.setType(ACLPrincipal.TYPE_OWNER);

        ACLPrincipal[] requiredPrincipals = new ACLPrincipal[] { owner };

        standardRestrictions.setRequiredPrincipals(requiredPrincipals);
    }

}
