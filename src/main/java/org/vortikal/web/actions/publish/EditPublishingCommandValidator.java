/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.web.actions.publish;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;

public class EditPublishingCommandValidator implements Validator {

    private static final SimpleDateFormat DATEFORMATTER;
    static {
        DATEFORMATTER = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        DATEFORMATTER.setLenient(false);
    }

    private PropertyTypeDefinition publishDatePropDef;
    private PropertyTypeDefinition unpublishDatePropDef;

    @Override
    @SuppressWarnings("unchecked")
    public boolean supports(Class clazz) {
        return clazz == EditPublishingCommand.class;
    }

    @Override
    public void validate(Object command, Errors errors) {

        EditPublishingCommand editPublishingCommand = (EditPublishingCommand) command;

        if (editPublishingCommand.getCancelAction() != null) {
            return;
        }

        if (editPublishingCommand.getPublishDateUpdateAction() != null) {
            if (!StringUtils.isBlank(editPublishingCommand.getPublishDate())) {
                Date date = getValidDate(editPublishingCommand.getPublishDate(), "publishDate", errors);
                if (date == null) {
                    return;
                }
                editPublishingCommand.setPublishDateValue(new Value(date, false));
            } else {
                Property unpublishDateProp = editPublishingCommand.getResource().getProperty(this.unpublishDatePropDef);
                if (unpublishDateProp != null) {
                    editPublishingCommand.getResource().removeProperty(this.unpublishDatePropDef);
                }
            }
        } else if (editPublishingCommand.getUnpublishDateUpdateAction() != null) {
            if (!StringUtils.isBlank(editPublishingCommand.getUnpublishDate())) {
                Date date = getValidDate(editPublishingCommand.getUnpublishDate(), "unpublishDate", errors);
                if (date == null) {
                    return;
                }
                Property publishDateProp = editPublishingCommand.getResource().getProperty(this.publishDatePropDef);
                if (publishDateProp == null) {
                    errors.rejectValue("unpublishDate", "publishing.edit.invalid.unpublishDateNonExisting",
                            "Invalid date");
                } else if (date.before(publishDateProp.getDateValue())) {
                    errors.rejectValue("unpublishDate", "publishing.edit.invalid.unpublishDateBefore", "Invalid date");
                }
                editPublishingCommand.setUnpublishDateValue(new Value(date, false));
            }
        }

    }

    private Date getValidDate(String dateString, String bindName, Errors errors) {
        try {
            return DATEFORMATTER.parse(dateString);
        } catch (ParseException e) {
            errors.rejectValue(bindName, "publishing.edit.invalid." + bindName, "Invalid date");
        }
        return null;
    }

    @Required
    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

    @Required
    public void setUnpublishDatePropDef(PropertyTypeDefinition unpublishDatePropDef) {
        this.unpublishDatePropDef = unpublishDatePropDef;
    }

}
