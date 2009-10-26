/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.repository.search.preprocessor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.QueryException;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public abstract class MultiValuePropertyInExpressionEvaluator implements ExpressionEvaluator {

    private Log logger = LogFactory.getLog(MultiValuePropertyInExpressionEvaluator.class);
    private Repository repository;

    protected abstract String getVariableName();

    protected abstract Property getMultiValueProperty(Resource resource);

    public String evaluate(String token) throws QueryException {

        if (!matches(token)) {
            throw new QueryException("Unknown query token: '" + token + "'");
        }

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        if (requestContext != null && securityContext != null) {

            String securityToken = securityContext.getToken();
            Path uri = requestContext.getResourceURI();

            try {
                Resource resource = this.repository.retrieve(securityToken, uri, true);

                if (!resource.isCollection()) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Resource is not a collection");
                    }
                    return token;
                }

                Property multiValueProp = getMultiValueProperty(resource);

                if (multiValueProp == null) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Collection " + uri.toString() + " has no values for " + getVariableName());
                    }
                    return token;
                }

                Value[] values = multiValueProp.getValues();
                StringBuilder multiValueList = new StringBuilder();
                for (Value value : values) {
                    if (StringUtils.isNotBlank(multiValueList.toString())) {
                        multiValueList.append(",");
                    }
                    multiValueList.append(value.getStringValue());
                }

                return multiValueList.toString();
            } catch (Throwable t) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Unable to get multi value list for collection " + uri.toString(), t);
                }
            }
        }
        return token;
    }

    public boolean matches(String token) {
        return getVariableName().equals(token);
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

}
