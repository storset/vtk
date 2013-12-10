/* Copyright (c) 2011, 2013 University of Oslo, Norway
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
package org.vortikal.web.actions.create;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.web.JSONController;
import org.vortikal.web.JSONTreeHelper;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;

public class CreateDropDownController extends JSONController {

    private CreateDropDownProvider provider;
    private Service service;
    private Repository repository;
    private PropertyTypeDefinition unpublishedCollectionPropDef;
    
    private static String PARAMETER_SERVICE = "service";
    private static String PARAMETER_REPORT_TYPE = "report-type";

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean success = handleUri(request, response);
        if(!success) return null;
        
        String token = RequestContext.getRequestContext().getSecurityToken();
        List<Resource> resources = provider.buildSearchAndPopulateResources(uri, token);
        writeResults(resources, request, response, token);
        return null;
    }
    

    private void writeResults(List<Resource> resources, HttpServletRequest request, HttpServletResponse response,
            String token) throws Exception {

        String buttonText = mapServiceParamToButtonText(request.getParameter(PARAMETER_SERVICE));
        Map<String, String> uriParameters = getReportTypeParam(request.getParameter(PARAMETER_REPORT_TYPE));
        
        JSONArray listNodes = new JSONArray();
        for (Resource resource : resources) {
            JSONObject node = generateJSONObjectNode(resource, token, request, uriParameters, buttonText);
            if(listNodes != null) {
                listNodes.add(node);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }
        okRequest(listNodes, response);
    }

    
    private JSONObject generateJSONObjectNode(Resource resource, String token, HttpServletRequest request, Map<String, String> uriParameters, String buttonText) {
        JSONObject o = new JSONObject();
        Principal principal = RequestContext.getRequestContext().getPrincipal();

        String title;
        try {
            String url = service.constructURL(resource, principal, uriParameters).getPathRepresentation();     

            title = "<a target=&quot;_top&quot; class=&quot;vrtx-button-small&quot; href=&quot;" + url + "&quot;>"
                    + "<span>" + provider.getLocalizedTitle(request, buttonText, null) + "</span>" + "</a>";
        } catch (ServiceUnlinkableException e) {
            title = "<span class=&quot;no-create-permission&quot;>"
                    + provider.getLocalizedTitle(request, "manage.no-permission", null) + "</span>";
        } catch (Exception e) {
            return null;
        }
        Path uri = resource.getURI();
        
        boolean unpublished = resource.getProperty(unpublishedCollectionPropDef) != null 
                || !resource.isPublished();
        
        o.put(JSONTreeHelper.URI, uri.toString());
        o.put(JSONTreeHelper.TITLE, title);
        o.put(JSONTreeHelper.CLASSES_SPAN, JSONTreeHelper.CLASSES_FOLDER + (!unpublished ? "" : " " + JSONTreeHelper.CLASSES_UNPUBLISHED));
        o.put(JSONTreeHelper.HAS_CHILDREN, provider.hasChildren(resource, token));
        o.put(JSONTreeHelper.TEXT, uri.isRoot() ? repository.getId() : resource.getName());

        return o;
    }

    
    private String mapServiceParamToButtonText(String serviceParam) {
        if (serviceParam != null) {
            if (serviceParam.equals("upload-file-from-drop-down")) {
                return "manage.upload-here";
            } else if (serviceParam.equals("view-report-from-drop-down")) {
                return "manage.view-this";
            }
        }
        return "manage.place-here";
    }
    
    private Map<String, String> getReportTypeParam(String reportType) {
        Map<String, String> uriParameters = new HashMap<String, String>();
        if(reportType != null) {
            uriParameters.put(PARAMETER_REPORT_TYPE, reportType);
        }
        return uriParameters;
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setProvider(CreateDropDownProvider provider) {
        this.provider = provider;
    }
    
    @Required
    public void setUnpublishedCollectionPropDef(PropertyTypeDefinition prop) {
        this.unpublishedCollectionPropDef = prop;
    }

    @Required
    public void setService(Service service) {
        this.service = service;
    }

}
