/* Copyright (c) 2006, 2007, 2009, University of Oslo, Norway
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
package org.vortikal.repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.PropertyEvaluationContext.Type;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.LatePropertyEvaluator;
import org.vortikal.repository.resourcetype.MixinResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.web.service.RepositoryAssertion;

public class RepositoryResourceHelper {

    private static Log logger = LogFactory.getLog(RepositoryResourceHelper.class);

    private AuthorizationManager authorizationManager;
    private ResourceTypeTree resourceTypeTree;
    
    public ResourceImpl create(Principal principal, ResourceImpl resource, boolean collection, Content content) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Evaluate create: " + resource.getURI());
        }
        PropertyEvaluationContext ctx = PropertyEvaluationContext
                .createResourceContext(resource, collection, principal, content);
        recursiveTreeEvaluation(ctx, this.resourceTypeTree.getRoot());
        lateEvaluation(ctx);
        return ctx.getNewResource();
    }

    public ResourceImpl propertiesChange(ResourceImpl originalResource, Principal principal,
            ResourceImpl suppliedResource, Content content) throws AuthenticationException, AuthorizationException,
            InternalRepositoryException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Evaluate properties change: " + originalResource.getURI());
        }

        PropertyEvaluationContext ctx = PropertyEvaluationContext.propertiesChangeContext(originalResource,
                suppliedResource, principal, content);
        recursiveTreeEvaluation(ctx, this.resourceTypeTree.getRoot());
        lateEvaluation(ctx);
        checkForDeadAndZombieProperties(ctx);
        return ctx.getNewResource();
    }

    public ResourceImpl inheritablePropertiesChange(ResourceImpl originalResource, Principal principal,
            ResourceImpl suppliedResource, Content content, InheritablePropertiesStoreContext storeContext) throws AuthenticationException, AuthorizationException,
            InternalRepositoryException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Evaluate inhertiable properties change: " + originalResource.getURI());
        }

        PropertyEvaluationContext ctx = PropertyEvaluationContext.inheritablePropertiesChangeContext(originalResource,
                suppliedResource, principal, content);
        ctx.setStoreContext(storeContext);
        recursiveTreeEvaluation(ctx, this.resourceTypeTree.getRoot());
        lateEvaluation(ctx);
        checkForDeadAndZombieProperties(ctx);
        return ctx.getNewResource();
    }
    
    public ResourceImpl commentsChange(ResourceImpl originalResource, Principal principal, 
            ResourceImpl suppliedResource, Content content)
            throws AuthenticationException, AuthorizationException, InternalRepositoryException, IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("Evaluate comments change: " + originalResource.getURI());
        }
        PropertyEvaluationContext ctx = PropertyEvaluationContext.commentsChangeContext(originalResource,
                suppliedResource, principal, content);
        recursiveTreeEvaluation(ctx, this.resourceTypeTree.getRoot());
        lateEvaluation(ctx);
        checkForDeadAndZombieProperties(ctx);
        return ctx.getNewResource();
    }

    public ResourceImpl contentModification(ResourceImpl resource, Principal principal, Content content) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Evaluate content modification: " + resource.getURI());
        }
        PropertyEvaluationContext ctx = PropertyEvaluationContext.contentChangeContext(resource, principal, content);
        recursiveTreeEvaluation(ctx, this.resourceTypeTree.getRoot());
        lateEvaluation(ctx);
        checkForDeadAndZombieProperties(ctx);
        return ctx.getNewResource();
    }

    public ResourceImpl nameChange(ResourceImpl original, ResourceImpl resource, Principal principal, Content content)
            throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Evaluate name change: " + resource.getURI());
        }
        PropertyEvaluationContext ctx = PropertyEvaluationContext.nameChangeContext(original, resource, principal,
                content);
        recursiveTreeEvaluation(ctx, this.resourceTypeTree.getRoot());
        lateEvaluation(ctx);
        checkForDeadAndZombieProperties(ctx);
        return ctx.getNewResource();
    }

    public ResourceImpl systemChange(ResourceImpl originalResource, Principal principal, 
            ResourceImpl suppliedResource, Content content, SystemChangeContext systemChangeContext)
            throws AuthenticationException, AuthorizationException, InternalRepositoryException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Evaluate system change: " + originalResource.getURI());
        }
        if (systemChangeContext == null) {
            throw new IllegalArgumentException("System change context cannot be null for system change evaluation");
        }
        
        PropertyEvaluationContext ctx = PropertyEvaluationContext.systemChangeContext(originalResource,
                suppliedResource, principal, content);
        ctx.setStoreContext(systemChangeContext);
        recursiveTreeEvaluation(ctx, this.resourceTypeTree.getRoot());
        lateEvaluation(ctx);
        checkForDeadAndZombieProperties(ctx);
        return ctx.getNewResource();
    }

    /**
     * XXX: This hard coded list must be replaced by standard prop handling
     * methods..
     */
    public PropertySet getFixedCopyProperties(ResourceImpl resource, Principal principal, Path destUri)
            throws CloneNotSupportedException {
        PropertySetImpl fixedProps = new PropertySetImpl();
        fixedProps.setUri(destUri);

        final java.util.Date now = new java.util.Date();

        Property owner = (Property) resource.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.OWNER_PROP_NAME)
                .clone();
        owner.setPrincipalValue(principal);
        fixedProps.addProperty(owner);

        Property creationTime = (Property) resource.getProperty(Namespace.DEFAULT_NAMESPACE,
                PropertyType.CREATIONTIME_PROP_NAME).clone();
        creationTime.setDateValue(now);
        fixedProps.addProperty(creationTime);

        Property lastModified = (Property) resource.getProperty(Namespace.DEFAULT_NAMESPACE,
                PropertyType.LASTMODIFIED_PROP_NAME).clone();
        lastModified.setDateValue(now);
        fixedProps.addProperty(lastModified);

        Property contentLastModified = (Property) resource.getProperty(Namespace.DEFAULT_NAMESPACE,
                PropertyType.CONTENTLASTMODIFIED_PROP_NAME).clone();
        contentLastModified.setDateValue(now);
        fixedProps.addProperty(contentLastModified);

        Property propertiesLastModified = (Property) resource.getProperty(Namespace.DEFAULT_NAMESPACE,
                PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME).clone();
        propertiesLastModified.setDateValue(now);
        fixedProps.addProperty(propertiesLastModified);

        Property createdBy = (Property) resource.getProperty(Namespace.DEFAULT_NAMESPACE,
                PropertyType.CREATEDBY_PROP_NAME).clone();
        createdBy.setPrincipalValue(principal);
        fixedProps.addProperty(createdBy);

        Property modifiedBy = (Property) resource.getProperty(Namespace.DEFAULT_NAMESPACE,
                PropertyType.MODIFIEDBY_PROP_NAME).clone();
        modifiedBy.setPrincipalValue(principal);
        fixedProps.addProperty(modifiedBy);

        Property contentModifiedBy = (Property) resource.getProperty(Namespace.DEFAULT_NAMESPACE,
                PropertyType.CONTENTMODIFIEDBY_PROP_NAME).clone();
        contentModifiedBy.setPrincipalValue(principal);
        fixedProps.addProperty(contentModifiedBy);

        Property propertiesModifiedBy = (Property) resource.getProperty(Namespace.DEFAULT_NAMESPACE,
                PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME).clone();
        propertiesModifiedBy.setPrincipalValue(principal);
        fixedProps.addProperty(propertiesModifiedBy);

        return fixedProps;
    }

    private void checkForDeadAndZombieProperties(PropertyEvaluationContext ctx) {
        Resource newResource = ctx.getNewResource();

        Resource resource = ctx.getSuppliedResource();
        if (resource == null)
            resource = ctx.getOriginalResource();

        for (Property suppliedProp : resource) {
            PropertyTypeDefinition propDef = suppliedProp.getDefinition();

            ResourceTypeDefinition[] rts = resourceTypeTree.getPrimaryResourceTypesForPropDef(propDef);

            if (rts == null) {
                // Dead property, no resource type connected to it.
                newResource.addProperty(suppliedProp);
            } else if (newResource.getProperty(propDef) == null) {

                // If it hasn't been set for the new resource, check if zombie
                boolean zombie = true;
                for (ResourceTypeDefinition definition : rts) {
                    if (this.resourceTypeTree.isContainedType(definition, newResource.getResourceType())) {
                        zombie = false;
                        break;
                    }
                }
                if (zombie) {
                    // Zombie property, preserve
                    newResource.addProperty(suppliedProp);
                }
            }
        }
    }

    private boolean recursiveTreeEvaluation(PropertyEvaluationContext ctx, PrimaryResourceTypeDefinition rt)
            throws IOException {

        // Check resource type assertions
        if (!checkAssertions(rt, ctx)) {
            return false;
        }

        // Set resource type
        ctx.getNewResource().setResourceType(rt.getName());

        // For all prop defs, do evaluation
        PropertyTypeDefinition[] propertyDefinitions = rt.getPropertyTypeDefinitions();
        for (PropertyTypeDefinition def : propertyDefinitions) {
            if (def.getPropertyEvaluator() instanceof LatePropertyEvaluator) {
                ctx.addPropertyTypeDefinitionForLateEvaluation(def);
                continue;
            }

            if (def.isInheritable()) {
                if (ctx.shouldEvaluateInheritableProperty(def)) {
                    System.out.println("--- Evaluating inherited prop: " + def);
                    evaluateManagedProperty(ctx, def);
                } else {
                    // Remove it, to make sure it isn't stored on resource
                    ctx.getNewResource().removeProperty(def);
                }
            } else {
                evaluateManagedProperty(ctx, def);            
            }
        }

        // For all prop defs in mixin types, also do evaluation
        List<MixinResourceTypeDefinition> mixinTypes = this.resourceTypeTree.getMixinTypes(rt);
        for (MixinResourceTypeDefinition mixinDef : mixinTypes) {
            PropertyTypeDefinition[] mixinPropDefs = mixinDef.getPropertyTypeDefinitions();
            for (PropertyTypeDefinition def : mixinPropDefs) {
                if (def.getPropertyEvaluator() instanceof LatePropertyEvaluator) {
                    ctx.addPropertyTypeDefinitionForLateEvaluation(def);
                    continue;
                }
                
                if (def.isInheritable()) {
                    if (ctx.shouldEvaluateInheritableProperty(def)) {
                        logger.debug("Evaluating inherited prop from mixinDef: " + def);
                        evaluateManagedProperty(ctx, def);
                    } else {
                        // Remove it, to make sure it isn't stored on resource
                        ctx.getNewResource().removeProperty(def);
                    }
                } else {
                    evaluateManagedProperty(ctx, def);
                }
            }
        }

        // Trigger child evaluation
        List<PrimaryResourceTypeDefinition> childTypes = this.resourceTypeTree.getResourceTypeDefinitionChildren(rt);

        for (PrimaryResourceTypeDefinition childDef : childTypes) {
            if (recursiveTreeEvaluation(ctx, childDef)) {
                break;
            }
        }
        return true;
    }
    
    private void lateEvaluation(PropertyEvaluationContext ctx) throws IOException {
        for (PropertyTypeDefinition def: ctx.getLateEvalutionPropertyTypeDefinitions()) {
            evaluateManagedProperty(ctx, def);
        }
    }

    private void evaluateManagedProperty(PropertyEvaluationContext ctx, PropertyTypeDefinition propDef)
            throws IOException {

        Property evaluatedProp = doEvaluate(ctx, propDef);
        Resource newResource = ctx.getNewResource();

        if (evaluatedProp == null && propDef.isMandatory()) {
            Value defaultValue = propDef.getDefaultValue();
            if (defaultValue == null) {
                throw new InternalRepositoryException("Property " + propDef + " is mandatory with no default value, "
                        + "and evaluator either did not exist or returned false. " + "Resource " + newResource
                        + " not evaluated (resource type: " + newResource.getResourceType() + ")");
            }
            evaluatedProp = propDef.createProperty();
            evaluatedProp.setValue(defaultValue);
        }

        if (propDef.getValidator() != null && evaluatedProp != null) {
            propDef.getValidator().validate(evaluatedProp, ctx);
        }
        if (evaluatedProp != null) {
            newResource.addProperty(evaluatedProp);
        } else {
            newResource.removeProperty(propDef);
        }
    }

    private Property doEvaluate(PropertyEvaluationContext ctx, PropertyTypeDefinition propDef) throws IOException {

        if (ctx.getEvaluationType() == Type.SystemPropertiesChange) {
            Property originalUnchanged = ctx.getOriginalResource().getProperty(propDef);
            if (! ctx.isSystemChangeAffectedProperty(propDef)) {
                // Not to be affected by system change, return original unchanged.
                return originalUnchanged;
            }
        }
        
        if (ctx.getEvaluationType() == Type.PropertiesChange || 
            ctx.getEvaluationType() == Type.SystemPropertiesChange ||
            ctx.getEvaluationType() == Type.InheritablePropertiesChange) {
            // Check for user change or addition
            Property property = checkForUserAdditionOrChange(ctx, propDef);
            if (property != null) {
                if (propDef.getProtectionLevel() != null) {
                    try {
                        this.authorizationManager.authorizeAction(ctx.getOriginalResource().getURI(), propDef
                                .getProtectionLevel(), ctx.getPrincipal());
                    } catch (AuthorizationException e) {
                        throw new AuthorizationException("Principal " + ctx.getPrincipal()
                                + " not authorized to set property " + property + " (protectionLevel="
                                + propDef.getProtectionLevel() + ") on resource " + ctx.getNewResource(), e);
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Property user-modified or added: " + property + " for resource "
                            + ctx.getNewResource() + ", type " + ctx.getNewResource().getResourceType());
                }

                return property;
            }
            // Check for user deletion
            if (checkForUserDeletion(ctx, propDef)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Property user-deleted: " + propDef + " for resource " + ctx.getNewResource()
                            + ", type " + ctx.getNewResource().getResourceType());
                }

                return null;
            }
        }

        Resource newResource = ctx.getNewResource();

        PropertyEvaluator evaluator = propDef.getPropertyEvaluator();
        Property property = ctx.getOriginalResource().getProperty(propDef);

        /**
         * The evaluator will be given a clone of the original property as input
         * if it previously existed, or an uninitialized property otherwise.
         */
        if (property != null) {
            try {
                property = (Property) property.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalRepositoryException("Error: unable to clone property " + propDef + "on resource '"
                        + newResource.getURI() + "'", e);
            }
        }

        if (evaluator != null) {
            // Initialize prop if necessary
            if (property == null) {
                property = propDef.createProperty();
            }
            boolean evaluated = evaluator.evaluate(property, ctx);
            if (!evaluated) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Property not evaluated: " + propDef + " by evaluator " + evaluator + " for resource "
                            + ctx.getNewResource() + ", type " + ctx.getNewResource().getResourceType());
                }
                return null;
            }
            if (!property.isValueInitialized()) {
                throw new InternalRepositoryException("Evaluator " + evaluator + " on resource '"
                        + newResource.getURI() + "' returned un-initialized value: " + propDef);
            }
        }
        if (property == null && propDef.isMandatory()) {
            Value defaultValue = propDef.getDefaultValue();
            if (defaultValue == null) {
                throw new InternalRepositoryException("Property " + propDef + " is mandatory with no default value, "
                        + "and evaluator either did not exist or returned false. " + "Resource " + newResource
                        + " not evaluated (resource type: " + newResource.getResourceType() + ")");
            }
            property = propDef.createProperty();
            property.setValue(defaultValue);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("[" + ctx.getEvaluationType() + "] evaluated: " + property 
                    + ", evaluator " + evaluator + ", resource "
                    + ctx.getNewResource() + ", type " + ctx.getNewResource().getResourceType());
        }
        return property;
    }

    private boolean checkForUserDeletion(PropertyEvaluationContext ctx, PropertyTypeDefinition propDef)
            throws ConstraintViolationException {
        Property originalProp = ctx.getOriginalResource().getProperty(propDef);
        Property suppliedProp = ctx.getSuppliedResource().getProperty(propDef);

        if (originalProp != null && suppliedProp == null) {
            return true;
        }

        return false;
    }

    private Property checkForUserAdditionOrChange(PropertyEvaluationContext ctx, PropertyTypeDefinition propDef) {
        Property originalProp = ctx.getOriginalResource().getProperty(propDef);
        Property suppliedProp = ctx.getSuppliedResource().getProperty(propDef);
        try {
            // Added
            if (originalProp == null && suppliedProp != null) {
                return (Property) suppliedProp.clone();
            }
            // Changed
            if (originalProp != null && suppliedProp != null && !originalProp.equals(suppliedProp)) {
                return (Property) suppliedProp.clone();
            }
        } catch (CloneNotSupportedException e) {
            throw new InternalRepositoryException("Error: unable to clone property " + suppliedProp + "on resource '"
                    + ctx.getNewResource().getURI() + "'", e);
        }
        return null;
    }

    /**
     * Checking that all resource type assertions match for resource
     */
    private boolean checkAssertions(PrimaryResourceTypeDefinition rt, PropertyEvaluationContext ctx) {

        Resource resource = ctx.getNewResource();
        Principal principal = ctx.getPrincipal();

        RepositoryAssertion[] assertions = rt.getAssertions();

        if (assertions != null) {
            for (int i = 0; i < assertions.length; i++) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Checking assertion " + assertions[i] + " for resource " + resource);
                }

                if (assertions[i] instanceof RepositoryContentEvaluationAssertion) {
                    // XXX Hack for all assertions that implement this interface
                    // (they need content)
                    RepositoryContentEvaluationAssertion cea = (RepositoryContentEvaluationAssertion) assertions[i];

                    if (!cea.matches(resource, principal, ctx.getContent())) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Checking for type '" + rt.getName() + "', resource " + resource
                                    + " failed, unmatched content evaluation assertion: " + cea);
                        }
                        return false;
                    }
                } else {
                    // Normal assertions that should not require content or
                    // resource input stream:
                    if (!assertions[i].matches(resource, principal)) {

                        if (logger.isDebugEnabled()) {
                            logger.debug("Checking for type '" + rt.getName() + "', resource " + resource
                                    + " failed, unmatched assertion: " + assertions[i]);
                        }
                        return false;
                    }
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Checking for type '" + rt.getName() + "', resource " + resource
                    + " succeeded, assertions matched: " + (assertions != null ? Arrays.asList(assertions) : null));
        }
        return true;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

}
