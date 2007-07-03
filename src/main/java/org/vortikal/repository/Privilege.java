/* Copyright (c) 2006, University of Oslo, Norway
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the privileges supported in an Acl.
 */
public class Privilege {
    
    public static final RepositoryAction READ_PROCESSED = RepositoryAction.READ_PROCESSED;
    public static final RepositoryAction READ = RepositoryAction.READ;
    public static final RepositoryAction BIND = RepositoryAction.CREATE;
    public static final RepositoryAction WRITE = RepositoryAction.WRITE;
    public static final RepositoryAction ALL = RepositoryAction.ALL;
    

    public static final Set<RepositoryAction> PRIVILEGES =
        new HashSet<RepositoryAction>(Arrays.asList(new RepositoryAction[] {READ_PROCESSED,READ,BIND,WRITE,ALL}));
    

    public static final RepositoryAction getActionByName(String actionName) {
        if ("read".equals(actionName)) {
            return READ;
        }
        if ("write".equals(actionName)) {
            return WRITE;
        }
        if ("all".equals(actionName)) {
            return ALL;
        }
        if ("read-processed".equals(actionName)) {
            return READ_PROCESSED;
        }
        if ("bind".equals(actionName)) {
            return BIND;
        }
        throw new IllegalArgumentException("Invalid ACL action: '" + actionName + "'");
    }
    

    public static final String getActionName(RepositoryAction action) {

        if (READ.equals(action)) {
            return "read";
        }
        if (WRITE.equals(action)) {
            return "write";
        }
        if (ALL.equals(action)) {
            return "all";
        }
        if (READ_PROCESSED.equals(action)) {
            return "read-processed";
        }
        if (BIND.equals(action)) {
            return "bind";
        }
        throw new IllegalArgumentException("Invalid ACL privilege: '" + action + "'");
    }
}
