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
package org.vortikal.resourcemanagement.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.vortikal.repository.resource.ResourcetreeLexer;
import org.vortikal.resourcemanagement.DerivedPropertyDescription;
import org.vortikal.resourcemanagement.PropertyDescription;
import org.vortikal.resourcemanagement.SimplePropertyDescription;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.DerivedPropertyDescription.EvalDescription;

public class PropertyDescriptionParser {

    @SuppressWarnings("unchecked")
    public void parsePropertyDescriptions(StructuredResourceDescription srd, List<CommonTree> propertyDescriptions) {
        List<PropertyDescription> props = new ArrayList<PropertyDescription>();
        if (propertyDescriptions != null) {
            for (CommonTree propDesc : propertyDescriptions) {
                if (propDesc.getChild(0).getType() == ResourcetreeLexer.DERIVED) {
                    DerivedPropertyDescription p = new DerivedPropertyDescription();
                    p.setName(propDesc.getText());
                    populateDerivedPropertyDescription(p, propDesc.getChildren());
                    props.add(p);
                } else {
                    SimplePropertyDescription p = new SimplePropertyDescription();
                    p.setName(propDesc.getText());
                    populateSimplePropertyDescription(p, propDesc.getChildren());
                    props.add(p);
                }
            }
            srd.setPropertyDescriptions(props);
        }
    }

    private void populateSimplePropertyDescription(SimplePropertyDescription p, List<CommonTree> propertyDescription) {
        for (CommonTree descEntry : propertyDescription) {
            switch (descEntry.getType()) {
            case ResourcetreeLexer.PROPTYPE:
                p.setType(descEntry.getText());
                break;
            case ResourcetreeLexer.REQUIRED:
                p.setRequired(true);
                break;
            case ResourcetreeLexer.NOEXTRACT:
                p.setNoExtract(true);
                break;
            case ResourcetreeLexer.OVERRIDES:
                p.setOverrides(descEntry.getChild(0).getText());
                break;
            case ResourcetreeLexer.MULTIPLE:
                p.setMultiple(true);
                break;
            case ResourcetreeLexer.EXTERNAL:
                p.setExternalService(descEntry.getChild(0).getText());
                break;
            default:
                throw new IllegalStateException("Unknown token type for simple property description: "
                        + descEntry.getType());
            }
        }
    }

    private void populateDerivedPropertyDescription(DerivedPropertyDescription p, List<CommonTree> propertyDescription) {
        for (CommonTree descEntry : propertyDescription) {
            switch (descEntry.getType()) {
            case ResourcetreeLexer.DERIVED:
                handleDerivedProperty(p, descEntry);
                break;
            case ResourcetreeLexer.OVERRIDES:
                p.setOverrides(descEntry.getChild(0).getText());
                break;
            case ResourcetreeLexer.MULTIPLE:
                p.setMultiple(true);
                break;
            default:
                throw new IllegalStateException("Unknown token type for derived property description: "
                        + descEntry.getType());
            }
        }
    }

    private void handleDerivedProperty(DerivedPropertyDescription p, CommonTree descEntry) {
        List<String> dependentFields = new ArrayList<String>();
        Tree fields = descEntry.getChild(0);
        for (int i = 0; i < fields.getChildCount(); i++) {
            dependentFields.add(fields.getChild(i).getText());
        }

        Tree eval = descEntry.getChild(1);
        boolean quote = false;
        List<EvalDescription> evalDescriptions = new ArrayList<EvalDescription>();
        for (int i = 0; i < eval.getChildCount(); i++) {
            if (ResourcetreeLexer.DQ == eval.getChild(i).getType()) {
                quote = !quote;
                continue;
            }
            String value = eval.getChild(i).getText();
            EvalDescription desc = new EvalDescription(quote, value);
            evalDescriptions.add(desc);
        }
        p.setDependentProperties(dependentFields);
        p.setEvalDescriptions(evalDescriptions);
    }
}
