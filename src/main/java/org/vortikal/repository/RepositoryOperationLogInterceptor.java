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

import java.io.IOException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RepositoryOperation operation = RepositoryOperation.byName(invocation.getMethod().getName());
        if (operation == null) {
            // Unknown repository operation (wrt. logging)
            return invocation.proceed();
        }

        // Should generalize more, but just do it like this in the first iteration.
        // TODO: shred unnecessary operation logging to simplify things (remove quirks) ..
        
        // Handle quirks first
        if (RepositoryOperation.SET_READ_ONLY == operation) {
            return dispatchAndLogSetReadOnly(invocation, operation);
        }

        if (RepositoryOperation.EXISTS == operation) {
            return dispatchAndLogExists(invocation, operation);
        }

        Object[] args = invocation.getArguments();
        String token = (String)args[0];
        String params = null;
        Revision rev = null;
        
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null && args[i] instanceof Revision) {
                rev = (Revision) args[i];
            }
        }
        
        // Reduce avg. overhead by putting most common ops early in list ..
        if (operation == RepositoryOperation.RETRIEVE                ||
                operation == RepositoryOperation.LIST_CHILDREN       ||
                operation == RepositoryOperation.GET_INPUTSTREAM     ||
                operation == RepositoryOperation.LOCK                ||
                operation == RepositoryOperation.UNLOCK              ||
                operation == RepositoryOperation.CREATE_DOCUMENT     ||
                operation == RepositoryOperation.CREATE_COLLECTION   ||
                operation == RepositoryOperation.CREATE              ||
                operation == RepositoryOperation.DELETE              ||
                operation == RepositoryOperation.STORE_CONTENT       ||
                operation == RepositoryOperation.STORE_ACL           ||
                operation == RepositoryOperation.DELETE_ACL          ||
                operation == RepositoryOperation.GET_REVISIONS       ||
                operation == RepositoryOperation.DELETE_REVISION) {            

            Path uri = (Path)args[1];
            if (rev != null) {
                params = "(" + uri + ", r" + rev.getID() + ")";
            } else {
                params = "(" + uri + ")";
            }
        } else if (RepositoryOperation.CREATE_REVISION == operation) {
            Path uri = (Path) args[1];
            Revision.Type type = (Revision.Type) args[2];
            params = "(" + uri + ", " + type + ")";
            
        } else if (RepositoryOperation.COPY == operation ||
                   RepositoryOperation.MOVE == operation) {
            
            Path srcUri = (Path)args[1];
            Path dstUri = (Path)args[2];
            params = "(" + srcUri + ", " + dstUri + ")";
            
        } else if (RepositoryOperation.STORE == operation               ||
                   RepositoryOperation.DELETE_COMMENT == operation      ||
                   RepositoryOperation.DELETE_ALL_COMMENTS == operation ||
                   RepositoryOperation.UPDATE_COMMENT == operation      ||
                   RepositoryOperation.GET_COMMENTS == operation) {

            Resource resource = (Resource)args[1];
            Path uri = resource.getURI();
            params = "(" + uri + ")";

        } else if (RepositoryOperation.ADD_COMMENT == operation) {
        	Object o = args[1];
        	Path uri = null;
        	if (o instanceof Resource) {
        		uri = ((Resource) o).getURI();
        	} else {
        		uri = ((Comment) o).getURI();
        	}
        	params = "(" + uri + ")";
        } else if (RepositoryOperation.SEARCH == operation) {
            params = "(...)";
        }
        
        // Dispatch to intercepted method and log result
        return dispatchAndLog(invocation, params, operation, token, getPrincipal(token));
    }
    


    private Object dispatchAndLog(MethodInvocation mi, 
                                  String params, 
                                  RepositoryOperation op, 
                                  String token, 
                                  Principal principal) throws Throwable {
        
        Object retVal = null;
        try {
            retVal = mi.proceed();
            OperationLog.success(op, params, token, principal);
            return retVal;
            
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
    }
    
    private Object dispatchAndLogSetReadOnly(MethodInvocation mi, RepositoryOperation op) throws Throwable {
        Object[] args = mi.getArguments();
        
        String token = (String)args[0];
        Boolean readOnly = (Boolean)args[1];
        
        try {
            mi.proceed();
            OperationLog.success(op, "(" + readOnly + ")", token, getPrincipal(token));
            return null;
        } catch (AuthorizationException ae) {
            OperationLog.failure(op, "(" + readOnly + ")", "not authorized", token, 
                    getPrincipal(token));
            throw ae;
        }
    }
    
    private Object dispatchAndLogExists(MethodInvocation mi, RepositoryOperation op) throws Throwable {
        Object[] args = mi.getArguments();
        
        String token = (String) args[0];
        Path uri = (Path) args[1];
        
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
