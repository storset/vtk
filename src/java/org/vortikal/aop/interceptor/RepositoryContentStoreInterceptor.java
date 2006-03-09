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

package org.vortikal.aop.interceptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.repositoryimpl.RepositoryImpl;
import org.vortikal.repositoryimpl.Resource;
import org.vortikal.repositoryimpl.dao.DataAccessor;
import org.vortikal.util.io.StreamUtil;


/**
 * Intercepts calls to {@link RepositoryImpl#storeContent}, allowing
 * special processing of resources when stored based on their content.
 *
 * <p>This interceptor applies a list of {@link ContentStoreHandler
 * content handlers} to the content stream and resource object in
 * sequence. The content handlers may modify either of these, and the
 * results are stored in the repository.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <p><code>contentHandlers</code> - an array of {@link
 *   ContentStoreHandler} objects performing content processing.
 * </ul>
 *
 */
public class RepositoryContentStoreInterceptor implements MethodInterceptor {

    private Log logger = LogFactory.getLog(this.getClass());
    
    private ContentStoreHandler[] contentHandlers = new ContentStoreHandler[0];
    

    public void setContentHandlers(ContentStoreHandler[] contentHandlers) {
        this.contentHandlers = contentHandlers;
    }
    

    public Object invoke(MethodInvocation invocation) throws Throwable {

        Object result = null;
        Method m = invocation.getMethod();
            
        Object o = invocation.getThis();
        if (!(o instanceof RepositoryImpl)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not a RepositoryImpl instance: "
                             + o + ", proceeding with invocation");
            }

            return invocation.proceed();
        }
        RepositoryImpl repository = (RepositoryImpl) o;

        if (!m.getName().equals("storeContent")) {
            return invocation.proceed();
        }

        Object[] args = invocation.getArguments();
        DataAccessor dao = repository.getDataAccessor();

        String uri = (String) args[1];
        InputStream inStream = (InputStream) args[2];

        Resource resource = dao.load(uri);
            
        List applicableHandlers = new ArrayList();
        for (int i = 0; i < this.contentHandlers.length; i++) {
            if (this.contentHandlers[i].isApplicableHandler(resource)) {
                applicableHandlers.add(this.contentHandlers[i]);
            }
        }

        if (applicableHandlers.size() == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "No applicable content store handlers found for resource "
                    + resource);
            }
            return invocation.proceed();
        }

        byte[] buffer = StreamUtil.readInputStream(inStream);
        inStream.close();
            
        ByteArrayInputStream bufferStream = new ByteArrayInputStream(buffer);
        args[2] = bufferStream;
        result = invocation.proceed();

        bufferStream.reset();
        resource = dao.load(uri);

        if (logger.isDebugEnabled()) {
            logger.debug("Will run content store handlers on resource " + resource
                         + ": " + applicableHandlers);
        }


        for (Iterator i = applicableHandlers.iterator(); i.hasNext();) {
            ContentStoreHandler handler = (ContentStoreHandler) i.next();
            bufferStream = handler.processContent(
                bufferStream, resource);
        }

        // XXX: what's happening here?
        dao.storeContent(resource, bufferStream);
        dao.store(resource);
        return result;
    }

}
