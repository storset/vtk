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
package org.vortikal.repositoryimpl;

import java.io.IOException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.FailedDependencyException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.RepositoryOperations;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.ResourceOverwriteException;
import org.vortikal.repositoryimpl.OperationLog;
import org.vortikal.repositoryimpl.RepositoryImpl;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.token.TokenManager;

/**
 * Spring AOP method interceptor class for excplicit logging for 
 * repository operations.
 * 
 * Functionality is currently equivalent to the old logging from
 * RepositoryImpl (including most "quirks"). 
 * This could potentially be simplified/reduced.
 * 
 * @author oyviste
 *
 */
public class RepositoryOperationLogInterceptor implements MethodInterceptor {

    Log logger = LogFactory.getLog(RepositoryOperationLogInterceptor.class);
    
    private TokenManager tokenManager;
    
    public Object invoke(MethodInvocation invocation) throws Throwable {
        
        Object repo = invocation.getThis();
        if (!(repo instanceof RepositoryImpl)) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Not an org.vortikal.repositoryimpl.RepositoryImpl instance: "
                             + repo + ", proceeding with invocation.");
            }

            return invocation.proceed();
        }

        String operation = invocation.getMethod().getName();
        Object[] args = invocation.getArguments();

        // Should generalize more, but just do it like this in the first iteration.
        // TODO: shred unnecessary operation logging to simplify things (remove quirks) ..
        
        // Handle quirks first
        if (RepositoryOperations.SET_READ_ONLY.equals(operation)) {
            return dispatchAndLogSetReadOnly(invocation);
        }

        if (RepositoryOperations.EXISTS.equals(operation)) {
            return dispatchAndLogExists(invocation);
        }
        
        String token = null;
        String params = null;
        
        // Reduce avg. overhead by putting most common ops early in list ..
        if (RepositoryOperations.RETRIEVE.equals(operation)          ||
            RepositoryOperations.LIST_CHILDREN.equals(operation)     ||
            RepositoryOperations.GET_ACL.equals(operation)           ||
            RepositoryOperations.GET_INPUTSTREAM.equals(operation)   ||
            RepositoryOperations.LOCK.equals(operation)              ||
            RepositoryOperations.UNLOCK.equals(operation)            ||
            RepositoryOperations.CREATE_DOCUMENT.equals(operation)   ||
            RepositoryOperations.CREATE_COLLECTION.equals(operation) ||
            RepositoryOperations.CREATE.equals(operation)            ||
            RepositoryOperations.DELETE.equals(operation)            ||
            RepositoryOperations.STORE_CONTENT.equals(operation)     ||
            RepositoryOperations.STORE_ACL.equals(operation)) {
            
            token = (String)args[0];
            String uri = (String)args[1];
            
            params = "(" + uri + ")";
        } else if (RepositoryOperations.COPY.equals(operation) ||
                   RepositoryOperations.MOVE.equals(operation)) {
            
            token = (String)args[0];
            String srcUri = (String)args[1];
            String dstUri = (String)args[2];
            params = "(" + srcUri + ", " + dstUri + ")";
            
        } else if (RepositoryOperations.STORE.equals(operation)) {

            token = (String)args[0];
            Resource resource = (Resource)args[1];
            String uri = resource.getURI();
            params = "(" + uri + ")";
            
        } else {
            // Unknown repository operation (wrt. logging)
            return invocation.proceed();
        }
        
        // Dispatch to intercepted method and log result
        return dispatchAndLog(invocation, params, operation, token, getPrincipal(token));
    }
    
    private Object dispatchAndLog(MethodInvocation mi, 
                                  String params, 
                                  String op, 
                                  String token, 
                                  Principal principal) throws Throwable {
        
        Object retVal = null;
        try {
            retVal = mi.proceed();
            
            OperationLog.success(op, params, token, principal);
            
        } catch (ReadOnlyException roe) {
            OperationLog.failure(op, params, "read-only", token, principal);
            throw roe;
        } catch (ResourceNotFoundException rnf) {
            OperationLog.failure(op, params, "resource not found: '" + rnf.getURI() + "'", token,
                    principal);
            throw rnf;
        } catch (IllegalOperationException ioe) {
            OperationLog.failure(op, params, ioe.getMessage(), token,
                    principal);
            throw ioe;
        } catch (AuthenticationException authenticationException) {
            OperationLog.failure(op, params, "not authenticated", token,
                    principal);
            throw authenticationException;
        } catch (AuthorizationException authorizationException) {
            OperationLog.failure(op, params, "not authorized", token,
                    principal);
            throw authorizationException;
        } catch (ResourceLockedException le) {
            OperationLog.failure(op, params, "resource locked", token,
                    principal);
            throw le;
        } catch (ResourceOverwriteException roe) {
            OperationLog.failure(op, params, "cannot overwrite destination resource", token,
                    principal);
            throw roe;
        } catch (FailedDependencyException fde) {
            // XXX: Log this exception ?
            OperationLog.failure(op, params, "failed dependency", token, principal);
            throw fde;
        } catch (IOException io) {
            OperationLog.failure(op, params, io.getMessage(), token, principal);
            throw io;
        }
        
        return retVal;
    }
    
    private Object dispatchAndLogSetReadOnly(MethodInvocation mi) throws Throwable {
        String op = RepositoryOperations.SET_READ_ONLY;
        Object[] args = mi.getArguments();
        
        String token = (String)args[0];
        Boolean readOnly = (Boolean)args[1];
        
        try {
            mi.proceed();
        } catch (AuthorizationException ae) {
            OperationLog.failure(op, "(" + readOnly + ")", "not authorized", token, 
                    getPrincipal(token));
            throw ae;
        }
        
        OperationLog.success(op, "(" + readOnly + ")", token, getPrincipal(token));
        
        return null;
    }
    
    private Object dispatchAndLogExists(MethodInvocation mi) throws Throwable {
        String op = RepositoryOperations.EXISTS;
        Object[] args = mi.getArguments();
        
        String token = (String) args[0];
        String uri = (String) args[1];
        
        Boolean retVal = (Boolean)mi.proceed();
        
        if (retVal.booleanValue()) {
            OperationLog.info(op, "(" + uri + "): true",
                    token, getPrincipal(token));
        } else {
            OperationLog.info(op, "(" + uri + "): false",
                    token, getPrincipal(token));
        }
        
        return retVal;
    }
    
    private Principal getPrincipal(String token) {
        return this.tokenManager.getPrincipal(token);
    }

    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

}
