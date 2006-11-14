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
package org.vortikal.repositoryimpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.AuthorizationManager;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.repository.resourcetype.MixinResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PropertiesModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyValidator;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repositoryimpl.content.ContentImpl;
import org.vortikal.repositoryimpl.content.ContentRepresentationRegistry;
import org.vortikal.repositoryimpl.dao.ContentStore;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.web.service.RepositoryAssertion;


/**
 * XXX: Legacy props do have problems...
 * XXX: Validate all logic!
 * XXX: catch or declare evaluation and authorization exceptions on a reasonable level
 */
public class RepositoryResourceHelperImpl implements 
    RepositoryResourceHelper, InitializingBean {

    private static Log logger = LogFactory.getLog(RepositoryResourceHelperImpl.class);

    private AuthorizationManager authorizationManager;
    private ResourceTypeTree resourceTypeTree;
    private PropertyManager propertyManager;
    
    // Needed for property-evaluation. Should be a reasonable dependency.
    private ContentStore contentStore;
    private ContentRepresentationRegistry contentRepresentationRegistry;
    
    public ResourceImpl create(Principal principal, String uri, boolean collection) {

        ResourceImpl resource = new ResourceImpl(uri, this.propertyManager, this.authorizationManager);
        PrimaryResourceTypeDefinition rt = create(principal, resource, 
                new Date(), collection, this.resourceTypeTree.getRoot());

        if (logger.isDebugEnabled())
            logger.debug("Found resource type definition: " 
                    + rt + " for resource created at '" + uri + "'");
        
        resource.setResourceType(rt.getName());
        
        if (collection)
            resource.setChildURIs(new String[]{});
        
        return resource;
    }


    private PrimaryResourceTypeDefinition create(Principal principal, 
            ResourceImpl newResource, Date time, boolean isCollection, 
            PrimaryResourceTypeDefinition rt) {

        // Checking if resource type matches
        if (!checkAssertions(rt, newResource, principal)) return null;
        
        List newProps = new ArrayList();

        // Evaluating resource type properties
        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();
        
        evalCreate(principal, newResource, time, isCollection, rt, def, newProps);

        // Evaluating mixin resource type properties
        MixinResourceTypeDefinition[] mixinTypes = this.resourceTypeTree.getMixinTypes(rt);
        
        for (int i = 0; i < mixinTypes.length; i++) {

            PropertyTypeDefinition[] mixinDefs = mixinTypes[i].getPropertyTypeDefinitions();
            evalCreate(principal, newResource, time, isCollection, mixinTypes[i],
                       mixinDefs, newProps);
        }


        for (Iterator iter = newProps.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }


        // Checking child resource types by delegating
        List children = this.resourceTypeTree.getResourceTypeDefinitionChildren(rt);
        for (Iterator iterator = children.iterator(); iterator.hasNext();) {
            PrimaryResourceTypeDefinition child = (PrimaryResourceTypeDefinition) iterator.next();
            PrimaryResourceTypeDefinition resourceType =
                create(principal, newResource, time, isCollection, child);
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }
    
    private void evalCreate(Principal principal, ResourceImpl newResource,
                            Date time, boolean isCollection, ResourceTypeDefinition rt,
                            PropertyTypeDefinition[] definitions, List newProps) {
        for (int i = 0; i < definitions.length; i++) {
            PropertyTypeDefinition propertyDef = definitions[i];
            
            CreatePropertyEvaluator evaluator = 
                propertyDef.getCreateEvaluator();            

            Property prop = this.propertyManager.createProperty(rt.getNamespace(), propertyDef.getName());
            if (evaluator != null && evaluator.create(principal, prop, newResource,
                                                      isCollection, time)) {
                if (logger.isDebugEnabled())
                    logger.debug("Property evaluated for creation [" + rt.getName() + "]: "
                                 + prop + " using evaluator " + evaluator);
                
            }

            if (propertyDef.isMandatory() && !((PropertyImpl) prop).isValueInitialized())
                throw new Error("Property  " + prop + " not initialized");

            if (prop.isValueInitialized())
                newProps.add(prop);
        }
    }
    

    /**
     * Evaluates and validates properties on a resource before
     * storing.
     * 
     * <p>Properties are one of:
     * <ul>
     *   <li>dead
     *   <li>user created/changed
     *   <li>deleted
     *   <li>to be evaluated
     *
     * @param resource a the original resource
     * @param principal the principal performing the store operation
     * @param dto the user-supplied resource
     * @return the resulting resource after property evaluation
     */
    public ResourceImpl storeProperties(ResourceImpl resource, Principal principal,
                                        Resource dto)
        throws AuthenticationException, AuthorizationException,
        CloneNotSupportedException, IOException {

        String uri = resource.getURI();
        
        // For all properties, check if they are modified, deleted or created
        Map alreadySetProperties = new HashMap();
        Map deletedProps = new HashMap();
        List deadProperties = new ArrayList();
        Map toEvaluateProps = new HashMap();
        // Looping over already existing properties
        for (Iterator iter = resource.getProperties().iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            Property userProp = dto.getProperty(prop.getNamespace(), prop.getName());

            if (userProp == null) {
                // Deleted
                if (prop.getDefinition() != null) {
                    if (prop.getDefinition().isMandatory())
                        throw new ConstraintViolationException(
                            "Mandatory property deleted by user: " + prop);
                    // check if allowed
                    this.authorizationManager.tmpAuthorizeForPropStore(prop.getDefinition().getProtectionLevel(), principal, uri);
                    // It will be removed
                    addToPropsMap(deletedProps, prop);
                } else {
                    // Dead - ok
                    if (logger.isDebugEnabled())
                        logger.debug("Property " + prop + " deleted by user "
                                + "(dead property, no definition)");
                }
            } else if (!prop.equals(userProp)) {
                // Changed value
                if (prop.getDefinition() == null) {
                    // Dead
                    if (logger.isDebugEnabled()) 
                        logger.debug("Property " + prop + " changed value "
                                     + "(dead property, no definition)");
                    deadProperties.add(userProp);
                } else {
                    // check if allowed
                    this.authorizationManager.tmpAuthorizeForPropStore(prop.getDefinition().getProtectionLevel(), principal, uri);
                    addToPropsMap(alreadySetProperties, userProp);
                }
            } else if (prop.getDefinition() == null) {
                // Dead and un-changed.
                deadProperties.add(userProp);
            } else
                // Otherwise unchanged - to be evaluated
                addToPropsMap(toEvaluateProps, prop);
                
        }
        
        for (Iterator iter = dto.getProperties().iterator(); iter.hasNext();) {
            Property userProp = (Property) iter.next();
            Property prop = resource.getProperty(userProp.getNamespace(),
                                                 userProp.getName());
            
            if (prop != null)
                continue;

            // Otherwise added
            if (userProp.getDefinition() == null) {
                // Dead
                if (!userProp.isValueInitialized())
                    throw new ConstraintViolationException(
                            "Property " + userProp + " is not initialized");
                if (logger.isDebugEnabled())
                    logger.debug("Property " + prop + " added "
                            + "(dead property, no definition)");
                deadProperties.add(userProp);
            } else {
                // check if allowed
                this.authorizationManager.tmpAuthorizeForPropStore(userProp.getDefinition().getProtectionLevel(), principal, uri);
                // XXX: is value initialized????
                addToPropsMap(alreadySetProperties, userProp);
            }
        }
        
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), this.propertyManager,
                                                    this.authorizationManager);
        newResource.setID(resource.getID());
        newResource.setAcl(resource.getAcl() != null ? (Acl)resource.getAcl().clone() : null);

        if (resource.getLock() != null)
            newResource.setLock((Lock)resource.getLock().clone());
        
        if (logger.isDebugEnabled()) {
            logger.debug("About to evaluate resource type for resource " + dto
                         + ", alreadySetProps = " + alreadySetProperties
                         + ", deletedProps = " + deletedProps
                         + ", deadProps = " + deadProperties
                         + ", toEvaluateProps = " + toEvaluateProps
                         + ", suppliedProps = " + dto.getProperties());
        }

        List evaluatedProps = new ArrayList();
        
        // Evaluate resource tree, for all live props not overridden, evaluate
        ResourceTypeDefinition rt = propertiesModification(principal, 
                                                           newResource, 
                                                           toEvaluateProps, 
                                                           evaluatedProps,
                                                           new Date(), 
                                                           alreadySetProperties,
                                                           deletedProps, 
                                                           this.resourceTypeTree.getRoot());

        newResource.setResourceType(rt.getName());
        
        // Remaining props are legacy props to be kept
        for (Iterator iter = toEvaluateProps.values().iterator(); iter.hasNext();) {
            Map map = (Map)iter.next();
            for (Iterator iterator = map.values().iterator(); iterator.hasNext();) {
                Property prop = (Property) iterator.next();
                if (!evaluatedProps.contains(prop))
                    newResource.addProperty(prop);
            }
        }
        
        for (Iterator iter = deadProperties.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            if (logger.isDebugEnabled()) 
                logger.debug("Adding dead property " + prop + "to resource: " + newResource);
            newResource.addProperty(prop);
        }
        
        for (Iterator iter = alreadySetProperties.values().iterator(); iter.hasNext();) {
            Map map = (Map) iter.next();
            for (Iterator iterator = map.values().iterator(); iterator.hasNext();) {
                Property prop = (Property) iterator.next();

                if (logger.isDebugEnabled())
                    logger.debug("Adding property " + prop + " to resource " + newResource);
                newResource.addProperty(prop);
            }
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Returning evaluated resource: " + newResource
                         + " having properties: " + newResource.getProperties());
        }

        return newResource;
    }
    

    private PrimaryResourceTypeDefinition propertiesModification(
        Principal principal, ResourceImpl newResource, Map toEvaluateProps, 
        List evaluatedProps, Date time, Map alreadySetProperties, Map deletedProps, 
        PrimaryResourceTypeDefinition rt) {

        // Checking if resource type matches
        if (!checkAssertions(rt, newResource, principal)) 
            return null;

        // Evaluating primary resource type properties
        List propertiesToAdd = new ArrayList();
        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();
        propertiesToAdd.addAll(
                evalPropertiesModification(principal, newResource, toEvaluateProps, evaluatedProps, 
                        time, alreadySetProperties, deletedProps, rt, def));
        
        // Evaluating mixin resource type properties
        MixinResourceTypeDefinition[] mixinTypes =
            this.resourceTypeTree.getMixinTypes(rt);
        

        for (int i = 0; i < mixinTypes.length; i++) {
            PropertyTypeDefinition[] mixinDef = mixinTypes[i].getPropertyTypeDefinitions();
            propertiesToAdd.addAll(evalPropertiesModification(principal, 
                                                              newResource, 
                                                              toEvaluateProps, 
                                                              evaluatedProps,
                                                              time,
                                                              alreadySetProperties,
                                                              deletedProps, 
                                                              mixinTypes[i],
                                                              mixinDef));
        }

        // Check validator...
//        for (Iterator iter = propertiesToAdd.iterator(); iter.hasNext();) {
//            Property prop = (Property) iter.next();
//            PropertyValidator validator = prop.getDefinition().getValidator();
//            if (validator != null) {
//                validator.validate(principal, newResource, prop);                
//            }
//        }
        
        for (Iterator iter = propertiesToAdd.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }

        // Checking child resource types by delegating
        List children = this.resourceTypeTree.getResourceTypeDefinitionChildren(rt);
        for (Iterator iterator = children.iterator(); iterator.hasNext();) {
            PrimaryResourceTypeDefinition child = (PrimaryResourceTypeDefinition) iterator.next();
            PrimaryResourceTypeDefinition resourceType = 
                propertiesModification(principal, 
                                       newResource,
                                       toEvaluateProps,
                                       evaluatedProps,
                                       time,
                                       alreadySetProperties,
                                       deletedProps,
                                       child);
            if (resourceType != null)
                return resourceType;
        }

        return rt;
    }
    

    private List evalPropertiesModification(
        Principal principal, ResourceImpl newResource, Map toEvaluateProps, List evaluatedProps,
        Date time, Map alreadySetProperties, Map deletedProps, ResourceTypeDefinition rt,
        PropertyTypeDefinition[] propertyDefs) {

        List newProps = new ArrayList();
        
        if (logger.isDebugEnabled())
            logger.debug("Evaluating properties modification for resource "
                         + newResource + ", resource type " + rt);

        for (int i = 0; i < propertyDefs.length; i++) {
            if (logger.isDebugEnabled())
                logger.debug("Evaluating properties modification for resource "
                             + newResource + ", resource type " + rt
                             + ", property " + propertyDefs[i]);

            PropertyTypeDefinition propertyDef = propertyDefs[i];
            PropertyValidator validator = propertyDef.getValidator();

            // If property already set, don't evaluate
            Map propsMap = (Map) alreadySetProperties.get(rt.getNamespace());
            if (propsMap != null) {
                Property p = (Property) propsMap.get(propertyDef.getName());
                if (p != null) {
                    if (logger.isDebugEnabled())
                        logger.debug("Property " + p 
                                + " already set, will not evaluate");
                    
                    // Validate 
                    if (validator != null) 
                        validator.validate(principal, newResource, p);
                    
                    newProps.add(p);
                    propsMap.remove(propertyDef.getName());
                    continue;
                }
            }
            // If prop deleted, don't evaluate
            propsMap = (Map) deletedProps.get(rt.getNamespace());
            if (propsMap != null) {
                Property p = (Property) propsMap.get(propertyDef.getName());
                if (p != null) {
                    if (logger.isDebugEnabled())
                        logger.debug("Property " + p + " was deleted, will not evaluate");
                    continue;
                }
            }

            // Not user edited, evaluate
            if (logger.isDebugEnabled())
                logger.debug("Property " + propertyDef + " not user edited, evaluating");

            Property prop = null;
            propsMap = (Map) toEvaluateProps.get(rt.getNamespace());
            if (propsMap != null) {
                prop = (Property) propsMap.get(propertyDef.getName());
                evaluatedProps.add(prop);
            }
            PropertiesModificationPropertyEvaluator evaluator =
                propertyDef.getPropertiesModificationEvaluator();

            boolean addProperty = false;

            if (evaluator != null) {
                if (prop == null) 
                    prop = this.propertyManager.createProperty(rt.getNamespace(), propertyDef.getName());

                if (logger.isDebugEnabled())
                    logger.debug("Created property " + prop + ", will evaluate using "
                                 + evaluator);
                
                if (evaluator.propertiesModification(principal, prop, newResource, time)) {
                    addProperty = true;
                    if (logger.isDebugEnabled())
                        logger.debug("Property evaluated for properties modification ["
                                     + rt.getName() + "]: " + prop
                                     + " using evaluator " + evaluator);
                } else if (propertyDef.isMandatory()) {
                    Value defaultValue = propertyDef.getDefaultValue();
                    if (defaultValue == null)
                        throw new Error("Property " + propertyDef + "is " +
                                "mandatory, but evaluator returned false");
                    addProperty = true;
                    prop.setValue(defaultValue);

                }
                
                // Validate
                if (validator != null) {
                    validator.validate(principal, newResource, prop);
                }
                
            } else if (prop != null) {
                if (logger.isDebugEnabled()) 
                    logger.debug("No properties modification evaluator for property " + prop
                                 + ", but it already existed on resource");
                addProperty = true;
            } else if (logger.isDebugEnabled())
                logger.debug("No properties modification evaluator for property "
                        + propertyDef + ", and it did not already exist, not adding");
                

            if (!addProperty && propertyDef.isMandatory())
                throw new ConstraintViolationException(
                    "Property defined by " + propertyDef
                    + " is mandatory for resource type " + rt);
            
            if (addProperty)
                newProps.add(prop);
        }
        
        return newProps;
    }
    



    public ResourceImpl collectionContentModification(ResourceImpl resource, 
            Principal principal) {
        
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), this.propertyManager, this.authorizationManager);
        newResource.setID(resource.getID());
        newResource.setAcl(resource.getAcl());
        newResource.setLock(resource.getLock());
        ResourceTypeDefinition rt = contentModification(principal, newResource, 
                resource, null, new Date(), this.resourceTypeTree.getRoot());
        
        newResource.setResourceType(rt.getName());
        for (Iterator i = resource.getProperties().iterator(); i.hasNext();) {
            Property prop = (Property) i.next();

            // Preserve dead properties:
            if (prop.getDefinition() == null) {
                newResource.addProperty(prop);
            }
        }
        
        return newResource;
    }


    public ResourceImpl fileContentModification(ResourceImpl resource,
                                                            Principal principal) {
        
        // XXX: What to do about swapping old resource with new?
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), this.propertyManager, this.authorizationManager);
        newResource.setID(resource.getID());
        newResource.setAcl(resource.getAcl());
        newResource.setLock(resource.getLock());
        ResourceTypeDefinition rt = contentModification(
            principal, newResource, resource,
            new ContentImpl(resource.getURI(), this.contentStore,
                            this.contentRepresentationRegistry),
            new Date(), this.resourceTypeTree.getRoot());
        
        if (logger.isDebugEnabled())
            logger.debug("Setting new resource type: '" + rt.getName()
                         + "' on resource " + resource);

        newResource.setResourceType(rt.getName());
        for (Iterator i = resource.getProperties().iterator(); i.hasNext();) {
            Property prop = (Property) i.next();

            // Preserve dead properties:
            if (prop.getDefinition() == null) {
                newResource.addProperty(prop);
            }
        }
        
        return newResource;
        
    }
    
    
    

    private ResourceTypeDefinition contentModification(
        Principal principal, ResourceImpl newResource, Resource original,
        Content content, Date time, PrimaryResourceTypeDefinition rt) {

        // Checking if resource type matches
        if (!checkAssertions(rt, newResource, principal)) return null;

        List newProps = new ArrayList();

        // Evaluating primary resource type properties
        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();

        evalContentModification(principal, newResource, original, content,
                                time, rt, def, newProps);
        
        // Evaluating mixin resource type properties
        MixinResourceTypeDefinition[] mixinTypes =
            this.resourceTypeTree.getMixinTypes(rt);
        for (int i = 0; i < mixinTypes.length; i++) {

            PropertyTypeDefinition[] mixinDef = mixinTypes[i].getPropertyTypeDefinitions();

            evalContentModification(principal, newResource, original, content,
                                    time, mixinTypes[i], mixinDef, newProps);
        }


        for (Iterator iter = newProps.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }

        // Checking child resource types by delegating
        List children = this.resourceTypeTree.getResourceTypeDefinitionChildren(rt);
        for (Iterator iterator = children.iterator(); iterator.hasNext();) {
            PrimaryResourceTypeDefinition child = (PrimaryResourceTypeDefinition) iterator.next();

            ResourceTypeDefinition resourceType = 
                contentModification(principal, newResource,
                                    original, content, time, child); // Recursive call
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }

    
    private void evalContentModification(Principal principal, ResourceImpl newResource,
                                         Resource original, Content content, Date time,
                                         ResourceTypeDefinition rt,
                                         PropertyTypeDefinition[] definitions, List newProps) {

        for (int i = 0; i < definitions.length; i++) {
            PropertyTypeDefinition propertyDef = definitions[i];
            
            Property prop = original.getProperty(rt.getNamespace(), propertyDef.getName());
            ContentModificationPropertyEvaluator evaluator =
                propertyDef.getContentModificationEvaluator();

            if (evaluator != null) {
                if (prop == null) 
                    prop = this.propertyManager.createProperty(rt.getNamespace(), propertyDef.getName());

                if (evaluator.contentModification(principal, prop, newResource, content, time)) {
                    if (logger.isDebugEnabled())
                        logger.debug("Property evaluated for content modification ["
                                     + rt.getName() + "]: " + prop + " using evaluator "
                                     + evaluator);
                    newProps.add(prop);
                } 
            } else if (prop != null) 
                newProps.add(prop);
        }
    }


    public PropertySet getFixedCopyProperties(Resource resource, Principal principal, String destUri)
        throws CloneNotSupportedException {
        PropertySetImpl fixedProps = new PropertySetImpl(destUri);
        java.util.Date now = new java.util.Date();

        Property owner = (Property) resource.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.OWNER_PROP_NAME).clone();
        owner.setPrincipalValue(principal);
        fixedProps.addProperty(owner);
            
        Property creationTime = (Property) resource.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CREATIONTIME_PROP_NAME).clone();
        creationTime.setDateValue(now);
        fixedProps.addProperty(creationTime);

        Property lastModified = (Property) resource.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.LASTMODIFIED_PROP_NAME).clone();
        lastModified.setDateValue(now);
        fixedProps.addProperty(lastModified);

        Property contentLastModified = (Property) resource.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLASTMODIFIED_PROP_NAME).clone();
        contentLastModified.setDateValue(now);
        fixedProps.addProperty(contentLastModified);

        Property propertiesLastModified = (Property) resource.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME).clone();
        propertiesLastModified.setDateValue(now);
        fixedProps.addProperty(propertiesLastModified);

        Property createdBy = (Property) resource.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CREATEDBY_PROP_NAME).clone();
        createdBy.setPrincipalValue(principal);
        fixedProps.addProperty(createdBy);

        Property modifiedBy = (Property) resource.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.MODIFIEDBY_PROP_NAME).clone();
        modifiedBy.setPrincipalValue(principal);
        fixedProps.addProperty(modifiedBy);

        Property contentModifiedBy = (Property) resource.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTMODIFIEDBY_PROP_NAME).clone();
        contentModifiedBy.setPrincipalValue(principal);
        fixedProps.addProperty(contentModifiedBy);

        Property propertiesModifiedBy = (Property) resource.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME).clone();
        propertiesModifiedBy.setPrincipalValue(principal);
        fixedProps.addProperty(propertiesModifiedBy);

        return fixedProps;
    }


    private boolean checkAssertions(PrimaryResourceTypeDefinition rt,
                                    Resource resource, Principal principal) {

        RepositoryAssertion[] assertions = rt.getAssertions();

        if (assertions != null) {
            for (int i = 0; i < assertions.length; i++) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Checking assertion "
                                 + assertions[i] + " for resource " + resource);
                }

                if (!assertions[i].matches(resource, principal)) {
                    
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                            "Checking for type '" + rt.getName() + "', resource " + resource
                            + " failed, unmatched assertion: " + assertions[i]);
                    }
                    return false;
                }
                
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Checking for type '" + rt.getName() + "', resource "
                         + resource + " succeeded, assertions matched: "
                         + (assertions != null ? Arrays.asList(assertions) : null));
        }
        return true;
    }

    private void addToPropsMap(Map parent, Property property) {
        Map map = (Map) parent.get(property.getNamespace());
        if (map == null) {
            map = new HashMap();
            parent.put(property.getNamespace(), map);
        }
        map.put(property.getName(), property);

    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.propertyManager == null) {
            throw new BeanInitializationException("Property 'propertyManager' not set.");
        } 
        if (this.authorizationManager == null) {
            throw new BeanInitializationException("Property 'authorizationManager' not set.");
        }
        if (this.contentStore == null) {
            throw new BeanInitializationException("Property 'contentStore' not set.");
        }
        if (this.contentRepresentationRegistry == null) {
            throw new BeanInitializationException("Property 'contentRepresentationRegistry' not set.");
        }
        if (this.resourceTypeTree == null) {
            throw new BeanInitializationException("Property 'resourceTypeTree' not set.");
        }
        
    }

    
    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    public void setContentRepresentationRegistry(
        ContentRepresentationRegistry contentRepresentationRegistry) {
        this.contentRepresentationRegistry = contentRepresentationRegistry;
    }
    
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    public ResourceTypeTree getResourceTypeTree() {
        return this.resourceTypeTree;
    }

}
