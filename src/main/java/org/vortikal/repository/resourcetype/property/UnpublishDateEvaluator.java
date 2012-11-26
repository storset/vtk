/* Copyright (c) 2012, University of Oslo, Norway
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

import java.util.Date;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.AuthorizationManager;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.PropertyEvaluationContext.Type;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

/**
 * Makes sure value of unpublish-date is sane.
 */
public class UnpublishDateEvaluator implements PropertyEvaluator {

    private PropertyTypeDefinition publishDatePropDef;
    private AuthorizationManager authorizationManager;
    
    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

        // Unpublish-date never set on create
        if (ctx.getEvaluationType() == Type.Create) {
            return false;
        }
        
        // If no value is set, then we stop here.
        if (!property.isValueInitialized()) {
            return false;
        }
        
        // If publish-date does not exist, then unpublish-date makes no sense.
        Property publishDateProp = ctx.getNewResource().getProperty(this.publishDatePropDef);
        if (publishDateProp == null) {
            return false;
        }
        
        // At this point, we authorize PUBLISH_UNPUBLISH
        // If not allowed, then we do not change the value.
        try {
            authorizationManager.authorizePublishUnpublish(ctx.getNewResource().getURI(), ctx.getPrincipal());
        } catch (Exception e) {
            return property.isValueInitialized();
        }

        // If unpublish-date is before publish-date, then we remove unpublish-date
        Date publishDate = publishDateProp.getDateValue();
        Date unpublishDate = property.getDateValue();
        if (unpublishDate.before(publishDate)) {
            return false;
        }
        
        // Keep set value
        return true;
    }

    @Required
    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

    @Required
    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

}
