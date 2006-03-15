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

import org.vortikal.repository.RepositoryException;


/**
 * Exception class indicating that an operation on an ACE failed for
 * some reason.
 *
 */
public class AclException extends RepositoryException {

    private static final long serialVersionUID = 3258134635094358064L;

    /**
     * Indicates that the operation in question is in violation of an
     * ACL restriction. This status code may be used in cases where
     * there are no other status codes that describe the violation in a
     * more specific manner.
     *
     */
    public static final int NO_ACE_CONFLICT = 0;

    /**
     * Indicates that the submitted ACE is in conflict with one or
     * more ACEs on the resource.
     *
     */
    public static final int NO_PROTECTED_ACE_CONFLICT = 1;

    /**
     * Indicates that the ACE submitted is in conflict with inherited
     * ACEs on the resource.
     *
     */
    public static final int NO_INHERITED_ACE_CONFLICT = 2;

    /**
     * The ACE submitted will lead to the exceeding of the number of
     * allowed ACEs on the resourcce.
     *
     */
    public static final int LIMITED_NUMBER_OF_ACES = 3;

    /**
     * The (deny) ACE must precede any grant ACEs.
     *
     */
    public static final int DENY_BEFORE_GRANT = 4;

    /**
     * The ACL submitted violates the condition (if applicable) that
     * any principal may only have one ACE.
     *
     */
    public static final int PRINCIPAL_ONLY_ONE_ACE = 5;

    /**
     * The resource allows only grant ACEs.
     *
     */
    public static final int GRANT_ONLY = 6;

    /**
     * The resource does not allow inverted principals.
     *
     */
    public static final int NO_INVERT = 7;

    /**
     * The ACL submitted attempted to grant or deny an abstract
     * privilege.
     *
     */
    public static final int NO_ABSTRACT = 9;

    /**
     * The privilege submitted in the ACE is not supported.
     *
     */
    public static final int NOT_SUPPORTED_PRIVILEGE = 10;

    /**
     * A certain required principal is missing in the ACL.
     *
     */
    public static final int MISSING_REQUIRED_PRINCIPAL = 11;

    /**
     * One or more principals submitted with the ACL are unknown to
     * the server.
     *
     */
    public static final int RECOGNIZED_PRINCIPAL = 12;

    /**
     * One or more principals submitted with the ACL are not allowed
     * by the server.
     *
     */
    public static final int ALLOWED_PRINCIPAL = 13;
    private int status = NO_ACE_CONFLICT;

    public AclException(String message) {
        super(message);
    }

    public AclException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }
}
