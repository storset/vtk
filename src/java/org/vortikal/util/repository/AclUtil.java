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
package org.vortikal.util.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.vortikal.repository.ACLPrincipal;
import org.vortikal.repository.Ace;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repository.Resource;

import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;



/**
 * Utility class containing commonly used methods for manipulating
 * Vortex Acces Control Lists.
 *
 * @author Endre Rognerud
 * @version $Id: AclUtil.java,v 1.4 2004/03/16 20:27:11 storset Exp $
 */

public class AclUtil {


    public static String aclAsString(Ace[] acl, String msg) {
        String debugMsg = "\n" + msg + ":\n";
        String part1;
        String part2;

        for (int i = 0; i < acl.length; i++) {
            Ace ace = acl[i];

            part1 = "   " + ace.getPrincipal().getName() + ": ";
            part2 = "";

            Privilege[] privileges = ace.getPrivileges();
            Privilege privilege;
            for (int j = 0; j < privileges.length; j++) {
                privilege = privileges[j];
                part2 += privilege.getName() + " ";
            }
            debugMsg += part1 + part2 + "\n";
        }
        return debugMsg;
    }



    public static Ace createOwnerACE() {
        Ace ace = new Ace();
        ACLPrincipal principal = new ACLPrincipal();
        principal.setType(ACLPrincipal.TYPE_OWNER);

        ace.setPrincipal(principal);
        ace.setGranted(true);
        Privilege[] privileges = new Privilege[2];
        Privilege priv = new Privilege();
        priv.setName(PrivilegeDefinition.READ);
        privileges[0] = priv;
        priv = new Privilege();
        priv.setName(PrivilegeDefinition.WRITE);
        privileges[1] = priv;
        ace.setPrivileges(privileges);
        return ace;
    }
    


    /** 
     * Returns the default ACL to use with a resource. Doesn't include the
     * "read-processed" privilege since it's a UIO-thing.
     */
    public static Ace[] createDefaultACL() {
        Ace[] acl = new Ace[2];
        Ace ace;
        ACLPrincipal principal;
        Privilege[] privileges;
        Privilege privilege;

        // OWNER
        principal = new ACLPrincipal();
        principal.setType(ACLPrincipal.TYPE_OWNER);
        privileges = new Privilege[3];
        privilege = new Privilege();
        privilege.setName(PrivilegeDefinition.WRITE_ACL);
        privilege.setNamespace(PrivilegeDefinition.STANDARD_NAMESPACE);
        privileges[0] = privilege;
        privilege = new Privilege();
        privilege.setName(PrivilegeDefinition.READ);
        privilege.setNamespace(PrivilegeDefinition.STANDARD_NAMESPACE);
        privileges[1] = privilege;
        privilege = new Privilege();
        privilege.setName(PrivilegeDefinition.WRITE);
        privilege.setNamespace(PrivilegeDefinition.STANDARD_NAMESPACE);
        privileges[2] = privilege;
        ace = new Ace();
        ace.setPrincipal(principal);
        ace.setPrivileges(privileges);
        acl[0] = ace;

        // AUTHENTICATED
        principal = new ACLPrincipal();
        principal.setType(ACLPrincipal.TYPE_AUTHENTICATED);
        privilege = new Privilege();
        privilege.setName(PrivilegeDefinition.READ);
        privilege.setNamespace(PrivilegeDefinition.STANDARD_NAMESPACE);
        ace = new Ace();
        ace.setPrincipal(principal);
        ace.setPrivileges(new Privilege[] { privilege });
        acl[1] = ace;

        //  // ALL
        //  principal = new ACLPrincipal();
        //  principal.setType(ACLPrincipal.TYPE_ALL);
        //  privilege = new Privilege();
        //  privilege.setName("read-processed");
        //  privilege.setNamespace("uio");
        //  ace = new Ace();
        //  ace.setPrincipal(principal);
        //  ace.setPrivileges(new Privilege[] { privilege });
        //  acl[2] = ace;

        return acl;
    }



