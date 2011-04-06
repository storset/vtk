/* Copyright (c) 2010, University of Oslo, Norway
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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.QueryException;
import org.vortikal.web.RequestContext;

public class ManuallyApprovedResourcesExpressionEvaluator implements ExpressionEvaluator {

    private final String variableName = "manuallyApprovedResources";
    private PropertyTypeDefinition manuallyApprovedResourcesPropDef;

    @Override
    public String evaluate(String token) throws QueryException {

        if (!matches(token)) {
            throw new QueryException("Unknown query token: '" + token + "'");
        }

        RequestContext requestContext = RequestContext.getRequestContext();
        if (requestContext != null) {

            String securityToken = requestContext.getSecurityToken();
            Path uri = requestContext.getResourceURI();
            Repository repository = requestContext.getRepository();

            try {
                Resource resource = repository.retrieve(securityToken, uri, true);
                if (!resource.isCollection()) {
                    return token;
                }

                Property manuallyApprovedResourcesProp = resource.getProperty(this.manuallyApprovedResourcesPropDef);
                if (manuallyApprovedResourcesProp == null) {
                    return token;
                }

                StringBuilder multiValueList = new StringBuilder();
                String value = manuallyApprovedResourcesProp.getFormattedValue();
                JSONArray arr = JSONArray.fromObject(value);
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    if (StringUtils.isNotBlank(multiValueList.toString())) {
                        multiValueList.append(",");
                    }
                    multiValueList.append(obj.getString("uri").replaceAll(" ", "\\\\ "));
                }
                return multiValueList.toString();
            } catch (Throwable t) {
                // XXX log
            }
        }
        return token;
    }

    @Override
    public boolean matches(String token) {
        return this.variableName.equals(token);
    }

    @Required
    public void setManuallyApprovedResourcesPropDef(PropertyTypeDefinition manuallyApprovedResourcesPropDef) {
        this.manuallyApprovedResourcesPropDef = manuallyApprovedResourcesPropDef;
    }

}
