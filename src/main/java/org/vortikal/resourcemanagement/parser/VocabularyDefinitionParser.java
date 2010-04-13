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
package org.vortikal.resourcemanagement.parser;

import java.util.List;
import java.util.Locale;

import org.antlr.runtime.tree.CommonTree;
import org.apache.commons.lang.LocaleUtils;
import org.vortikal.resourcemanagement.PropertyDescription;
import org.vortikal.resourcemanagement.StructuredResourceDescription;

public class VocabularyDefinitionParser {

    @SuppressWarnings("unchecked")
    public void handleVocabulary(StructuredResourceDescription srd, List<CommonTree> propertyDescriptions) {
        if (propertyDescriptions == null || propertyDescriptions.isEmpty()) {
            return;
        }
        for (CommonTree propName : propertyDescriptions) {
            for (CommonTree vocab : (List<CommonTree>) propName.getChildren()) {
                if ("range".equals(vocab.getText())) {

                    // XXX implement

                } else {
                    for (CommonTree vocabularyName : (List<CommonTree>) vocab.getChildren()) {
                        for (CommonTree vocabularyValue : (List<CommonTree>) vocabularyName.getChildren()) {
                            Locale locale = LocaleUtils.toLocale(vocab.getText());
                            List<PropertyDescription> propDescs = srd.getPropertyDescriptions();
                            PropertyDescription p = null;
                            for (PropertyDescription propDesc : propDescs) {
                                if (propDesc.getName().equals(propName.getText())) {
                                    p = propDesc;
                                }
                            }
                            if (p != null) {
                                p.addVocabulary(locale, vocabularyName.getText(), vocabularyValue.getText());
                            }
                        }
                    }
                }
            }
        }
    }

}