    public static Ace[] addPrivilegeToACL(Resource r, Ace[] acl,
                                         String username,
                                         String privilegeName,
                                         boolean isUser) {
        
        ArrayList newACL = new ArrayList();
        
        int userIndex = -1;

        for (int i = 0; i < acl.length; i++) {
            Ace ace = acl[i];
            newACL.add(ace);
            if (ace.getPrincipal().getType() == ACLPrincipal.TYPE_URL) {
                if (ace.getPrincipal().getURL().equals(username) &&
                    (ace.getPrincipal().isUser() == isUser)) {
                    userIndex = i;
                }

            } else if (ace.getPrincipal().getType() == 
                       ACLPrincipal.TYPE_AUTHENTICATED) {
                if (username.equals("dav:authenticated")) {
                    userIndex = i;
                }
            } else if (ace.getPrincipal().getType() == 
                       ACLPrincipal.TYPE_OWNER) {
                if (username.equals("dav:owner")) {
                    userIndex = i;
                }
                
            } else if (ace.getPrincipal().getType() == 
                       ACLPrincipal.TYPE_ALL) {
                if (username.equals("dav:all")) {
                    userIndex = i;
                }
                
            }
        }

        String namespace = 
            ("read-processed".equals(privilegeName)) ? "uio" : 
            PrivilegeDefinition.STANDARD_NAMESPACE;

        if (userIndex == -1) {
            Ace ace = new Ace();
            ACLPrincipal principal = new ACLPrincipal();
            if (username.equals("dav:authenticated")) {
                principal.setType(ACLPrincipal.TYPE_AUTHENTICATED);

            } else if (username.equals("dav:owner")) {
                principal.setType(ACLPrincipal.TYPE_OWNER);

            } else if (username.equals("dav:all")) {
                principal.setType(ACLPrincipal.TYPE_ALL);

            } else {
                principal.setType(ACLPrincipal.TYPE_URL);
                principal.setURL(username);
                principal.setIsUser(isUser);
            }
            
            ace.setPrincipal(principal);
            ace.setGranted(true);
            Privilege[] privileges = new Privilege[1];
            Privilege priv = new Privilege();
            priv.setName(privilegeName);
            priv.setNamespace(namespace);
            privileges[0] = priv;
            ace.setPrivileges(privileges);
            newACL.add(ace);
        }
        

        if (userIndex != -1) {
            Ace ace = (Ace) newACL.get(userIndex);
            Privilege[] privileges = ace.getPrivileges();

            boolean privilegeFound = false;
            for (int i = 0; i < privileges.length; i++) {
                if (privileges[i].getName().equals(privilegeName)
                    && privileges[i].getNamespace().equals(namespace)) {
                    privilegeFound = true;
                    break;
                }
            }
            if (!privilegeFound) {
                // Add new privilege for user
                ArrayList newPrivileges = new ArrayList(
                    Arrays.asList(privileges));
                
                Privilege priv = new Privilege();
                priv.setName(privilegeName);
                priv.setNamespace(namespace);
                newPrivileges.add(priv);
                ace.setPrivileges((Privilege[])
                                  newPrivileges.toArray(new Privilege[]{}));
            }
        }

        return (Ace[]) newACL.toArray(new Ace[]{});

    }
    




    public static Ace[] withdrawPrivilegeFromACL(
        Ace[] acl, String username, String privilegeName) {
        ArrayList newACL = new ArrayList();
        
        int userIndex = -1;

        for (int i = 0; i < acl.length; i++) {
            Ace ace = acl[i];
            newACL.add(ace);

            if (ace.getPrincipal().getType() == ACLPrincipal.TYPE_URL) {
                if (ace.getPrincipal().getURL().equals(username)) {
                    userIndex = i;
                }

            } else if (ace.getPrincipal().getType() == ACLPrincipal.TYPE_AUTHENTICATED) {
                if (username.equals("dav:authenticated")) {
                    userIndex = i;
                }

            } else if (ace.getPrincipal().getType() == ACLPrincipal.TYPE_ALL) {
                
                if (username.equals("dav:all")) {
                    userIndex = i;
                }
                
            }
        }

        if (userIndex == -1) {
            return acl;
        }
        
        if (userIndex != -1) {
            Ace ace = (Ace) newACL.get(userIndex);
            Privilege[] privileges = ace.getPrivileges();

            if (privileges.length > 0) {

                ArrayList newPrivileges = new ArrayList();
                for (int i = 0; i < privileges.length; i++) {

                    if (!privileges[i].getName().equals(privilegeName)) {
                        newPrivileges.add(privileges[i]);
                    }
                }

                if (newPrivileges.size() == 0) {
                    newACL.remove(userIndex);
                }
                
                ace.setPrivileges((Privilege[])
                                  newPrivileges.toArray(new Privilege[]{}));
            } else {
                newACL.remove(userIndex);
            }
        }

        return (Ace[]) newACL.toArray(new Ace[]{});
    }
    


