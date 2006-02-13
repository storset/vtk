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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.security.Principal;


public class OperationLog {

    public final static String RETRIEVE = "retrieve";
    public final static String CREATE = "create";
    public final static String CREATE_COLLECTION = "createCollection";
    public final static String COPY = "copy";
    public final static String MOVE = "move";
    public final static String DELETE = "delete";
    public final static String EXISTS = "exists";
    public final static String LOCK = "lock";
    public final static String UNLOCK = "unlock";
    public final static String LIST_CHILDREN = "listChildren";
    public final static String STORE = "store";
    public final static String GET_INPUTSTREAM = "getInputStream";
    public final static String STORE_CONTENT = "storeContent";
    public final static String GET_ACL = "getACL";
    public final static String STORE_ACL = "storeACL";
    
    private static final Set WRITE_OPERATIONS;
    
    static {
        WRITE_OPERATIONS = new HashSet();
        WRITE_OPERATIONS.add(CREATE);
        WRITE_OPERATIONS.add(CREATE_COLLECTION);
        WRITE_OPERATIONS.add(COPY);
        WRITE_OPERATIONS.add(MOVE);
        WRITE_OPERATIONS.add(DELETE);
        WRITE_OPERATIONS.add(LOCK);
        WRITE_OPERATIONS.add(UNLOCK);
        WRITE_OPERATIONS.add(STORE);
        WRITE_OPERATIONS.add(STORE_CONTENT);
        WRITE_OPERATIONS.add(STORE_ACL);
    }
    


    private static Log logger = LogFactory.getLog(OperationLog.class);

    private static Log writeLogger = LogFactory.getLog(
        OperationLog.class.getName() + ".write");

    public static void info(String operation, String parameters,
                            String token, Principal principal) {
        StringBuffer msg = new StringBuffer();
        msg.append(operation).append(parameters);
        msg.append(", principal: ").append(principal);
        msg.append(", token: ").append(token);
        if (logger.isDebugEnabled()) {
            logger.debug(msg.toString());
        }
        if (writeLogger.isDebugEnabled() && WRITE_OPERATIONS.contains(operation)) {
            writeLogger.debug(msg.toString());
        }
    }

    public static void success(String operation, String parameters,
                               String token, Principal principal) {
        StringBuffer msg = new StringBuffer();
        msg.append(operation).append(parameters);
        msg.append(" succeeded, principal: ").append(principal);
        msg.append(", token: ").append(token);
        if (logger.isDebugEnabled()) {
            logger.debug(msg.toString());
        }
        if (writeLogger.isDebugEnabled() && WRITE_OPERATIONS.contains(operation)) {
            writeLogger.debug(msg.toString());
        }
    }

    public static void failure(String operation, String parameters,
                               String message, String token,
        Principal principal) {
        StringBuffer msg = new StringBuffer();

        msg.append(operation).append(parameters);
        msg.append(" failed: ").append(message);
        msg.append(", principal: ").append(principal);
        msg.append(", token: ").append(token);
        if (logger.isDebugEnabled()) {
            logger.debug(msg.toString());
        }
        if (writeLogger.isDebugEnabled() && WRITE_OPERATIONS.contains(operation)) {
            writeLogger.debug(msg.toString());
        }
    }

    
}
