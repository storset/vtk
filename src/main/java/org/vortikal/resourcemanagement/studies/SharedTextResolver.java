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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
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
import org.vortikal.web.servlet.ResourceAwareLocaleResolver;

/**
 * UiO specific.
 * 
 * For shared, common texts related to studies.
 * 
 * Resolves shared texts used to describe different aspects (properties) such as
 * "how to apply", "deadlines", "contact information" etc. in resource types.
 * 
 * By default, files containing shared texts are located under path
 * /vrtx/fellestekst/[propertyname].html. Shared text files are a specific
 * resource type "shared-text".
 * 
 * Shared text ability for a property in a resource type is specified by setting
 * the edithint "vrtx-shared-text" (see e.g. course-description.vrtx).
 * 
 * Edithints "vrtx-shared-text-path" and "vrtx-shared-text-file-name" can be
 * used to override default resolving from
 * /vrtx/fellestekst/[propertyname].html.
 * 
 */
public class SharedTextResolver {

    private static final String SHARED_TEXT_DEFAULT_PATH = "/vrtx/fellestekst";
    private static final String SHARED_TEXT_EDITHINT_CLASS = "vrtx-shared-text";
    private static final String SHARED_TEXT_EDITHINT_CLASS_VIEW = "vrtx-shared-text-view";
    private static final String SHARED_TEXT_EDITHINT_ATTRIBUTE_PATH = "vrtx-shared-text-path";
    private static final String SHARED_TEXT_EDITHINT_ATTRIBUTE_FILENAME = "vrtx-shared-text-file-name";

    private ResourceTypeTree resourceTypeTree;
    private HtmlPageFilter safeHtmlFilter;
    private HtmlPageParser htmlParser;
    private ResourceAwareLocaleResolver localeResolver;

    @SuppressWarnings("unchecked")
    public Map<String, JSONObject> getSharedTextValues(String docType, PropertyTypeDefinition propDef, boolean view) {

        // No propdef to work on
        if (propDef == null) {
            return null;
        }

        // Propdef has no edithints
        Map<String, Set<String>> editHints = (Map<String, Set<String>>) propDef.getMetadata().get(
                PropertyTypeDefinition.METADATA_EDITING_HINTS);
        if (editHints == null) {
            return null;
        }

        // Necessary edithint class not set for shared texts
        Set<String> classes = editHints.get("class");
        if (classes == null
                || !(classes.contains(SHARED_TEXT_EDITHINT_CLASS) || classes.contains(SHARED_TEXT_EDITHINT_CLASS_VIEW))) {
            return null;
        }

        if (classes.contains(SHARED_TEXT_EDITHINT_CLASS_VIEW) && !view) {
            return null;
        }

        String sharedTextPath = SHARED_TEXT_DEFAULT_PATH.concat("/").concat(docType);
        String sharedTextFileName = propDef.getName();

        // Check if default location is overridden
        Set<String> attributes = editHints.get("attribute");
        if (attributes != null) {
            for (String attribute : attributes) {

                if (attribute.startsWith(SHARED_TEXT_EDITHINT_ATTRIBUTE_PATH)) {
                    sharedTextPath = attribute.substring(attribute.indexOf(":") + 1);
                }

                if (attribute.startsWith(SHARED_TEXT_EDITHINT_ATTRIBUTE_FILENAME)) {
                    sharedTextFileName = attribute.substring(attribute.indexOf(":") + 1);
                }
            }
        }

        return getSharedTextValuesMap(sharedTextPath, sharedTextFileName);
    }

    public Map<String, Map<String, JSONObject>> resolveSharedTexts() throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Path currentResource = requestContext.getResourceURI();
        Resource resource = repository.retrieve(token, currentResource, false);

        ResourceTypeDefinition rtd = resourceTypeTree.getResourceTypeDefinitionByName(resource.getResourceType());
        PropertyTypeDefinition[] propTypeDefs = rtd.getPropertyTypeDefinitions();

        if (propTypeDefs != null) {

            Map<String, Map<String, JSONObject>> sharedTextPropsMap = new HashMap<String, Map<String, JSONObject>>();

            for (PropertyTypeDefinition propDef : propTypeDefs) {
                Map<String, JSONObject> sharedTexts = getSharedTextValues(resource.getResourceType(), propDef, false);
                if (sharedTexts != null) {
                    sharedTextPropsMap.put(propDef.getName(), sharedTexts);
                }
            }

            if (!sharedTextPropsMap.isEmpty()) {
                return sharedTextPropsMap;
            }
        }