    public static Principal[] listPrivilegedUsers(
        Ace[] acl, String privilegeName, PrincipalManager principalManager) {

        ArrayList userList = new ArrayList();

        for (int i = 0; i < acl.length; i++) {
            Ace ace = acl[i];

            String username;
            if (ace.getPrincipal().getType() ==
                ACLPrincipal.TYPE_AUTHENTICATED) {
                // Don't include user = "dav:authenticated":
                continue;

            } else if (ace.getPrincipal().getType() ==
                       ACLPrincipal.TYPE_OWNER) {
                username = "dav:owner";

            } else {
                username = ace.getPrincipal().getURL();
            }
                
            Privilege[] privileges = ace.getPrivileges();

            for (int j = 0; j < privileges.length; j++) {
                Privilege privilege = privileges[j];

                if (privilege.getName().equals(privilegeName) &&
                    ace.getPrincipal().isUser()) {
                    userList.add(principalManager.getPrincipal(username));
                }
            }
        }
        return (Principal[]) userList.toArray(new Principal[0]);
    }
    




    public static String[] listPrivilegedGroups(
        Ace[] acl, String privilegeName) {

        ArrayList userList = new ArrayList();

        for (int i = 0; i < acl.length; i++) {
            Ace ace = acl[i];

            String username;
            if (ace.getPrincipal().getType() ==
                ACLPrincipal.TYPE_AUTHENTICATED) {
                // Don't include user = "dav:authenticated":
                continue;

            } else if (ace.getPrincipal().getType() ==
                       ACLPrincipal.TYPE_OWNER) {
                username = "dav:owner";

            } else {
                username = ace.getPrincipal().getURL();
            }
                
            Privilege[] privileges = ace.getPrivileges();

            for (int j = 0; j < privileges.length; j++) {
                Privilege privilege = privileges[j];

                if (privilege.getName().equals(privilegeName) &&
                    !ace.getPrincipal().isUser()) {
                    
                    userList.add(username);
                }
            }
        }
        return (String[]) userList.toArray(new String[]{});
    }
    


    
    /**
     * Lists principals (users and groups) having a given privilege.
     *
     * @param acl an <code>Ace[]</code> value
     * @param privilegeName a <code>String</code> value
     * @return a <code>List</code>
     */
    public static List listPrivilegedPrincipals(Ace[] acl, String privilegeName) {
        List principalList = new ArrayList();

        for (int i = 0; i < acl.length; i++) {
            principalList.add(acl[i].getPrincipal());
        }
        return principalList;
    }
    


    public static boolean hasPrivilege(Ace[] acl, String userURL,
                                   String privilegeName) {

        for (int i = 0; i < acl.length; i++) {
            Ace ace = acl[i];
                
            Privilege[] privileges = ace.getPrivileges();

            for (int j = 0; j < privileges.length; j++) {
                Privilege privilege = privileges[j];

                if (privilege.getName().equals(privilegeName)) {
                    
                    if (ace.getPrincipal().getType() ==
                        ACLPrincipal.TYPE_AUTHENTICATED) {
                        return true;
                    } 

                    if (ace.getPrincipal().getType() ==
                       ACLPrincipal.TYPE_URL) {

                        if (ace.getPrincipal().getURL().equals(userURL)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    
    
    public static boolean isInherited(Ace[] acl) {
        for (int i = 0; i < acl.length; i++) {
            if (acl[i].getInheritedFrom() == null) {
                return false;
            }
        }
        return true;
    }
    


    public static void modifyInheritance(Ace[] acl, String inheritedFrom) {
        for (int i = 0; i < acl.length; i++) {
            acl[i].setInheritedFrom(inheritedFrom);
        }
    }
    

    public static boolean equal(Ace[] acl1, Ace[] acl2) {
        if (acl1.length != acl2.length)
            return false;

        for (int i = 0; i < acl1.length; i++) {
            boolean found = false;
            for (int j = 0; j < acl2.length; j++) {
                if (acl1[i].equals(acl2[j]))
                    found = true;
            }
            if (!found) return false;
        }
        return true;
    }
    

}
