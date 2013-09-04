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
package org.vortikal.repository.resourcetype.property;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.AuthorizationManager;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyValidator;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;

/**
 * An evaluator and validator for the resource owner property. 
 *
 * <p>Further restrictions, such as associating certain privileges to
 * the operation of taking ownership should be configured as a
 * protection level in the property definition.
 */
public class OwnerEvaluator 
    implements PropertyEvaluator, PropertyValidator, InitializingBean {

    private PrincipalManager principalManager;
    private AuthorizationManager authorizationManager;
    
    
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }
    
    public void afterPropertiesSet() {
        if (this.principalManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'principalManager' not set");
        }
        if (this.authorizationManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'authorizationManager' not set");
        }
    }


    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {
        if (ctx.getEvaluationType() == PropertyEvaluationContext.Type.Create) {
            property.setPrincipalValue(ctx.getPrincipal());
        }
        return true;
    }


    public void validate(Property property, PropertyEvaluationContext ctx)
        throws ConstraintViolationException {
        
        Principal owner = property.getPrincipalValue();
        if (owner == null) {
           throw new ConstraintViolationException(
                   "Unable to set owner of resource to NULL");
        }
        if (!this.principalManager.validatePrincipal(owner)) {
            // Keep existing, invalid owner principals, disallow new ones:
            if (ctx.getEvaluationType() == PropertyEvaluationContext.Type.Create) {
                throw new ConstraintViolationException(
                    "Unable to set owner of resource to invalid value: '" 
                    + owner + "'");
            }
        }
        
        // Require root to change owner to any other value than current user
        if (ctx.getEvaluationType() == PropertyEvaluationContext.Type.PropertiesChange) {
            Property existingProp = ctx.getOriginalResource().getProperty(property.getDefinition());
            Principal existing = existingProp.getPrincipalValue();
        
            if ((!existing.equals(owner)) && (!owner.equals(ctx.getPrincipal()))) {
                try {
                    this.authorizationManager.authorizeRootRoleAction(ctx.getPrincipal());
                    // Principal is root, allow any value:
                    return;
                } catch (AuthorizationException e) {
                    throw new ConstraintViolationException(
                           "Must be root to set other value than current user, new value: '" 
                           + owner + "'" + " existing: '" + existing + "' by user: '" + ctx.getPrincipal() + "'");                    
                }
            }
               
        } 
    }    
}
    
