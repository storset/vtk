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

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.repository.resourcetype.ValueFormatterRegistry;
import org.vortikal.repository.resourcetype.PropertyType.Type;

public class EditPublishingCommandValidator implements Validator {

    private ValueFormatterRegistry valueFormatterRegistry;

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
                Value value = getValidDate(editPublishingCommand.getPublishDate(), "publishDate", errors);
                editPublishingCommand.setPublishDateValue(value);
            }
        } else if (editPublishingCommand.getUnpublishDateUpdateAction() != null) {
            if (!StringUtils.isBlank(editPublishingCommand.getUnpublishDate())) {
                Value value = getValidDate(editPublishingCommand.getUnpublishDate(), "unpublishDate", errors);
                editPublishingCommand.setUnpublishDateValue(value);
            }
        }

    }

    private Value getValidDate(String date, String bindName, Errors errors) {
        ValueFormatter valueFormatter = this.valueFormatterRegistry.getValueFormatter(Type.TIMESTAMP);
        try {
            Value value = valueFormatter.stringToValue(date, null, null);
            return value;
        } catch (IllegalArgumentException e) {
            errors.rejectValue(bindName, "publishing.edit.invalid.date", "Invalid date");
        }
        return null;
    }

    public void setValueFormatterRegistry(ValueFormatterRegistry valueFormatterRegistry) {
        this.valueFormatterRegistry = valueFormatterRegistry;
    }

}
