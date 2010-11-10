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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.RecoverableResource;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.Message;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class TrashCanController extends SimpleFormController {

    private static Log logger = LogFactory.getLog(TrashCanController.class);

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
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors) throws Exception {

        TrashCanCommand trashCanCommand = (TrashCanCommand) command;
        if (!trashCanCommand.hasSelectedObjectsForRecovery() || !trashCanCommand.isValidAction()) {
            return this.showNewForm(request, response);
        }

        String token = SecurityContext.getSecurityContext().getToken();
        Path parentURI = trashCanCommand.getParentResource().getURI();
        List<RecoverableResource> selectedResources = trashCanCommand.getSelectedResources();

        if (trashCanCommand.getRecoverAction() != null) {

            RecoveryObject recoveryObject = this.getRecoverableResources(parentURI, selectedResources);

            // Recover what u can
            this.repository.recover(token, parentURI, recoveryObject.getRecoverable());

            // Check for conflicted resources, notify user of failed recovery
            List<RecoverableResource> conflicted = recoveryObject.getConflicted();
            if (conflicted != null && conflicted.size() > 0) {
                String msgKey = "trash-can.recovery.conflict.";
                msgKey = conflicted.size() == 1 ? msgKey + "single" : msgKey + "multiple";
                Message msg = new Message(msgKey);

                for (RecoverableResource rr : conflicted) {
                    msg.addMessage(rr.getName());
                }
                RequestContext.getRequestContext().addErrorMessage(msg);
                return this.showNewForm(request, response);
            }

            return new ModelAndView(this.getSuccessView());

        } else if (trashCanCommand.getDeletePermanentAction() != null) {

            this.repository.deleteRecoverable(token, parentURI, selectedResources);
            if (selectedResources.size() == trashCanCommand.getTrashCanObjects().size()) {
                return new ModelAndView(this.getSuccessView());
            }
            return this.showNewForm(request, response);

        } else {
            throw new IllegalArgumentException("Invalid action, cannot process");
        }
    }

    private RecoveryObject getRecoverableResources(Path parentURI, List<RecoverableResource> selectedResources) {

        List<String> duplicateConflicted = new ArrayList<String>();
        Set<String> duplicates = new HashSet<String>();
        for (RecoverableResource rr : selectedResources) {
            if (!duplicates.add(rr.getName())) {
                duplicateConflicted.add(rr.getName());
            }
        }

        List<RecoverableResource> recoverable = new ArrayList<RecoverableResource>();
        List<RecoverableResource> conflicted = new ArrayList<RecoverableResource>();
        for (RecoverableResource rr : selectedResources) {
            Path recoveryPath = parentURI.extend(rr.getName());
            if (!this.exists(recoveryPath) && !duplicateConflicted.contains(rr.getName())) {
                recoverable.add(rr);
            } else {
                conflicted.add(rr);
            }
        }
        return new RecoveryObject(recoverable, conflicted);
    }

    private boolean exists(Path path) {
        try {
            return this.repository.exists(null, path);
        } catch (Exception e) {
            logger.warn("An error occured while checking resource existense for " + path, e);
        }
        return false;
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    class RecoveryObject {

        List<RecoverableResource> recoverable;
        List<RecoverableResource> conflicted;

        protected RecoveryObject(List<RecoverableResource> recoverable, List<RecoverableResource> conflicted) {
            this.recoverable = recoverable;
            this.conflicted = conflicted;
        }

        protected List<RecoverableResource> getRecoverable() {
            return recoverable;
        }

        protected List<RecoverableResource> getConflicted() {
            return conflicted;
        }

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
