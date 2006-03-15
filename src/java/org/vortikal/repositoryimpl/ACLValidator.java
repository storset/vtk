package org.vortikal.repositoryimpl;

import org.vortikal.repository.Acl;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;

public class ACLValidator {

    private PrincipalManager principalManager;
    
    /**
     * Checks the validity of an ACL.
     *
     * @param aceList an <code>Ace[]</code> value
     * @exception AclException if an error occurs
     * @exception IllegalOperationException if an error occurs
     * @exception IOException if an error occurs
     */
    public void validateACL(Acl acl)
        throws AclException, IllegalOperationException {
        /*
         * Enforce ((dav:owner (dav:read dav:write dav:write-acl))
         */
        if (!acl.hasPrivilege("dav:owner", PrivilegeDefinition.WRITE)) {
            throw new IllegalOperationException(
                "Owner must be granted write privilege in ACL.");
        }

        if (!acl.hasPrivilege("dav:owner", PrivilegeDefinition.READ)) {
            throw new IllegalOperationException(
                "Owner must be granted read privilege in ACL.");
        }

        if (!acl.hasPrivilege("dav:owner", PrivilegeDefinition.WRITE_ACL)) {
            throw new IllegalOperationException(
                "Owner must be granted write-acl privilege in ACL.");
        }

        boolean inheritance = acl.isInherited();

        /*
         * Walk trough the ACL, for every ACE, enforce that:
         * 1) Every principal is valid
         * 2) Every privilege has a supported namespace and name
         */

//        for (int i = 0; i < acl.length; i++) {
//            Ace ace = acl[i];
//
//            org.vortikal.repositoryimpl.ACLPrincipal principal = ace.getPrincipal();
//
//            if (principal.getType() == org.vortikal.repositoryimpl.ACLPrincipal.TYPE_URL) {
//                boolean validPrincipal = false;
//
//                if (principal.isUser()) {
//                    Principal p = null;
//                    try {
//                        p = principalManager.getPrincipal(principal.getURL());
//                    } catch (InvalidPrincipalException e) {
//                        throw new AclException("Invalid principal '" 
//                                + principal.getURL() + "' in ACL");
//                    }
//                    validPrincipal = principalManager.validatePrincipal(p);
//                } else {
//                    validPrincipal = principalManager.validateGroup(principal.getURL());
//                }
//
//                if (!validPrincipal) {
//                    throw new AclException(AclException.RECOGNIZED_PRINCIPAL,
//                        "Unknown principal: " + principal.getURL());
//                }
//            } else {
//                if ((principal.getType() != org.vortikal.repositoryimpl.ACLPrincipal.TYPE_ALL) &&
//                        (principal.getType() != org.vortikal.repositoryimpl.ACLPrincipal.TYPE_OWNER) &&
//                        (principal.getType() != org.vortikal.repositoryimpl.ACLPrincipal.TYPE_AUTHENTICATED)) {
//                    throw new AclException(AclException.RECOGNIZED_PRINCIPAL,
//                        "Allowed principal types are " +
//                        "either TYPE_ALL, TYPE_OWNER " + "OR  TYPE_URL.");
//                }
//            }
//
//            Privilege[] privileges = ace.getPrivileges();
//
//            for (int j = 0; j < privileges.length; j++) {
//                Privilege privilege = privileges[j];
//
//                if (privilege.getNamespace().equals(Namespace.STANDARD_NAMESPACE)) {
//                    if (!(privilege.getName().equals(PrivilegeDefinition.WRITE) ||
//                            privilege.getName().equals(PrivilegeDefinition.READ) ||
//                            privilege.getName().equals(PrivilegeDefinition.WRITE_ACL))) {
//                        throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
//                            "Unsupported privilege name: " +
//                            privilege.getName());
//                    }
//                } else if (privilege.getNamespace().equals(Namespace.CUSTOM_NAMESPACE)) {
//                    if (!(privilege.getName().equals(PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED))) {
//                        throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
//                            "Unsupported privilege name: " +
//                            privilege.getName());
//                    }
//                } else {
//                    throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
//                        "Unsupported privilege namespace: " +
//                        privilege.getNamespace());
//                }
//            }
//        }
    }

    
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

}
