/* Copyright (c) 2013, University of Oslo, Norway
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
package org.vortikal.resourcemanagement.studies;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;

/**
 * XXX Very UiO specific.
 * 
 * For resources related to studies, resolve shared texts used to describe
 * different aspects such as "how to apply", "deadlines" etc.
 * 
 */
public class SharedTextResolver {

    private ResourceTypeTree resourceTypeTree;
    private HtmlPageFilter safeHtmlFilter;
    private HtmlPageParser htmlParser;

    /* TODO: Need better error handling */
    @SuppressWarnings("unchecked")
    public Map<String, JSONObject> getSharedTextValues(String docType, String propName) {

        // XXX Hack for re-use amongst different resource-types:
        if (propName.equals("studinfo-kontakt")) {
            docType = "studinfo-kontakt";
        }

        Path sharedTextResourcePath = Path.fromString("/vrtx/fellestekst/" + docType + "/" + propName + ".html");
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();

        Map<String, JSONObject> sharedTextValuesMap = new LinkedHashMap<String, JSONObject>();

        try {
            Resource sharedTextResource = repository.retrieve(token, sharedTextResourcePath, false);
            if (!sharedTextResource.isPublished()) {
                return sharedTextValuesMap;
            }
        } catch (Exception e) {
            return sharedTextValuesMap;
        }
        try {

            InputStream stream = repository.getInputStream(token, sharedTextResourcePath, false);
            String jsonString = StreamUtil.streamToString(stream, "utf-8");
            JSONObject document = JSONObject.fromObject(jsonString);

            List<Object> json = (List<Object>) document.getJSONObject("properties").get("shared-text-box");

            for (Object obj : json) {
                JSONObject jsonObj = JSONObject.fromObject(obj);
                sharedTextValuesMap.put(jsonObj.getString("id"), filterDescription(jsonObj));
            }
            return sharedTextValuesMap;
        } catch (Exception e) {
            return sharedTextValuesMap;
        }
    }

    private JSONObject filterDescription(JSONObject j) {

        String[] list = { "description-no", "description-nn", "description-en" };

        for (String descriptionKey : list) {
            HtmlFragment fragment;
            try {
                fragment = htmlParser.parseFragment(j.getString(descriptionKey));
                fragment.filter(safeHtmlFilter);
                j.put(descriptionKey, fragment.getStringRepresentation());
            } catch (Exception e) {
                // XXX handle
            }
        }
        return j;
    }

    @SuppressWarnings("rawtypes")
    public Map<String, Map<String, JSONObject>> resolveSharedTexts(HttpServletRequest request) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Path currentResource = requestContext.getResourceURI();
        Resource r = repository.retrieve(token, currentResource, false);

        ResourceTypeDefinition rtd = resourceTypeTree.getResourceTypeDefinitionByName(r.getResourceType());
        PropertyTypeDefinition[] propTypeDefs = rtd.getPropertyTypeDefinitions();

        if (propTypeDefs != null) {

            Map<String, Map<String, JSONObject>> sharedTextPropsMap = new HashMap<String, Map<String, JSONObject>>();

            for (PropertyTypeDefinition propDef : propTypeDefs) {
                if (propDef != null) {
                    Map editHints = (Map) propDef.getMetadata().get(PropertyTypeDefinition.METADATA_EDITING_HINTS);
                    if (editHints != null && "vrtx-shared-text".equals(editHints.get("class"))) {
                        sharedTextPropsMap.put(propDef.getName(),
                                getSharedTextValues(r.getResourceType(), propDef.getName()));
                    }
                }
            }

            if (!sharedTextPropsMap.isEmpty()) {
                return sharedTextPropsMap;
            }
        }

        return null;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setSafeHtmlFilter(HtmlPageFilter safeHtmlFilter) {
        this.safeHtmlFilter = safeHtmlFilter;
    }

    @Required
    public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }

}
