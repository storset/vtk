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
package org.vortikal.resourcemanagement.edit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.vortikal.resourcemanagement.EditRule;
import org.vortikal.resourcemanagement.EditablePropertyDescription;
import org.vortikal.resourcemanagement.PropertyDescription;
import org.vortikal.resourcemanagement.StructuredResource;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.EditRule.EditRuleType;
import org.vortikal.web.actions.UpdateCancelCommand;
import org.vortikal.web.service.URL;

@SuppressWarnings("unchecked")
public class FormSubmitCommand extends UpdateCancelCommand {

    private StructuredResource resource;
    private List<FormElementBox> elements = new ArrayList<FormElementBox>();

    public FormSubmitCommand(StructuredResource resource, URL url) {
        super(url.toString());
        this.resource = resource;
        StructuredResourceDescription type = resource.getType();
        for (PropertyDescription def : type.getAllPropertyDescriptions()) {
            if (def instanceof EditablePropertyDescription) {
                EditablePropertyDescription editable = (EditablePropertyDescription) def;
                FormElementBox elementBox = new FormElementBox(def.getName());
                elementBox.addFormElement(new FormElement(editable, null, resource.getProperty(def.getName())));
                this.elements.add(elementBox);
            }
        }

        List<EditRule> editRules = type.getEditRules();
        if (editRules != null && editRules.size() > 0) {
            for (EditRule editRule : editRules) {
                EditRuleType ruleType = editRule.getType();
                if (EditRuleType.GROUP.equals(ruleType)) {
                    groupElements(editRule);
                } else if (EditRuleType.POSITION_BEFORE.equals(ruleType)) {
                    rearrangePosition(editRule, EditRuleType.POSITION_BEFORE);
                } else if (EditRuleType.POSITION_AFTER.equals(ruleType)) {
                    rearrangePosition(editRule, EditRuleType.POSITION_AFTER);
                } else if (EditRuleType.EDITHINT.equals(ruleType)) {
                    setEditHints(editRule);
                }
            }
        }
    }

    private void groupElements(EditRule editRule) {
        FormElementBox elementBox = new FormElementBox(editRule.getName());
        List<String> groupedProps = (List<String>) editRule.getValue();
        for (String groupedProp : groupedProps) {
            FormElement formElement = this.findElement(groupedProp);
            if (formElement != null) {
                elementBox.addFormElement(formElement);
                this.removeElementBox(formElement);
            }
        }
        this.elements.add(elementBox);
    }

    private void rearrangePosition(EditRule editRule, EditRuleType ruleType) {
        int indexOfpropToMove = -1;
        int indexToMoveToo = -1;
        for (int i = 0; i < elements.size(); i++) {
            FormElementBox elementBox = elements.get(i);
            if (editRule.getName().equals(elementBox.getName())) {
                indexOfpropToMove = i;
            }
            if (editRule.getValue().toString().equals(elementBox.getName())) {
                indexToMoveToo = i;
            }
        }
        if (indexOfpropToMove != -1 && indexToMoveToo != -1 && indexOfpropToMove != indexToMoveToo) {
            int rotation = EditRuleType.POSITION_BEFORE.equals(ruleType) ? 0 : 1;
            if (indexToMoveToo < indexOfpropToMove) {
                Collections.rotate(elements.subList(indexToMoveToo + rotation, indexOfpropToMove + 1), 1);
            } else {
                Collections.rotate(elements.subList(indexOfpropToMove, indexToMoveToo + rotation), -1);
            }
        }
    }

    private void setEditHints(EditRule editRule) {
        for (FormElementBox elementBox : elements) {
            if (elementBox.getName().equals(editRule.getName())) {
                elementBox.addMetaData(editRule.getEditHintKey(), editRule.getEditHintValue());
            }
            for (FormElement formElement : elementBox.getFormElements()) {
                EditablePropertyDescription pd = formElement.getDescription();
                if (pd.getName().equals(editRule.getName())) {
                    pd.addEdithint(editRule.getEditHintKey(), editRule.getEditHintValue());
                }
            }
        }
    }

    public List<FormElementBox> getElements() {
        return Collections.unmodifiableList(this.elements);
    }

    public StructuredResource getResource() {
        return this.resource;
    }

    public void bind(String name, Object value) throws Exception {
        FormElement elem = findElement(name);
        if (elem == null) {
            throw new IllegalArgumentException("No such element: " + name);
        }
        elem.setValue(value);
    }

    private FormElement findElement(String name) {
        for (FormElementBox elementBox : this.elements) {
            for (FormElement formElement : elementBox.getFormElements()) {
                if (formElement.getDescription().getName().equals(name)) {
                    return formElement;
                }
            }
        }
        return null;
    }

    private void removeElementBox(FormElement formElement) {
        FormElementBox elementBoxToRemove = null;
        for (FormElementBox elementBox : this.elements) {
            List<FormElement> formElements = elementBox.getFormElements();
            if (formElements.size() == 1 && formElement.equals(formElements.get(0))) {
                elementBoxToRemove = elementBox;
                break;
            }
        }
        if (elementBoxToRemove != null) {
            this.elements.remove(elementBoxToRemove);
        }
    }

    public void sync() {
        List<PropertyDescription> descriptions = this.resource.getType().getAllPropertyDescriptions();
        for (PropertyDescription desc : descriptions) {
            if (desc instanceof EditablePropertyDescription) {
                String name = desc.getName();
                FormElement elem = findElement(name);
                Object value = elem.getValue();
                this.resource.removeProperty(name);
                if (value != null) {
                    this.resource.addProperty(name, value);
                }
            }
        }
    }

}
