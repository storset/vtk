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
package org.vortikal.web.actions.properties;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.vortikal.repository.Vocabulary;
import org.vortikal.repository.resourcetype.Constraint;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.security.PrincipalManager;


public class PropertyEditValidator implements Validator {
    
    private ValueFactory valueFactory;
    
    private PrincipalManager principalManager;
    

    @SuppressWarnings("unchecked")
    public boolean supports(Class clazz) {
        return PropertyEditCommand.class.isAssignableFrom(clazz);
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
                Value[] values = this.valueFactory.createValues(
                    splitValues, command.getDefinition().getType());

                if (command.getDefinition().getType() == PropertyType.Type.PRINCIPAL) {
                    for (Value v: values) {
                        if (!this.principalManager.validatePrincipal(v.getPrincipalValue())) {
                            throw new ValueFormatException("Invalid principal " + v);
                        }
                    } 
                }

            } else {
                Value value = this.valueFactory.createValue(
                    formValue, command.getDefinition().getType());
                if (command.getDefinition().getType() == PropertyType.Type.PRINCIPAL) {
                    if (!this.principalManager.validatePrincipal(value.getPrincipalValue())) {
                            throw new ValueFormatException("Invalid principal " + value);
                    }
                }

                Constraint constraint = command.getDefinition().getConstraint();
                if (constraint != null) {
                    constraint.validate(value);
                }

                Vocabulary<Value> vocabulary = command.getDefinition().getVocabulary();

                if (vocabulary == null) {
                    return;
                }
                
                Value[] allowedValues = vocabulary.getAllowedValues();

                if (allowedValues == null) {
                    return;
                }

                for (Value v: allowedValues) {
                    if (value.equals(v)) {
                        return;
                    }
                }

                errors.rejectValue("value", "Illegal value");

            }

        } catch (ValueFormatException e) {
            errors.rejectValue("value", "Illegal value: " + e.getMessage()); // XXX
        } catch (ConstraintViolationException e) {
            errors.rejectValue("value", "Illegal value: " + e.getMessage()); // XXX
        }
    }

    
    
    @Required
    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }


    @Required
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

}