        return null;
    }

    public String resolveSharedText(Resource resource, Property prop) {
        Map<String, JSONObject> resolvedsharedTexts = getSharedTextValues(resource.getResourceType(),
                prop.getDefinition(), true);

        if (resolvedsharedTexts == null || resolvedsharedTexts.isEmpty()) {
            return null;
        }

        String key = prop.getStringValue();
        Locale locale = localeResolver.resolveResourceLocale(resource);
        String localeString = locale.toString().toLowerCase();

        JSONObject propSharedText;
        if (!resolvedsharedTexts.containsKey(key) || (propSharedText = resolvedsharedTexts.get(key)) == null) {
            return null;
        }

        String sharedText;
        try {
            if (localeString.contains("ny")) {
                sharedText = propSharedText.get("description-nn").toString();
            } else if (!localeString.contains("en")) {
                sharedText = propSharedText.get("description-no").toString();
            } else {
                sharedText = propSharedText.get("description-en").toString();
            }
        } catch (Exception e) {
            sharedText = "";
        }

        return sharedText;
    }

    public Map<String, String> getLocalizedSharedTextValues(Locale locale, String docType, String propertyName) {

        Map<String, String> sharedTextMap = new HashMap<String, String>();

        // Invalid path
        if (docType == null || propertyName == null) {
            return sharedTextMap;
        }

        String sharedTextPath = SHARED_TEXT_DEFAULT_PATH.concat("/").concat(docType);
        Map<String, JSONObject> sharedTextValuesMap = getSharedTextValuesMap(sharedTextPath, propertyName);

        if (sharedTextValuesMap == null || sharedTextValuesMap.isEmpty()) {
            return sharedTextMap;
        }

        String localeString = locale.toString().toLowerCase();
        if (localeString.contains("ny")) {
            localeString = "description-nn";
        } else if (!localeString.contains("en")) {
            localeString = "description-no";
        } else {
            localeString = "description-en";
        }

        for (String key : sharedTextValuesMap.keySet()) {
            try {
                sharedTextMap.put(key, sharedTextValuesMap.get(key).get(localeString).toString());
            } catch (Exception e) {
                sharedTextMap.put(key, "");
            }
        }

        return sharedTextMap;
    }

    private Map<String, JSONObject> getSharedTextValuesMap(String sharedTextPath, String sharedTextFileName) {

        // The resulting return object
        Map<String, JSONObject> sharedTextValuesMap = new LinkedHashMap<String, JSONObject>();

        Path sharedTextResourcePath = getSharedTextResourcepath(sharedTextPath, sharedTextFileName);
        // Invalid path
        if (sharedTextResourcePath == null) {
            return sharedTextValuesMap;
        }

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();

        try {

            Resource sharedTextResource = repository.retrieve(token, sharedTextResourcePath, true);
            if (!sharedTextResource.isPublished()) {
                return sharedTextValuesMap;
            }

            InputStream stream = repository.getInputStream(token, sharedTextResourcePath, true);
            String jsonString = StreamUtil.streamToString(stream, "utf-8");
            JSONObject document = JSONObject.fromObject(jsonString);

            JSONArray json = document.getJSONObject("properties").getJSONArray("shared-text-box");

            for (Object obj : json) {
                JSONObject jsonObj = JSONObject.fromObject(obj);
                sharedTextValuesMap.put(jsonObj.getString("id"), filterDescription(jsonObj));
            }

        } catch (Exception e) {
            // Ignore, return empty result object
        }

        return sharedTextValuesMap;
    }

    private Path getSharedTextResourcepath(String sharedTextPath, String sharedTextFileName) {
        try {
            return Path.fromString(sharedTextPath.concat("/").concat(sharedTextFileName).concat(".html"));
        } catch (IllegalArgumentException iae) {
            // Ignore, return null
        }
        return null;
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
                // Ignore
            }
        }
        return j;
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

    @Required
    public void setLocaleResolver(ResourceAwareLocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

}
