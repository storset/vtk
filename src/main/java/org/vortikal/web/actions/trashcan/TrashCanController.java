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
package org.vortikal.web.actions.trashcan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.RecoverableResource;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class TrashCanController extends SimpleFormController {

    private Repository repository;

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();

        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        Resource resource = this.repository.retrieve(token, uri, false);
        Service service = requestContext.getService();

        String submitURL = service.constructLink(resource, securityContext.getPrincipal());
        TrashCanCommand command = new TrashCanCommand(submitURL, resource);

        List<RecoverableResource> recoverableResources = this.repository.getRecoverableResources(token, uri);
        List<TrashCanObject> trashCanObjects = new ArrayList<TrashCanObject>();
        for (RecoverableResource rr : recoverableResources) {
            TrashCanObject tco = new TrashCanObject();
            tco.setRecoverableResource(rr);
            trashCanObjects.add(tco);
        }
        Collections.sort(trashCanObjects, new TrashCanObjectComparator());
        command.setTrashCanObjects(trashCanObjects);

        return command;
    }

    @Override
    protected ModelAndView processFormSubmission(HttpServletRequest req, HttpServletResponse resp, Object command,
            BindException errors) throws Exception {
        if (errors.hasErrors()) {
            // XXX Handle
        }
        return super.processFormSubmission(req, resp, command, errors);
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors) throws Exception {

        TrashCanCommand trashCanCommand = (TrashCanCommand) command;
        if (!trashCanCommand.hasSelectedObjectsForRecovery()) {
            return new ModelAndView(this.getFormView());
        }

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();

        String token = securityContext.getToken();
        Path currentCollectionPath = requestContext.getCurrentCollection();
        List<RecoverableResource> recoverableResources = trashCanCommand.getSelectedResourcesToRecover();
        this.repository.recover(token, currentCollectionPath, recoverableResources);

        return new ModelAndView(this.getSuccessView());
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    class TrashCanObjectComparator implements Comparator<TrashCanObject> {

        @Override
        public int compare(TrashCanObject tco1, TrashCanObject tco2) {
            String name1 = tco1.getRecoverableResource().getName();
            String name2 = tco2.getRecoverableResource().getName();
            if (name1.equals(name2)) {
                Date date1 = tco1.getRecoverableResource().getDeletedTime();
                Date date2 = tco2.getRecoverableResource().getDeletedTime();
                // most recently deleted first
                return date2.compareTo(date1);
            }
            return name1.compareTo(name2);
        }

    }

}
