/* Copyright (c) 2006, 2007, University of Oslo, Norway
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

import java.util.HashMap;
import java.util.Map;


public enum RepositoryOperation {

    SET_READ_ONLY("setReadOnly", true),
    RETRIEVE("retrieve", false),
    CREATE("create", true),
    CREATE_COLLECTION("createCollection", true),
    CREATE_DOCUMENT("createDocument", true),
    COPY("copy", true),
    MOVE("move", true),
    DELETE("delete", true),
    EXISTS("exists", false),
    
    GET_REVISIONS("getRevisions", false),
    CREATE_REVISION("createRevision", true),
    DELETE_REVISION("deleteRevision", true),
    
    LOCK("lock", true),
    UNLOCK("unlock", true),
    LIST_CHILDREN("listChildren", false),
    STORE("store", true),
    GET_INPUTSTREAM("getInputStream", false),
    STORE_CONTENT("storeContent", true),
    STORE_ACL("storeACL", true),
    DELETE_ACL("deleteACL", true),
    IS_VALID_ACL_ENTRY("isValidAclEntry", false),
    GET_COMMENTS("getComment", true),
    ADD_COMMENT("addComment", true),
    DELETE_COMMENT("deleteComment", true),
    DELETE_ALL_COMMENTS("deleteAllComments", true),
    UPDATE_COMMENT("updateComment", true),
    SEARCH("search", false),
    GET_TYPE_INFO("getTypeInfo", false);

    private String name;
    private boolean write;
    private RepositoryOperation(String name, boolean write) {
        this.name = name;
        this.write = write;
    }

    public String getName() {
        return this.name;
    }

    public boolean isWrite() {
        return this.write;
    }
    
    public String toString() {
        return this.name;
    }

    private static final Map<String,RepositoryOperation> ALL_OPERATIONS;
    static {
        ALL_OPERATIONS = new HashMap<String, RepositoryOperation>();
        for (RepositoryOperation op: values()) {
            ALL_OPERATIONS.put(op.getName(), op);
        }
    }
    
    public static RepositoryOperation byName(String name) {
        return ALL_OPERATIONS.get(name);
    }
    
}
