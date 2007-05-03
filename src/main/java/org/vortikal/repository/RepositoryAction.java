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

public final class RepositoryAction {
    
    public final static RepositoryAction READ_PROCESSED = new RepositoryAction("read-processed");

    public final static RepositoryAction READ = new RepositoryAction("read");

    public final static RepositoryAction CREATE = new RepositoryAction("create");

    public final static RepositoryAction WRITE = new RepositoryAction("write");

    public final static RepositoryAction WRITE_ACL = new RepositoryAction("write-acl");

    public final static RepositoryAction UNLOCK = new RepositoryAction("unlock");

    public final static RepositoryAction DELETE = new RepositoryAction("delete");

    public final static RepositoryAction COPY = new RepositoryAction("copy");

    public final static RepositoryAction MOVE = new RepositoryAction("move");

    public final static RepositoryAction ALL = new RepositoryAction("all");

    public final static RepositoryAction UNEDITABLE_ACTION =
        new RepositoryAction("property-edit-uneditable-action");

    public final static RepositoryAction REPOSITORY_ADMIN_ROLE_ACTION =
        new RepositoryAction("property-edit-admin-role");

    public final static RepositoryAction REPOSITORY_ROOT_ROLE_ACTION =
        new RepositoryAction("property-edit-root-role");

    
    
    public final static RepositoryAction[] REPOSITORY_ACTIONS = 
        new RepositoryAction[] {
        RepositoryAction.READ_PROCESSED,
        RepositoryAction.READ,
        RepositoryAction.CREATE,
        RepositoryAction.WRITE,
        RepositoryAction.WRITE_ACL,
        RepositoryAction.UNLOCK, 
        RepositoryAction.DELETE,
        RepositoryAction.COPY,
        RepositoryAction.MOVE,
        RepositoryAction.ALL,
        RepositoryAction.REPOSITORY_ADMIN_ROLE_ACTION,
        RepositoryAction.REPOSITORY_ROOT_ROLE_ACTION,
        RepositoryAction.UNEDITABLE_ACTION};
    
    /**
     * The list of defined repository actions
     */
    public final static Set<RepositoryAction> REPOSITORY_ACTION_SET = 
        new HashSet<RepositoryAction>(Arrays.asList(REPOSITORY_ACTIONS));
    

    
    private String name;
    
    private RepositoryAction(String name) {
        this.name = name;
    }
    

    public String toString() {
        return this.name;
    }
}
