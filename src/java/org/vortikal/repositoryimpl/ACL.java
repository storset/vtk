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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ACL implements Cloneable {

    /**
     * map: [action --> List(ACLPrincipal)]
     */
    private Map actionLists = new HashMap();

    public Map getActionMap() {
        return actionLists;
    }

    public List getPrincipalList(String action) {
        return (List) actionLists.get(action);
    }

    public boolean equals(Object o) {
        if (!(o instanceof ACL)) {
            return false;
        }

        ACL acl = (ACL) o;

        Set actions = actionLists.keySet();

        if (actions.size() != acl.actionLists.keySet().size()) {
            return false;
        }

        for (Iterator i = actions.iterator(); i.hasNext();) {
            String action = (String) i.next();

            if (!acl.actionLists.containsKey(action)) {
                return false;
            }

            List myPrincipals = (List) actionLists.get(action);
            List otherPrincipals = (List) acl.actionLists.get(action);

            if (myPrincipals.size() != otherPrincipals.size()) {
                return false;
            }

            for (Iterator j = myPrincipals.iterator(); j.hasNext();) {
                ACLPrincipal p = (ACLPrincipal) j.next();

                if (!otherPrincipals.contains(p)) {
                    return false;
                }
            }
        }

        return true;
    }

    public int hashCode() {
        int hashCode = super.hashCode();

        Set actions = actionLists.keySet();

        for (Iterator i = actions.iterator(); i.hasNext();) {
            String action = (String) i.next();
            List principals = (List) actionLists.get(action);

            for (Iterator j = principals.iterator(); j.hasNext();) {
                ACLPrincipal p = (ACLPrincipal) j.next();

                hashCode += p.hashCode();
            }
        }

        return hashCode;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("[ACL:");

        for (Iterator i = actionLists.keySet().iterator(); i.hasNext();) {
            String action = (String) i.next();
            List principalList = getPrincipalList(action);

            sb.append(" [");
            sb.append(action);
            sb.append(":");
            for (Iterator j = principalList.iterator(); j.hasNext();) {
                ACLPrincipal p = (ACLPrincipal) j.next();

                sb.append(" ");
                sb.append(p.getUrl());

                if (p.isGroup()) {
                    sb.append("(g)");
                }

                if (j.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }

}
