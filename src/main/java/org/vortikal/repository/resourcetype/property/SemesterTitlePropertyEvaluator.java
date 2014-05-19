/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.resourcetype.property;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.MessageSource;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class SemesterTitlePropertyEvaluator implements PropertyEvaluator {

    private static final Log logger = LogFactory.getLog(SemesterTitlePropertyEvaluator.class);

    private PropertyTypeDefinition semesterTermPropDef;
    private PropertyTypeDefinition semesterYearPropDef;
    private PropertyTypeDefinition semesterTermNumberPropDef;
    private Locale defaultLocale;
    private MessageSource messageSource;

    @Required
    public void setSemesterTermPropDef(PropertyTypeDefinition semesterTermPropDef) {
        this.semesterTermPropDef = semesterTermPropDef;
    }

    @Required
    public void setSemesterYearPropDef(PropertyTypeDefinition semesterYearPropDef) {
        this.semesterYearPropDef = semesterYearPropDef;
    }

    @Required
    public void setSemesterTermNumberPropDef(PropertyTypeDefinition semesterTermNumberPropDef) {
        this.semesterTermNumberPropDef = semesterTermNumberPropDef;
    }

    @Required
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {
        if (semesterTermPropDef == null || semesterYearPropDef == null || semesterTermNumberPropDef == null) {
            logger.warn("semesterTermPropDef, semesterYearPropDef or semesterTermNumberPropDef is null.");
            return false;
        }

        Property year = ctx.getSuppliedResource().getProperty(semesterYearPropDef);
        Property term = ctx.getSuppliedResource().getProperty(semesterTermPropDef);
        Property termNumberProp = ctx.getSuppliedResource().getProperty(semesterTermNumberPropDef);
        Locale locale = ctx.getSuppliedResource().getContentLocale();
        if (locale == null) {
            locale = defaultLocale;
        }

        if (term == null || year == null) {
            logger.warn("Property term or Property year is null.");
            return false;
        }

        String termNumber = "";
        if (termNumberProp != null) {
            termNumber = " - " + messageSource.getMessage("semester.termNumber", null, "Term", locale) + " "
                    + termNumberProp.getIntValue();
        }

        property.setStringValue(term.getFormattedValue("localized", locale) + " " + year.getFormattedValue()
                + termNumber);
        return true;
    }

    @Required
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

}
