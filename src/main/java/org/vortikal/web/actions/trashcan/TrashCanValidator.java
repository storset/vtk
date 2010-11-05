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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;

public class TrashCanValidator implements Validator {

    private Repository repository;

    @Override
    @SuppressWarnings("unchecked")
    public boolean supports(Class clazz) {
        return (clazz == TrashCanCommand.class);
    }

    @Override
    public void validate(Object command, Errors errors) {
        TrashCanCommand trashCanCommand = (TrashCanCommand) command;
        if (!trashCanCommand.hasSelectedObjectsForRecovery()) {
            errors.rejectValue("trashCanObjects", "trash-can.no.resource.selected");
        }

        Resource parent = trashCanCommand.getParentResource();
        Path parentUri = parent.getURI();
        List<TrashCanObject> trashCanObjects = trashCanCommand.getTrashCanObjects();

        Set<String> duplicates = new HashSet<String>();
        for (TrashCanObject tco : trashCanObjects) {
            if (tco.isSelectedForRecovery()) {
                String recoverToName = tco.getRecoverableResource().getName();
                if (!duplicates.add(recoverToName)) {
                    errors.rejectValue("trashCanObjects",
                            "trash-can.no.resource.recovery.multiple.selected.name.conflict");
                    return;
                }
            }
        }

        for (TrashCanObject tco : trashCanObjects) {
            if (tco.isSelectedForRecovery()) {
                Path recoveryPath = parentUri.extend(tco.getRecoverableResource().getName());
                if (this.exists(recoveryPath)) {
                    tco.setRecoveryNameConflicted(true);
                }
            }
        }

    }

    private boolean exists(Path path) {
        try {
            return this.repository.exists(null, path);
        } catch (Exception e) {
            // XXX handle
        }
        return false;
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

}
