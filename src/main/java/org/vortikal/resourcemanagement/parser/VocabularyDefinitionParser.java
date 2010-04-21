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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.antlr.runtime.tree.CommonTree;
import org.apache.commons.lang.LocaleUtils;
import org.vortikal.repository.resource.ResourcetreeLexer;
import org.vortikal.resourcemanagement.JSONPropertyAttributeDescription;
import org.vortikal.resourcemanagement.JSONPropertyDescription;
import org.vortikal.resourcemanagement.PropertyDescription;
import org.vortikal.resourcemanagement.StructuredResourceDescription;

public class VocabularyDefinitionParser {

    public void handleVocabulary(StructuredResourceDescription srd, List<CommonTree> propertyVocabularyEntries) {
        if (propertyVocabularyEntries == null || propertyVocabularyEntries.isEmpty()) {
            return;
        }
        for (CommonTree propertyVocabularyEntry : propertyVocabularyEntries) {
            this.handleVocabularyEntries(srd, propertyVocabularyEntry);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleVocabularyEntries(StructuredResourceDescription srd, CommonTree propertyVocabularyEntry) {
        for (CommonTree propertyVocabulary : (List<CommonTree>) propertyVocabularyEntry.getChildren()) {
            this.resolveVocabulary(srd, propertyVocabularyEntry.getText(), propertyVocabulary);
        }
    }

    @SuppressWarnings("unchecked")
    private void resolveVocabulary(StructuredResourceDescription srd, String propName, CommonTree propertyVocabulary) {
        if (!isLocale(propertyVocabulary.getText())) {
            // Assume json property:attribute vocabulary
            this.handleJSONPropertyAttributeVocabulary(srd, propName, propertyVocabulary);
        } else {
            for (CommonTree vocabularyEntry : (List<CommonTree>) propertyVocabulary.getChildren()) {
                if (vocabularyEntry.getType() == ResourcetreeLexer.RANGE) {
                    Object value = this.getRangeList(vocabularyEntry.getChild(0).getText());
                    this.addVocabulary(srd, propName, propertyVocabulary, vocabularyEntry.getText(), value);
                } else {
                    for (CommonTree vocabularyValue : (List<CommonTree>) vocabularyEntry.getChildren()) {
                        this.addVocabulary(srd, propName, propertyVocabulary, vocabularyEntry.getText(),
                                vocabularyValue.getText());
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleJSONPropertyAttributeVocabulary(StructuredResourceDescription srd, String propName,
            CommonTree attributeVocabulary) {
        String attributeName = attributeVocabulary.getText();
        for (CommonTree propertyVocabulary : (List<CommonTree>) attributeVocabulary.getChildren()) {
            for (CommonTree vocabularyEntry : (List<CommonTree>) propertyVocabulary.getChildren()) {
                for (CommonTree vocabularyValue : (List<CommonTree>) vocabularyEntry.getChildren()) {
                    this.addJSONPropertyAttributeVocabulary(srd, propName, propertyVocabulary, attributeName,
                            vocabularyEntry.getText(), vocabularyValue.getText());
                }
            }
        }
    }

    private void addJSONPropertyAttributeVocabulary(StructuredResourceDescription srd, String propName,
            CommonTree propertyVocabulary, String attributeName, String key, Object value) {
        Locale locale = LocaleUtils.toLocale(propertyVocabulary.getText());
        for (PropertyDescription propDesc : srd.getPropertyDescriptions()) {
            if (propDesc instanceof JSONPropertyDescription) {
                JSONPropertyDescription jpd = (JSONPropertyDescription) propDesc;
                if (jpd.getName().equals(propName)) {
                    for (JSONPropertyAttributeDescription jsonPropertyAttribute : jpd.getAttributes()) {
                        if (jsonPropertyAttribute.getName().equals(attributeName)) {
                            jsonPropertyAttribute.addVocabulary(locale, key, value);
                        }
                    }
                }
            }
        }
    }

    private void addVocabulary(StructuredResourceDescription srd, String propName, CommonTree propertyVocabulary,
            String key, Object value) {
        Locale locale = LocaleUtils.toLocale(propertyVocabulary.getText());
        for (PropertyDescription propDesc : srd.getPropertyDescriptions()) {
            if (propDesc.getName().equals(propName)) {
                propDesc.addVocabulary(locale, key, value);
            }
        }
    }

    private boolean isLocale(String text) {
        return "no".equals(text) || "nn".equals(text) || "en".equals(text);
    }

    public List<Integer> getRangeList(String unprocessedList) {
        List<Integer> rangeList = new ArrayList<Integer>();
        if (unprocessedList == null || "".equals(unprocessedList.trim())) {
            return rangeList;
        }
        String[] rangeEntries = unprocessedList.split(",");
        for (String rangeEntry : rangeEntries) {
            if (rangeEntry.contains("..")) {
                Integer start = checkRangeEntry(rangeEntry.substring(0, rangeEntry.indexOf(".")));
                Integer end = checkRangeEntry(rangeEntry
                        .substring(rangeEntry.lastIndexOf(".") + 1, rangeEntry.length()));
                if (start > end) {
                    throw new IllegalArgumentException("Start cannot be greater than end in a range");
                }
                for (Integer i = start; i <= end; i++) {
                    rangeList.add(i);
                }
            } else {
                rangeList.add(checkRangeEntry(rangeEntry));
            }
        }
        return rangeList;
    }

    private Integer checkRangeEntry(String rangeEntry) {
        try {
            return Integer.parseInt(rangeEntry);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Values in a range must be numeric, not: '" + rangeEntry + "'");
        }
    }

}
