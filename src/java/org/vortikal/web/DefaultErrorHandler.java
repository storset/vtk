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
package org.vortikal.web;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.View;

import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;


/**
 * Default error handler.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li>errorView - a <code>View</code> for rendering the error model
 *   <li>errorType - a {@link Throwable} deciding the type(s) of
 *   errors that are handled by this class
 *   <li>service - the {@link Service} (if any) for which this error
 *   handler is applicable
 *   <li>modelBuilders - a list of {@link ModelBuilder} objects,
 *   invoked on the error model. Note: some (or all) of these may
 *   crash during invocation, so this list should be a
 *   conservative set of model builders, that should at least not
 *   be operating on the repository.
 *   <li>statusCodeMappings - a {@link Map} that maps between
 *   error class names and HTTP status codes,
 *   e.g. <code>java.lang.Throwable --&gt; 500</code> (internal
 *   server error).
 * </ul>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>exception</code> - a {@link Throwable} - the error
 *   that occurred
 *   <li><code>errorDescription</code> - a localized error description
 * </ul>
 *
 */
public class DefaultErrorHandler implements ErrorHandler, BeanNameAware, InitializingBean {
    
    public static final String DEFAULT_ERROR_CODE = "error.default";
    public static final String DEFAULT_ERROR_DESCRIPTION = "Internal server error";

    public static final String ERROR_MODEL_KEY = "error";
    public static final String ERROR_MODEL_EXCEPTION_KEY = "exception";
    public static final String ERROR_MODEL_ERROR_DESCRIPTION_KEY = "errorDescription";
    
    private String beanName = null;
    private View errorView = null;
    private Class errorType = Throwable.class;
    private Service service = null;
    private ReferenceDataProvider[] providers = new ReferenceDataProvider[0];
    private Map statusCodeMappings = new HashMap();
    

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public void setErrorView(View errorView) {
        this.errorView = errorView;
    }

    public void setErrorType(Class errorType) {
        this.errorType = errorType;
    }

    public Class getErrorType() {
        return errorType;
    }
    
    public void setService(Service service) {
        this.service = service;
    }

    public Service getService() {
        return this.service;
    }
    
    public void setReferenceDataProviders(ReferenceDataProvider[] providers) {
        this.providers = providers;
    }
    
    public void setStatusCodeMappings(Map statusCodeMappings) {
        this.statusCodeMappings = statusCodeMappings;
    }
    
    public void afterPropertiesSet() {
        if (this.errorView == null) {
            throw new BeanInitializationException(
                "Bean property 'errorView' not set.");
        }
        if (this.errorType == null) {
            throw new BeanInitializationException(
                "Bean property 'errorType' cannot be null.");
        }
        if (this.providers == null) {
            throw new BeanInitializationException(
                "Bean property 'modelBuilders' cannot be null.");
        }
        if (this.statusCodeMappings == null) {
            throw new BeanInitializationException(
                "Bean property 'statusCodeMappings' cannot be null.");
        }
    }
    
    
    public Map getErrorModel(HttpServletRequest request,
                             HttpServletResponse response,
                             Throwable error) throws Exception {
        Map model = new HashMap();
        try {
            for (int i = 0; i < providers.length; i++) {
                providers[i].referenceData(model, request);
            }
        } catch (Throwable t) {
            // Silently ignore
        }

        org.springframework.web.servlet.support.RequestContext ctx = null;
        try {
            ctx = new org.springframework.web.servlet.support.RequestContext(request);
        } catch (Throwable t) {
            
        }
        
        String errorClassName = error.getClass().getName();
        String errorMessage = (ctx == null) ? errorClassName
            : ctx.getMessage(errorClassName, errorClassName);
        
        if (errorClassName.equals(errorMessage)) {
            errorMessage = (ctx == null) ? error.getMessage()
                : ctx.getMessage(DEFAULT_ERROR_CODE, DEFAULT_ERROR_DESCRIPTION);
        }

        Map errorModel = new HashMap();
        errorModel.put(ERROR_MODEL_EXCEPTION_KEY, error);
        errorModel.put(ERROR_MODEL_ERROR_DESCRIPTION_KEY, errorMessage);
        model.put(ERROR_MODEL_KEY, errorModel);
        return model;
    }
    


    public View getErrorView(HttpServletRequest request,
                             HttpServletResponse response,
                             Throwable error) throws Exception {
        return this.errorView;
    }
    

    public int getHttpStatusCode(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Throwable error) throws Exception {

        String key = error.getClass().getName();

        if (!this.statusCodeMappings.containsKey(key)) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        Object value = this.statusCodeMappings.get(key);
        if (value == null) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName()).append(":").append(this.beanName);
        sb.append(": [");
        sb.append("errorView = ").append(this.errorView).append(", ");
        sb.append("errorType = ").append(this.errorType.getName()).append("]");
        return sb.toString();
    }
    
}
