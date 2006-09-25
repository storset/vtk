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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.repositoryimpl.content.ContentImpl;
import org.vortikal.repositoryimpl.content.ContentRepresentationRegistry;
import org.vortikal.repositoryimpl.dao.ContentStore;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.web.service.RepositoryAssertion;


/**
 * XXX: Legacy props do have problems...
 * XXX: Validate all logic!
 * XXX: catch or declare evaluation and authorization exceptions on a reasonable level
 */
public class PropertyManagerImpl implements PropertyManager, 
    RepositoryPropertyHelper, InitializingBean, ApplicationContextAware {

    private static Log logger = LogFactory.getLog(PropertyManagerImpl.class);

    private RoleManager roleManager;
    private PrincipalManager principalManager;
    private AuthorizationManager authorizationManager;
    private ContentRepresentationRegistry contentRepresentationRegistry;
    private ValueFactory valueFactory;

    // Needed for property-evaluation. Should be a reasonable dependency.
    private ContentStore contentStore;
    
    private PrimaryResourceTypeDefinition rootResourceTypeDefinition;
    
    /* Currently maps a parent resource type def. to its children (arrays)
     * XXX: Resource type definitions that have no children are _not_
     *      represented as keys in this map. Beware if using key set iteration.
     */
    private Map resourceTypeDefinitions = new HashMap();
    
    /**
     * Maps resource type names to resource type objects
     */
    private Map resourceTypeNameMap = new HashMap();

    // Currently maps namespaceUris to maps which map property names to defs.
    private Map propertyTypeDefinitions = new HashMap();
    
    private Collection mixins;

    private Map mixinTypeDefinitionMap = new HashMap();

    private Map namespaceUriMap = new HashMap();
    
    private Map namespacePrefixMap = new HashMap();

    private ApplicationContext applicationContext;
    
    private PrimaryResourceTypeDefinition[] getResourceTypeDefinitionChildrenInternal(
        PrimaryResourceTypeDefinition rt) {

        PrimaryResourceTypeDefinition[] children =
            (PrimaryResourceTypeDefinition[])this.resourceTypeDefinitions.get(rt);
        if (children == null)
            return new PrimaryResourceTypeDefinition[0];
        return children;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.roleManager == null) {
            throw new BeanInitializationException("Property 'roleManager' not set.");
        } 
        if (this.principalManager == null) {
            throw new BeanInitializationException("Property 'principalManager' not set.");
        }
        if (this.authorizationManager == null) {
            throw new BeanInitializationException("Property 'authorizationManager' not set.");
        }
        if (this.valueFactory == null) {
            throw new BeanInitializationException("Property 'valueFactory' not set.");
        }
        if (this.contentStore == null) {
            throw new BeanInitializationException("Property 'contentStore' not set.");
        }
        if (this.contentRepresentationRegistry == null) {
            throw new BeanInitializationException("Property 'contentRepresentationRegistry' not set.");
        }

        init();
    }

    private  void init() {
        Collection resourceTypeDefinitionBeans = 
            BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, 
                    PrimaryResourceTypeDefinition.class, false, false).values();

        PrimaryResourceTypeDefinition rootDefinition = null;
        for (Iterator i = resourceTypeDefinitionBeans.iterator(); i.hasNext();) {
            PrimaryResourceTypeDefinition def = (PrimaryResourceTypeDefinition)i.next();
            if (def.getParentTypeDefinition() == null) {
                if (rootDefinition != null) {
                    throw new IllegalStateException(
                        "Only one PrimaryResourceTypeDefinition having "
                        + "parentTypeDefinition = null may be defined");
                }
                rootDefinition = def;
            }
        }
        if (rootDefinition == null) {
                    throw new IllegalStateException(
                        "A PrimaryResourceTypeDefinition having "
                        + "parentTypeDefinition = null must be defined");
        }
        this.rootResourceTypeDefinition = rootDefinition;
        
        for (Iterator i = resourceTypeDefinitionBeans.iterator(); i.hasNext();) {
            PrimaryResourceTypeDefinition def = (PrimaryResourceTypeDefinition)i.next();
            
            this.resourceTypeNameMap.put(def.getName(), def);
            if (def.getNamespace() == null) {
                throw new BeanInitializationException(
                    "Definition's namespace is null: " + def
                    + " (already initialized resourceTypes = " + this.resourceTypeNameMap + ")");
            }

            addNamespacesAndProperties(def);
            
            // Populate map of resourceTypeDefiniton parent -> children
            PrimaryResourceTypeDefinition parent = def.getParentTypeDefinition();
            PrimaryResourceTypeDefinition[] children = 
                    (PrimaryResourceTypeDefinition[]) this.resourceTypeDefinitions.get(parent);

            // Array append (or create if not exists for given parent)
            PrimaryResourceTypeDefinition[] newChildren = null;

            if (children == null) {
                newChildren = new PrimaryResourceTypeDefinition[1];
                newChildren[0] = def;
            } else {
                newChildren = new PrimaryResourceTypeDefinition[children.length + 1];
                System.arraycopy(children, 0, newChildren, 0, children.length);
                newChildren[newChildren.length - 1] = def;
            }

            this.resourceTypeDefinitions.put(parent, newChildren);
            this.mixinTypeDefinitionMap.put(def, getMixinTypes(def));

        }
        // Remove null-key (which is the root resource type's "parent")
        this.resourceTypeDefinitions.remove(null);
        
        this.mixins = 
            BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, 
                    MixinResourceTypeDefinition.class, false, false).values();

        for (Iterator iter = this.mixins.iterator(); iter.hasNext();) {
            ResourceTypeDefinition def = (ResourceTypeDefinition) iter.next();
            this.resourceTypeNameMap.put(def.getName(), def);
            addNamespacesAndProperties(def);
        }

        logger.info("Resource type tree: \n" + getResourceTypeTreeAsString());
    }
    
    public ResourceImpl create(Principal principal, String uri, boolean collection) {

        ResourceImpl resource = new ResourceImpl(uri, this, this.authorizationManager);
        PrimaryResourceTypeDefinition rt = create(principal, resource, 
                new Date(), collection, this.rootResourceTypeDefinition);

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
        MixinResourceTypeDefinition[] mixinTypes =
            (MixinResourceTypeDefinition[]) this.mixinTypeDefinitionMap.get(rt);
        
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
        PrimaryResourceTypeDefinition[] children = getResourceTypeDefinitionChildrenInternal(rt);
        
        if (children == null) return rt;
        
        for (int i = 0; i < children.length; i++) {
            PrimaryResourceTypeDefinition resourceType =
                create(principal, newResource, time, isCollection, children[i]);
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

            Property prop = createProperty(rt.getNamespace(), propertyDef.getName());
            if (evaluator != null && evaluator.create(principal, prop, newResource,
                                                      isCollection, time)) {
                if (logger.isDebugEnabled())
                    logger.debug("Property evaluated [" + rt.getName() + "]: " + prop);
                
            }

            if (propertyDef.isMandatory() && !((PropertyImpl) prop).isValueInitialized())
                throw new Error("Property  " + prop + " not initialized");

            if (prop.isValueInitialized())
                newProps.add(prop);
        }
    }
    

    private void addToPropsMap(Map parent, Property property) {
        Map map = (Map) parent.get(property.getNamespace());
        if (map == null) {
            map = new HashMap();
            parent.put(property.getNamespace(), map);
        }
        map.put(property.getName(), property);

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
                    authorize(prop.getDefinition().getProtectionLevel(), principal, uri);
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
                    authorize(prop.getDefinition().getProtectionLevel(), principal, uri);
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
                authorize(userProp.getDefinition().getProtectionLevel(), principal, uri);
                // XXX: is value initialized????
                addToPropsMap(alreadySetProperties, userProp);
            }
        }
        
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), this,
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
                                                           this.rootResourceTypeDefinition);

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
            (MixinResourceTypeDefinition[]) this.mixinTypeDefinitionMap.get(rt);
        

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
        PrimaryResourceTypeDefinition[] children = getResourceTypeDefinitionChildrenInternal(rt);
        for (int i = 0; i < children.length; i++) {
            PrimaryResourceTypeDefinition resourceType = 
                propertiesModification(principal, 
                                       newResource,
                                       toEvaluateProps,
                                       evaluatedProps,
                                       time,
                                       alreadySetProperties,
                                       deletedProps,
                                       children[i]);
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
                    prop = createProperty(rt.getNamespace(), propertyDef.getName());

                if (logger.isDebugEnabled())
                    logger.debug("Created property " + prop + ", will evaluate using "
                                 + evaluator);
                
                if (evaluator.propertiesModification(principal, prop, newResource, time)) {
                    addProperty = true;
                    if (logger.isDebugEnabled())
                        logger.debug("Property evaluated [" + rt.getName() + "]: " + prop);
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
        
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), this, this.authorizationManager);
        newResource.setID(resource.getID());
        newResource.setAcl(resource.getAcl());
        newResource.setLock(resource.getLock());
        ResourceTypeDefinition rt = contentModification(principal, newResource, 
                resource, null, new Date(), this.rootResourceTypeDefinition);
        
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
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), this, this.authorizationManager);
        newResource.setID(resource.getID());
        newResource.setAcl(resource.getAcl());
        newResource.setLock(resource.getLock());
        ResourceTypeDefinition rt = contentModification(
            principal, newResource, resource,
            new ContentImpl(resource.getURI(), this.contentStore,
                            this.contentRepresentationRegistry),
            new Date(), this.rootResourceTypeDefinition);
        
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
            (MixinResourceTypeDefinition[]) this.mixinTypeDefinitionMap.get(rt);
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
        PrimaryResourceTypeDefinition[] children = getResourceTypeDefinitionChildrenInternal(rt);
        for (int i = 0; i < children.length; i++) {
            ResourceTypeDefinition resourceType = 
                contentModification(principal, newResource,
                                    original, content, time, children[i]); // Recursive call
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
                    prop = createProperty(rt.getNamespace(), propertyDef.getName());

                if (evaluator.contentModification(principal, prop, newResource, content, time)) {
                    if (logger.isDebugEnabled())
                        logger.debug("Property evaluated [" + rt.getName() + "]: " + prop);
                    newProps.add(prop);
                } 
            } else if (prop != null) 
                newProps.add(prop);
        }
    }


    public Property createProperty(Namespace namespace, String name) {

        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespace);
        prop.setName(name);
        
        PropertyTypeDefinition def = findPropertyTypeDefinition(namespace, name);
        prop.setDefinition(def);
        
        if (def != null && def.getDefaultValue() != null) {
            if (logger.isDebugEnabled())
                logger.debug("Setting default value of prop " + prop + " to "
                             + def.getDefaultValue());

            prop.setValue(def.getDefaultValue());
        }

        return prop;
    }


    public Property createProperty(Namespace namespace, String name, Object value) 
        throws ValueFormatException {

        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespace);
        prop.setName(name);
        
        // Set definition (may be null)
        prop.setDefinition(findPropertyTypeDefinition(namespace, name));
        
        if (value instanceof Date) {
            Date date = (Date) value;
            prop.setDateValue(date);
        } else if (value instanceof Boolean) {
            Boolean bool = (Boolean) value;
            prop.setBooleanValue(bool.booleanValue());
        } else if (value instanceof Long) {
            Long l = (Long) value;
            prop.setLongValue(l.longValue());
        } else if (value instanceof Integer) {
            Integer i = (Integer)value;
            prop.setIntValue(i.intValue());
        } else if (value instanceof Principal) {
            Principal p = (Principal) value;
            prop.setPrincipalValue(p);
        } else {
            if (! (value instanceof String)) {
                throw new ValueFormatException(
                    "Supplied value of property [namespaces: "
                    + namespace + ", name: " + name
                    + "] not of any supported type " 
                    + "(type was: " + value.getClass() + ")");
            }
            prop.setStringValue((String) value);
        } 
        
        return prop;
    }
    

    public Property createProperty(String namespaceUrl, String name, 
                                   String[] stringValues, int type) 
        throws ValueFormatException {
        
        Namespace namespace = (Namespace) this.namespaceUriMap.get(namespaceUrl);
        
        if (namespace == null) 
            namespace = new Namespace(namespaceUrl);
        
        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespace);
        prop.setName(name);
        
        PropertyTypeDefinition def = findPropertyTypeDefinition(namespace, name);
        prop.setDefinition(def);
        
        if (def != null && def.isMultiple()) {
            Value[] values = this.valueFactory.createValues(stringValues, type);
            prop.setValues(values);

            if (logger.isDebugEnabled())
                logger.debug("Created multi-value property: " + prop);
        } else {
            // Not multi-value, stringValues must be of length 1, otherwise there are
            // inconsistency problems between data store and config.
            if (stringValues.length > 1) {
                logger.error("Cannot convert multiple values to a single-value prop"
                             + " for property " + prop);
                throw new ValueFormatException(
                    "Cannot convert multiple values: " + Arrays.asList(stringValues)
                    + " to a single-value property"
                    + " for property " + prop);
            }
            if (def == null)
                // Dead, ensure value is interpreted as string:
                type = PropertyType.TYPE_STRING;
            
            Value value = this.valueFactory.createValue(stringValues[0], type);
            prop.setValue(value);
        }
        
        return prop;
        
    }
    


    public ResourceImpl getDestinationCopyResource(ResourceImpl dest, Principal principal)
        throws CloneNotSupportedException {

        java.util.Date now = new java.util.Date();
        ResourceImpl copy = (ResourceImpl) dest.clone();
        Property lastModified = copy.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.LASTMODIFIED_PROP_NAME);
        lastModified.setDateValue(now);
        Property contentLastModified = copy.getProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLASTMODIFIED_PROP_NAME);
        contentLastModified.setDateValue(now);

        return copy;
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


    private PropertyTypeDefinition findPropertyTypeDefinition(Namespace namespace, 
                                                          String name) {
        PropertyTypeDefinition propDef = null;
        Map map = (Map) this.propertyTypeDefinitions.get(namespace);

        if (map != null) {
            propDef = (PropertyTypeDefinition) map.get(name);
        }
        
        if (logger.isDebugEnabled() && propDef == null) 
            logger.debug("No definition found for property " +
                    namespace.getPrefix() + ":" + name);
        
        return propDef;
    }
    

    // TODO: print mixin types as well
    public String getResourceTypeTreeAsString() {
        StringBuffer sb = new StringBuffer();
        printResourceTypes(sb, 0, this.rootResourceTypeDefinition);
        printMixinTypes(sb);
        return sb.toString();
    }
    
    private void printMixinTypes(StringBuffer sb) {

        sb.append("\n");
        for (Iterator i = this.mixins.iterator(); i.hasNext();) {
            MixinResourceTypeDefinition mixin = (MixinResourceTypeDefinition) i.next();
            printResourceTypes(sb, 0, mixin);
            sb.append("\n");
        }
    }
    

    private void printResourceTypes(StringBuffer sb, int level,
                                    ResourceTypeDefinition def) {
        
        if (level > 0) {
            for (int i = 1; i < level; i++) sb.append("  ");
            sb.append("|\n");
            for (int i = 1; i < level; i++) sb.append("  ");
            sb.append("+--");
        }

        sb.append("[").append(def.getNamespace()).append("] ").append(def.getName());
        if (def instanceof MixinResourceTypeDefinition) {
            sb.append(" (mixin)");
        }
        sb.append("\n");

        MixinResourceTypeDefinition[] mixins = (MixinResourceTypeDefinition[])
            this.mixinTypeDefinitionMap.get(def);
        if (mixins != null) {
            for (int i = 0; i < mixins.length; i++) {
                for (int j = 0; j < level; j++) sb.append("  ");
                sb.append("  mixin: [");
                sb.append(mixins[i].getNamespace()).append("] ");
                sb.append(mixins[i].getName()).append("\n");
            }
        }

        PropertyTypeDefinition[] definitions = def.getPropertyTypeDefinitions();
        if (definitions.length > 0) {
            for (int i = 0; i < definitions.length; i++) {
                sb.append("  ");
                for (int j = 0; j < level; j++) sb.append("  ");
                String type = PropertyType.PROPERTY_TYPE_NAMES[definitions[i].getType()];
                sb.append(type);
                if (definitions[i].isMultiple()) sb.append("[]");
                sb.append(" ").append(definitions[i].getName());
                sb.append("\n");
            }
        }
        PrimaryResourceTypeDefinition[] children = (PrimaryResourceTypeDefinition[])
            this.resourceTypeDefinitions.get(def);
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                printResourceTypes(sb, level + 1, children[i]);
            }
        }
    }


    private void addNamespacesAndProperties(ResourceTypeDefinition def) {
        // Populate map of property type definitions
        PropertyTypeDefinition[] definitions = def.getPropertyTypeDefinitions();
        Namespace namespace = def.getNamespace();

        if (!this.namespaceUriMap.containsKey(def.getNamespace().getUri())) {
            logger.info("Adding namespace URI mapping: " + def.getNamespace().getUri()
                        + " -> " + def.getNamespace());
            this.namespaceUriMap.put(def.getNamespace().getUri(), def.getNamespace());
        }        

        if (!this.namespacePrefixMap.containsKey(def.getNamespace().getPrefix())) {            
            logger.info("Adding namespace prefix mapping: " + def.getNamespace().getPrefix()
                        + " -> " + def.getNamespace());
            this.namespacePrefixMap.put(def.getNamespace().getPrefix(), def.getNamespace());
        }
        
        Map propDefMap = (Map)this.propertyTypeDefinitions.get(namespace);
        
        if (propDefMap == null) {
            propDefMap = new HashMap();
            // XXX: what about prefix when using namespaces as map keys?
            this.propertyTypeDefinitions.put(namespace, propDefMap);
        }
        for (int u = 0; u < definitions.length; u++) {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering property type definition "
                             + definitions[u].getName());
            }

            propDefMap.put(definitions[u].getName(), definitions[u]);
        }
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





    private void authorize(RepositoryAction action, Principal principal, String uri) 
        throws AuthenticationException, AuthorizationException, 
        ResourceLockedException, IOException{

        if (RepositoryAction.WRITE.equals(action)) {
            this.authorizationManager.authorizeWrite(uri, principal);
        } else if (RepositoryAction.WRITE_ACL.equals(action)) {
            this.authorizationManager.authorizeWriteAcl(uri, principal);
        } else if (RepositoryAction.REPOSITORY_ADMIN_ROLE_ACTION.equals(action)) {
            this.authorizationManager.authorizePropertyEditAdminRole(uri, principal);
        } else if (RepositoryAction.REPOSITORY_ROOT_ROLE_ACTION.equals(action)) {
            this.authorizationManager.authorizePropertyEditRootRole(uri, principal);
        } else {
            throw new AuthorizationException(
                "Principal " + principal + " not authorized to perform "
                + " action " + action + " on resource " + uri);
        }
    }
    
    private MixinResourceTypeDefinition[] getMixinTypes(ResourceTypeDefinition rt) {
        List mixinTypes = new ArrayList();
        MixinResourceTypeDefinition[] directMixins = rt.getMixinTypeDefinitions();
        if (directMixins != null) {
            for (int i = 0; i < directMixins.length; i++) {
                MixinResourceTypeDefinition mix = directMixins[i];
                mixinTypes.add(mix);
                if (!this.namespaceUriMap.containsKey(mix.getNamespace().getUri()))
                    this.namespaceUriMap.put(mix.getNamespace().getUri(), mix.getNamespace());                    

                MixinResourceTypeDefinition[] indirectMixins =
                    directMixins[i].getMixinTypeDefinitions();
                if (indirectMixins != null && indirectMixins.length > 0) {
                    mixinTypes.addAll(Arrays.asList(getMixinTypes(indirectMixins[i])));
                }
            }
        }        
        return (MixinResourceTypeDefinition[]) mixinTypes.toArray(
            new MixinResourceTypeDefinition[mixinTypes.size()]);
    }
    

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

