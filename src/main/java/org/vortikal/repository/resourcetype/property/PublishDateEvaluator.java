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
package org.vortikal.repository.resourcetype.property;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationManager;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.PropertyEvaluationContext.Type;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class PublishDateEvaluator implements PropertyEvaluator {

    private PropertyTypeDefinition creationTimePropDef;
    private AuthorizationManager authorizationManager;
    
    private boolean removeValueOnCreate = false;
    
    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx)
            throws PropertyEvaluationException {

        Property creationTimeProp = ctx.getNewResource().getProperty(this.creationTimePropDef);
        if (creationTimeProp == null) {
            throw new PropertyEvaluationException("creationTimePropDef needed for evaluation");
        }
        
        // Whether publishing is authorized, based on resource ACL
        // XXX this check might give false positive for copy with preserve ACL, if principal
        // has full read-write on the source, but only read-write-unpublished on destination PARENT.
        //
        final boolean authorizedToPublish = authorizationManager.authorize(ctx.getPrincipal(),
                                                                           ctx.getNewResource().getAcl(),
                                                                           Privilege.READ_WRITE);
        
        // On Create we do one of:
        // 1. Remove value if configured to do so.
        // 2. Remove value if not authorized to publish.
        // 3. Set value to creation time if authorized to publish (auto-publish).
        if (ctx.getEvaluationType() == Type.Create) {
            if (removeValueOnCreate) {
                return false;
            }
            
            // If not authorized to publish, make new resource unpublished.
            if (!authorizedToPublish) {
                return false;
            }
            
            property.setDateValue(creationTimeProp.getDateValue());
            return true;
        }

        // On NameChange (copy/move), we remove value if not authorized to publish
        if (ctx.getEvaluationType() == Type.NameChange) {
            if (!authorizedToPublish) {
                // If not authorized to publish, make copied/moved resource unpublished.
                return false;
            }
        }
        
        // Logic below when principal is authorized to publish and evaluation type
        // is anything but Create:

        Property existing = ctx.getOriginalResource().getProperty(property.getDefinition());
        if (existing != null && ctx.getEvaluationType() == Type.ContentChange) {
            // If existing published date < creation time, update it:
            if (existing.getDateValue().getTime() < creationTimeProp.getDateValue().getTime()) {
                property.setDateValue(creationTimeProp.getDateValue());
            }
        }

        // Keep existing publish-date prop
        return property.isValueInitialized();
    }

    @Required
    public void setCreationTimePropDef(PropertyTypeDefinition creationTimePropDef) {
        this.creationTimePropDef = creationTimePropDef;
    }

    @Required
    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    public void setRemoveValueOnCreate(boolean removeValueOnCreate) {
        this.removeValueOnCreate = removeValueOnCreate;
    }
}
