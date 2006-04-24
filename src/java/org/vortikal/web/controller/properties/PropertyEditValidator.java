/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.web.controller.properties;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.vortikal.repository.resourcetype.Constraint;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;


public class PropertyEditValidator implements Validator {
    
    private ValueFactory valueFactory;
    
    

    public PropertyEditValidator(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }
    

    public boolean supports(Class clazz) {
        boolean retVal = (PropertyEditCommand.class.isAssignableFrom(clazz));
        return retVal;
    }


    public void validate(Object object, Errors errors) {
        PropertyEditCommand command = (PropertyEditCommand) object;

        if (command.getCancelAction() != null) {
            return;
        }

        String formValue = command.getValue();
        if ("".equals(formValue) || formValue == null) {
            if (command.getDefinition().isMandatory()) {
                errors.rejectValue("value", "mandatory property"); // XXX
            }
            return;
        }

        try {
            
            if (command.getDefinition().isMultiple()) {
                String[] splitValues = formValue.split(",");
                Value[] values = this.valueFactory.createValues(splitValues,
                                                                command.getDefinition().getType());
            } else {
                Value value = this.valueFactory.createValue(
                    formValue, command.getDefinition().getType());

                Constraint constraint = command.getDefinition().getConstraint();
                if (constraint != null) {
                    constraint.validate(value);
                }

                Value[] allowedValues = command.getDefinition().getAllowedValues();
                if (allowedValues != null) {
                    boolean found = false;
                    for (int i = 0; i < allowedValues.length; i++) {
                        if (value.equals(allowedValues[i])) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        errors.rejectValue("value", "Illegal value");
                        return;
                    }
                }
            }

        } catch (ValueFormatException e) {
            errors.rejectValue("value", "Illegal value"); // XXX
        } catch (ConstraintViolationException e) {
            errors.rejectValue("value", "Illegal value"); // XXX
        }
    }
    
}

