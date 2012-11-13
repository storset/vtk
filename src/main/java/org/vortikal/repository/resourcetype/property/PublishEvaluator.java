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
import java.util.Date;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.AuthorizationManager;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.PropertyEvaluationContext.Type;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class PublishEvaluator implements PropertyEvaluator {

    private PropertyTypeDefinition publishDatePropDef;
    private PropertyTypeDefinition unpublishDatePropDef;

    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

        Property publishDateProp = ctx.getNewResource().getProperty(this.publishDatePropDef);
        Property unpublishDateProp = ctx.getNewResource().getProperty(this.unpublishDatePropDef);
        final Date now = Calendar.getInstance().getTime();
        final Date publishDate = publishDateProp != null ? publishDateProp.getDateValue() : null;
        final Date unpublishDate = unpublishDateProp != null ? unpublishDateProp.getDateValue() : null;

        property.setBooleanValue(false);
        
        if (publishDate != null) {
            if (publishDate.before(now)) {
                if (unpublishDate == null || unpublishDate.after(now)) {
                    property.setBooleanValue(true);
                }
            }
        }
        
        return true;

// Old logic temporarily kept for reference:
//        
//        if (publishDateProp != null) {
//            Date publishDate = publishDateProp.getDateValue();
//            if (unpublishDateProp != null) {
//                Date unpublishDate = unpublishDateProp.getDateValue();
//                if (publishDate.before(now) && unpublishDate.before(publishDate)) {
//                    property.setBooleanValue(true);
//                    
//                    // XXX: deleting a property that is not evaluated by this evaluator:
//                    ctx.getNewResource().removeProperty(this.unpublishDatePropDef);
//                    
//                    return true;
//                } else if (unpublishDate.before(now)) {
//                    property.setBooleanValue(false);
//                    return true;
//                }
//            }
//            if (publishDate.before(now)) {
//                property.setBooleanValue(true);
//            } else {
//                property.setBooleanValue(false);
//            }
//            return true;
//        }
//        
//        property.setBooleanValue(false);
//        
//        ctx.getNewResource().removeProperty(this.unpublishDatePropDef);
//        
//        return true;
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
