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
package org.vortikal.web.actions.create;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;
import org.vortikal.web.actions.report.subresource.SubResource;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;

public class CreateDropDownJSON implements Controller {

    private CreateDropDownProvider provider;
    private Service service;
    private Repository repository;

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

        String token = RequestContext.getRequestContext().getSecurityToken();
        List<SubResource> subresources = provider.buildSearchAndPopulateSubresources(uri, token, request);
        writeResults(subresources, request, response, token);
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

    private void writeResults(List<SubResource> subresources, HttpServletRequest request, HttpServletResponse response,
            String token) throws Exception {
        JSONArray list = new JSONArray();

        String buttonText;
        if ((buttonText = request.getParameter("service")) != null && buttonText.equals("upload-file-from-drop-down"))
            buttonText = "manage.upload-here";
        else
            buttonText = "manage.place-here";

        for (SubResource sr : subresources) {
            JSONObject o = new JSONObject();

            Path pURI = Path.fromString(sr.getUri());
            Resource resource = this.repository.retrieve(token, pURI, true);
            Principal principal = RequestContext.getRequestContext().getPrincipal();

            String title;
            try {
                String url = service.constructURL(resource, principal).getPathRepresentation();

                title = "<a target=&quot;_top&quot; class=&quot;vrtx-button-small&quot; href=&quot;" + url + "&quot;>"
                        + "<span>" + provider.getLocalizedTitle(request, buttonText, null) + "</span>" + "</a>";
            } catch (ServiceUnlinkableException e) {
                title = "<span class=&quot;no-create-permission&quot;>"
                        + provider.getLocalizedTitle(request, "manage.no-permission", null) + "</span>";
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            o.put("hasChildren", sr.hasChildren());
            o.put("text", pURI.isRoot() ? repository.getId() : sr.getName());
            o.put("uri", sr.getUri());
            o.put("spanClasses", "folder");
            o.put("title", title);

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

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setProvider(CreateDropDownProvider provider) {
        this.provider = provider;
    }

    @Required
    public void setService(Service service) {
        this.service = service;
    }

}
