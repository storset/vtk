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
package org.vortikal.repository.resourcetype.property;

import java.util.Calendar;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.PropertyEvaluationContext.Type;
import org.vortikal.repository.resourcetype.OverridingPropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class PublishEvaluator implements PropertyEvaluator {

    private PropertyTypeDefinition publishedPropDef;
    private PropertyTypeDefinition publishDatePropDef;

    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

        Property creationTimeProp = ctx.getNewResource().getProperty(Namespace.DEFAULT_NAMESPACE,
                PropertyType.CREATIONTIME_PROP_NAME);

        if (ctx.getEvaluationType() == Type.Create) {

            if (isOverridden()) {
                ctx.getNewResource().removeProperty(this.publishDatePropDef);
            } else {
                if (creationTimeProp == null) {
                    throw new PropertyEvaluationException("creationTimePropDef needed for evaluation");
                }
                Property publishDateProp = ctx.getNewResource().createProperty(this.publishDatePropDef);
                publishDateProp.setDateValue(creationTimeProp.getDateValue());
            }

            return true;
        }

        if (ctx.getEvaluationType() == Type.NameChange || ctx.getEvaluationType() == Type.ContentChange
                || ctx.getEvaluationType() == Type.PropertiesChange) {

            if (isOverridden()) {
                Property published = ctx.getNewResource().getProperty(this.publishedPropDef);
                if (published.getBooleanValue() == false) {
                    ctx.getNewResource().removeProperty(this.publishDatePropDef);
                }
            } else {
                Property publishDateProp = ctx.getOriginalResource().getProperty(this.publishDatePropDef);
                if (publishDateProp == null) {
                    publishDateProp = ctx.getNewResource().createProperty(this.publishDatePropDef);
                    publishDateProp.setDateValue(creationTimeProp.getDateValue());
                }
            }

            return true;

        }

        if (ctx.getEvaluationType() == Type.PublishChange) {

            Property published = ctx.getOriginalResource().getProperty(this.publishedPropDef);

            Property publishDateProp = ctx.getOriginalResource().getProperty(this.publishDatePropDef);

            if (published == null || published.getBooleanValue() == false) {
                property.setBooleanValue(true);
                if (publishDateProp == null) {
                    publishDateProp = ctx.getNewResource().createProperty(this.publishDatePropDef);
                }
                publishDateProp.setDateValue(Calendar.getInstance().getTime());
            } else {
                property.setBooleanValue(false);
                if (publishDateProp != null) {
                    ctx.getNewResource().removeProperty(publishDateProp.getDefinition());
                }
            }

            return true;
        }

        return false;
    }

    /**
     * @return true if the evaluator is configured for the overridden published
     *         property, i.e. if the default value is false (currently all
     *         structured resource)
     */
    private boolean isOverridden() {
        return (this.publishedPropDef instanceof OverridingPropertyTypeDefinitionImpl);
    }

    public void setPublishedPropDef(PropertyTypeDefinition publishedPropDef) {
        this.publishedPropDef = publishedPropDef;
    }

    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

}