//     public void setRootResourceTypeDefinition(
//             PrimaryResourceTypeDefinition rootResourceTypeDefinition) {
//         this.rootResourceTypeDefinition = rootResourceTypeDefinition;
//     }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
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
    
    public PropertyTypeDefinition getPropertyDefinitionByPrefix(String prefix, String name) {
        Namespace namespace = (Namespace) this.namespacePrefixMap.get(prefix);
        if (namespace == null) {
            return null;
        }
        return findPropertyTypeDefinition(namespace, name);

    }

    /**
     * XXX: equivalent methods for resource-types, mixin-types, etc ?
     * @return Return flat list of all registered property type definitions.
     */
    public List getPropertyTypeDefinitions() {
        ArrayList definitions = new ArrayList();
        
        for (Iterator i = this.propertyTypeDefinitions.values().iterator(); i.hasNext();) {
            Map propMap = (Map)i.next();
            definitions.addAll(propMap.values());
        }
        
        return definitions;
    }
    
    /**
     * Return flat list of all registered <code>PrimaryResourceTypeDefinition</code> objects.
     * @return list of all registered <code>PrimaryResourceTypeDefinition</code> objects.
     */
    public List getPrimaryResourceTypeDefinitions() {
        return new ArrayList(
            BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, 
                    PrimaryResourceTypeDefinition.class, false, false).values());
    }
    
    /**
     * Return a <code>List</code> of the immediate children of the given resource type.
     * @param def
     * @return
     */
    public List getResourceTypeDefinitionChildren(PrimaryResourceTypeDefinition def) {
        PrimaryResourceTypeDefinition[] children = 
            getResourceTypeDefinitionChildrenInternal(def);
        
        ArrayList childList = new ArrayList();
        if (children != null) {
            for (int i=0; i<children.length; i++) {
                childList.add(children[i]);
            }
        }
        
        return childList;
    }
    
    /**
     * Search upwards in resource type tree, collect property type definitions
     * from all encountered resource type definitions including mixin resource types.
     * Assuming that mixin types can never have other mixin types attached.
     * 
     * If there are more than one occurence of the same property type definition
     * for the given resource type, only the first occurence in the resource type
     * tree is added to the returned list (upward direction).
     * 
     * @param def The <code>ResourceTypeDefinition</code> 
     * @return A <code>Set</code> of <code>PropertyTypeDefinition</code> instances.
     */
    public List getPropertyTypeDefinitionsForResourceTypeIncludingAncestors(
                                                    ResourceTypeDefinition def) {
        Set encounteredIds = new HashSet();
        List propertyTypes = new ArrayList();
        
        if (def instanceof MixinResourceTypeDefinition) {
            MixinResourceTypeDefinition mixinDef = (MixinResourceTypeDefinition)def;
            
            PropertyTypeDefinition[] propDefs = mixinDef.getPropertyTypeDefinitions();
            addPropertyTypeDefinitions(encounteredIds, propertyTypes, propDefs);
        } else {
            // Assuming instanceof PrimaryResourceTypeDefinition
            PrimaryResourceTypeDefinition primaryDef = (PrimaryResourceTypeDefinition)def; 

            while (primaryDef != null) {
                PropertyTypeDefinition[] propDefs = primaryDef.getPropertyTypeDefinitions();
                addPropertyTypeDefinitions(encounteredIds, propertyTypes, propDefs);
                
                // Add any mixin resource types' property type defs
                MixinResourceTypeDefinition[] mixinDefs = primaryDef.getMixinTypeDefinitions();
                for (int i=0; i<mixinDefs.length; i++) {
                    addPropertyTypeDefinitions(encounteredIds, propertyTypes, mixinDefs[i].getPropertyTypeDefinitions());
                }

                primaryDef = primaryDef.getParentTypeDefinition();
            }
        }
        
        return propertyTypes;
    }
    
    private void addPropertyTypeDefinitions(Set encounteredIds, 
                                            List propertyTypes, 
                                            PropertyTypeDefinition[] propDefs) {
        for (int i = 0; i < propDefs.length; i++) {
            String id = propDefs[i].getNamespace().getUri() + ":" + propDefs[i].getName();
            // Add only _first_ occurence of property type definition
            if (encounteredIds.add(id)) {
                propertyTypes.add(propDefs[i]);
            }
        }
    }
    
    public ResourceTypeDefinition getResourceTypeDefinitionByName(String name) {
        ResourceTypeDefinition type = (ResourceTypeDefinition)
            this.resourceTypeNameMap.get(name);
        if (type == null) {
            throw new IllegalArgumentException(
                "No resource type of name '" + name + "' exists");
        }

        return type;
    }
    

    public boolean isContainedType(ResourceTypeDefinition def, String resourceTypeName) {


        ResourceTypeDefinition type = (ResourceTypeDefinition)
            this.resourceTypeNameMap.get(resourceTypeName);
        if (!(type instanceof PrimaryResourceTypeDefinition)) {
            throw new IllegalArgumentException("Supplied argument '" + resourceTypeName
                                               + "'not a primary resource type");
        }

        if (type == null) {
            return false;
        }

        ResourceTypeDefinition parent = type;
        while (parent != null) {
            if (def instanceof MixinResourceTypeDefinition) {
                MixinResourceTypeDefinition[] mixins = parent.getMixinTypeDefinitions();
                for (int i = 0; i < mixins.length; i++) {
                    if (mixins[i].equals(def)) {
                        return true;
                    }
                }
            } else if (parent.equals(def)) {
                return true;
            }
            parent = ((PrimaryResourceTypeDefinition) parent).getParentTypeDefinition();
        }
        return false;
    }
    

}
