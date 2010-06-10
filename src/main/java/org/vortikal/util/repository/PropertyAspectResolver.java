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
package org.vortikal.util.repository;

import net.sf.json.JSONObject;

import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;

public class PropertyAspectResolver {

    private Repository repository;
    private PropertyTypeDefinition aspectsPropdef;
    PropertyAspectDescription fieldConfig;
    
    public PropertyAspectResolver(Repository repository, PropertyTypeDefinition aspectsPropdef, PropertyAspectDescription fieldConfig) {
        this.repository = repository;
        this.aspectsPropdef = aspectsPropdef;
        this.fieldConfig = fieldConfig;
    }
    
    public JSONObject resolve(Path uri, String aspect) throws Exception {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        JSONObject result = new JSONObject();
        traverse(result, aspect, uri, token);
        return result;
    }

    
    private void traverse(JSONObject result, String aspect, Path uri, String token) {
        Path currentURI = Path.fromString(uri.toString());
        while (true) {
            Resource r = null;
            try {
                r = repository.retrieve(token, currentURI, true);
            } catch (Throwable t) { }
            if (r != null) {
                Property property = r.getProperty(aspectsPropdef);
                if (property != null && property.getType() == PropertyType.Type.JSON) {
                    JSONObject value = property.getJSONValue();
                    if (value.get(aspect) != null) {
                        value = value.getJSONObject(aspect);

                        for (PropertyAspectField field : this.fieldConfig.getFields()) {
                            Object key = field.getIdentifier();
                            Object newValue = value.get(key);

                            if (currentURI.equals(uri)) {
                                result.put(key, newValue);
                            } else if (field.isInherited() && result.get(key) == null) {
                                result.put(key, newValue);
                            }
                        }
                    }
                }
            }
            currentURI = currentURI.getParent();
            if (currentURI == null) {
                break;
            }
        }
    }
 }
