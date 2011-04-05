/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.web.service.subresource;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.web.RequestContext;
import org.vortikal.web.actions.report.subresource.SubResourcePermissions;
import org.vortikal.web.actions.report.subresource.SubResourcePermissionsProvider;
import org.vortikal.web.service.Service;

public class SubResourceJSONService implements Controller, InitializingBean {
    
    private SubResourcePermissionsProvider provider;
    private Service permissionsService;
      
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = null;
        try {
          uri = (String) request.getParameter("uri");  
        } catch (Exception e) {
            badRequest(e, response);
            return null;
        }
        if (uri == null) {
          return null;
        }
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        List<SubResourcePermissions> subresources = provider.buildSearchAndPopulateSubresources(uri, token, request);
        writeResults(subresources, request, response);
        return null;
    }

    private void badRequest(Throwable e, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter writer = response.getWriter();
        try {
            writer.write(e.getMessage());
        } finally {
            writer.close();
        }
    }
    
    private void writeResults(List<SubResourcePermissions> subresources, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONArray list = new JSONArray();
        for (SubResourcePermissions sr: subresources) {
            JSONObject o = new JSONObject();

            String listClasses = "";
            String spanClasses = "";
            
            // Add classes
            if (sr.isCollection()) {
              spanClasses = "folder";
              o.put("hasChildren", true);
            } else {
              spanClasses = "file";
            }
            if (sr.isReadRestricted()) {
              spanClasses += " restricted";
            } else {
              spanClasses += " allowed-for-all";    
            }
            
            // Generate title
            StringBuilder title = new StringBuilder();
            title.append("<span id=&quot;title-wrapper&quot;><strong id=&quot;title&quot;>" + sr.getName() + "</strong>");
            
            String uriService = permissionsService.constructURL(Path.fromString(sr.getUri())).getPathRepresentation();

            if (sr.isInheritedAcl()) {
              title.append(" " + provider.getLocalizedTitle(request, "report.collection-structure.inherited-permissions", null) + " (<a href=&quot;" + uriService
                         + "&quot;>" + provider.getLocalizedTitle(request, "report.collection-structure.edit", null)
                         + "</a>)</span><span class=&quot;inherited-permissions&quot;>");
            } else {
              title.append(" " + provider.getLocalizedTitle(request, "report.collection-structure.own-permissions", null) + " (<a href=&quot;" + uriService 
                         + "&quot;>" + provider.getLocalizedTitle(request, "report.collection-structure.edit", null) + "</a>)</span>");
              listClasses = "not-inherited";
            }
            
            // Generate table with permissions
            String notAssigned = provider.getLocalizedTitle(request, "permissions.not.assigned", null).toLowerCase();
            title.append("<table><tbody>");
            
            String read = sr.getRead().isEmpty() ? notAssigned : sr.getRead();
            title.append("<tr><td>" + provider.getLocalizedTitle(request, "permissions.privilege.read", null) + ":</td><td>" + read + "</td></tr>");
            
            String write = sr.getWrite().isEmpty() ? notAssigned : sr.getWrite();
            title.append("<tr><td>" + provider.getLocalizedTitle(request, "permissions.privilege.read-write", null) + ":</td><td>" + write + "</td></tr>");
            
            String admin = sr.getAdmin().isEmpty() ? notAssigned : sr.getAdmin();
            title.append("<tr><td>" + provider.getLocalizedTitle(request, "report.collection-structure.admin-permission", null) + ":</td><td>" + admin + "</td></tr>");
            
            title.append("</tbody></table>");
            
            if (sr.isInheritedAcl()) {
              title.append("</span>"); 
            }
            
            // Add to JSON-object
            o.put("text", sr.getName());
            o.put("uri", sr.getUri());
            o.put("title", title.toString());
            o.put("listClasses", listClasses);
            o.put("spanClasses", spanClasses);
            
            // Add object to JSON-array
            list.add(o);
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain;charset=utf-8");
        PrintWriter writer = response.getWriter();
        try {
            writer.print(list.toString(1));
        } finally {
            writer.close();
        }
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Required
    public void setProvider(SubResourcePermissionsProvider provider) {
        this.provider = provider;
    }
    
    @Required
    public void setPermissionsService(Service permissionsService) {
        this.permissionsService = permissionsService;
    }

}
