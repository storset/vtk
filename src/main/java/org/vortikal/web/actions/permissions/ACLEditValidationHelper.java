/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.web.actions.permissions;

import org.vortikal.repository.Privilege;
import org.vortikal.repository.Repository;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.Principal.Type;

public class ACLEditValidationHelper {
    
    public static final String GROUP_PREFIX = "group:";
    public static final String USER_PREFIX = "user:";
    
    public static final String VALIDATION_ERROR_GROUP_PREFIX = "group";
    public static final String VALIDATION_ERROR_USER_PREFIX = "user";
    
    public static final String VALIDATION_ERROR_NOT_FOUND = "not.found";
    public static final String VALIDATION_ERROR_ILLEGAL_BLACKLISTED = "illegal.blacklisted";
    public static final String VALIDATION_ERROR_ILLEGAL = "illegal";
    public static final String VALIDATION_ERROR_TOO_MANY_MATCHES = "too.many.matches";
    public static final String VALIDATION_ERROR_NONE = "";
    
    public static String validateGroupOrUserName(Type type, String name, Privilege privilege,
            PrincipalFactory principalFactory, PrincipalManager principalManager, Repository repository) {
        
        try {
            Principal groupOrUser = null;
            boolean exists = false;

            if (Type.GROUP.equals(type)) {
                groupOrUser = principalFactory.getPrincipal(name, type);
                exists = principalManager.validateGroup(groupOrUser);
            } else {
                if (PrincipalFactory.NAME_ALL.equals(name)) {
                    // pseudo:all cant be validated for the moment
                    return VALIDATION_ERROR_NONE;
                }
                groupOrUser = principalFactory.getPrincipal(name, type);
                exists = principalManager.validatePrincipal(groupOrUser);
            }

            if (groupOrUser != null && !exists) {
                return VALIDATION_ERROR_NOT_FOUND;
            }

            if (!repository.isValidAclEntry(privilege, groupOrUser)) {
                return VALIDATION_ERROR_ILLEGAL_BLACKLISTED;
            }
        } catch (InvalidPrincipalException ipe) {
            return VALIDATION_ERROR_ILLEGAL;
        }
        
        return VALIDATION_ERROR_NONE;
    }
   
    
}
