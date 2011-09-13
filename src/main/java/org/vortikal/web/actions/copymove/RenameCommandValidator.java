/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.actions.copymove;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;

public class RenameCommandValidator implements Validator {

    private static Log logger = LogFactory.getLog(RenameCommandValidator.class);

    @SuppressWarnings("rawtypes")
    public boolean supports(Class clazz) {
        return (clazz == RenameCommand.class);
    }

    public void validate(Object command, Errors errors) {

        RenameCommand renameCommand = (RenameCommand) command;
        if (renameCommand.getCancel() != null)
            return;

        String name = renameCommand.getName();

        if (StringUtils.isBlank(name)) {
            errors.rejectValue("name", "manage.create.document.missing.name",
                    "A name must be provided for the document");
            return;

        }
        name = name.trim();
        if (name.contains("/") || name.contains("?") || name.equals(".") || name.equals("..")) {
            errors.rejectValue("name", "manage.create.document.invalid.name", "This is an invalid name");
            return;
        }

        Resource resource = renameCommand.getResource();
        Path newUri = renameCommand.getRenamePath();

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();

        try {
            if (repository.exists(token, newUri)) {
                Resource existing = repository.retrieve(token, newUri, false);
                if (renameCommand.getOverwrite() == null) {
                    if (resource.isCollection() || existing.isCollection()) {
                        errors.rejectValue("name", "manage.rename.resource.exists");
                    } else {
                        errors.rejectValue("name", "manage.rename.resource.overwrite",
                                "A resource of this name already exists, do you want to overwrite it?");
                        renameCommand.setConfirmOverwrite(true);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed while validating rename operation", e);
            errors.reject("manage.rename.resource.validation.failed", "Validiation failed");
        }
    }
}