/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.controller;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;


public class FCKeditorConnector implements Controller {

    private static enum Command {
        GetFolders, GetFoldersAndFiles, CreateFolder, FileUpload;
    }
            
    private Repository repository;
    private Service viewService;
    private String viewName;
    

    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    @Required public void setViewService(Service viewService) {
        this.viewService = viewService;
    }
    
    @Required public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    
    
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();

        String currentFolderParam = request.getParameter("CurrentFolder");
        Resource currentFolder = this.repository.retrieve(token, currentFolderParam, true);
        if (!currentFolder.isCollection()) {
            
        }
        Resource[] children = this.repository.listChildren(token, currentFolder.getURI(), true);

        String commandParam = request.getParameter("Command");
        Command c = Command.valueOf(commandParam);

        Map model = new HashMap();
        model.put("currentFolder", currentFolder.getURI());
        model.put("command", c.name());

        switch (c) {
            case GetFolders:
                model.put("folders", getFolders(children));
                break;
            case GetFoldersAndFiles:
                model.put("folders", getFolders(children));
                model.put("files", getFiles(children));
                break;
            default:
                throw new RuntimeException("Not implemented");
        }

        return new ModelAndView(this.viewName, model);
    }
    
    private Map<String, Map> getFolders(Resource[] children) {
        Map<String, Map> result = new HashMap<String, Map>();
        for (Resource r: children) {
            if (r.isCollection()) {
                Map<String, Object> entry = new HashMap<String, Object>();
                URL url = this.viewService.constructURL(r, null);
                entry.put("resource", r);
                entry.put("url", url);
                result.put(r.getURI(), entry);
            }
        }
        return result;
    }

    private Map<String, Map> getFiles(Resource[] children) {
        Map<String, Map> result = new HashMap<String, Map>();
        for (Resource r: children) {
            if (!r.isCollection()) {
                Map<String, Object> entry = new HashMap<String, Object>();
                URL url = this.viewService.constructURL(r, null);
                entry.put("resource", r);
                entry.put("url", url);
                entry.put("contentLength", r.getContentLength());
                result.put(r.getURI(), entry);
            }
        }
        return result;
    }
    

}
