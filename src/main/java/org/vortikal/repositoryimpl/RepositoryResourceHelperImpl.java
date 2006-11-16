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
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.AuthorizationManager;
import org.vortikal.repository.InternalRepositoryException;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
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
import org.vortikal.repository.Acl;


/**
 */
public class RepositoryResourceHelperImpl 
    implements RepositoryResourceHelper, InitializingBean {

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
     *   <li>zombie
     *
     * @param originalResource - the original resource
     * @param principal - the principal performing the store operation
     * @param newResource - the user-supplied resource
     * @return the resulting resource after property evaluation
     */
    public ResourceImpl storeProperties(ResourceImpl originalResource, Principal principal,
                                        Resource newResource)
        throws AuthenticationException, AuthorizationException,
        InternalRepositoryException, IOException {

        String uri = originalResource.getURI();
        
        // For all properties, check if they are modified, deleted or created

        // 1. Deleted property - if it exists in original, but not in new resource
        //    - If dead, it will disappear
        //    - If mandatory or principal not authorized, throw exception
        //    - Otherwise mark as deleted prop
        // 2. Changed prop value - if the values in original and new resource differs
        //    - If dead, mark as dead prop
        //    - If principal not authorized, throw exception
        //    - Otherwise mark as allready set (not to be evaluated)
        // 3. Unchanged value
        //    - If dead, mark as dead
        //    - Otherwise mark as to be evaluated
        // 4. Added property
        //    - If dead, mark as dead
        //    - If principal not authorized, throw exception
        //    - Otherwise mark as allready set (not to be evaluated)
        
        Map alreadySetProperties = new HashMap();
        Map deletedProps = new HashMap();
        List deadProperties = new ArrayList();
        Map toEvaluateProps = new HashMap();

        // Looping over already original resource properties

        for (Iterator iter = originalResource.getProperties().iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            Property userProp = newResource.getProperty(prop.getNamespace(), prop.getName());

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
                } 
                // Otherwise dead - ok
            } else if (!prop.equals(userProp)) {
                // Changed value
                if (prop.getDefinition() == null) {
                    // Dead
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
        
        // Looping over new resource properties - to find added props
        
        for (Iterator iter = newResource.getProperties().iterator(); iter.hasNext();) {
            Property userProp = (Property) iter.next();
            Property prop = originalResource.getProperty(userProp.getNamespace(),
                                                 userProp.getName());
            
            if (prop != null)
                continue;

            // Otherwise added
            if (!userProp.isValueInitialized())
                throw new ConstraintViolationException(
                        "Property " + userProp + " is not initialized");

            if (userProp.getDefinition() == null) {
                // Dead
                deadProperties.add(userProp);
            } else {
                // check if allowed
                this.authorizationManager.tmpAuthorizeForPropStore(userProp.getDefinition().getProtectionLevel(), principal, uri);
                addToPropsMap(alreadySetProperties, userProp);
            }
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("About to evaluate resource type for resource " + newResource
                         + ", alreadySetProps = " + alreadySetProperties
                         + ", deletedProps = " + deletedProps
                         + ", deadProps = " + deadProperties
                         + ", toEvaluateProps = " + toEvaluateProps);
        }

        List evaluatedProps = new ArrayList();
        ResourceImpl evaluatedResource = newResource(originalResource);
        
        // Evaluate resource tree, for all live props not overridden, evaluate
        propertiesModification(principal, 
                evaluatedResource, 
                toEvaluateProps, 
                evaluatedProps,
                new Date(), 
                alreadySetProperties,
                deletedProps, 
                this.resourceTypeTree.getRoot());

        
        // Remaining props are legacy props to be kept
        for (Iterator iter = toEvaluateProps.values().iterator(); iter.hasNext();) {
            Map map = (Map)iter.next();
            for (Iterator iterator = map.values().iterator(); iterator.hasNext();) {
                Property prop = (Property) iterator.next();
                if (!evaluatedProps.contains(prop))
                    evaluatedResource.addProperty(prop);
            }
        }
        
        for (Iterator iter = deadProperties.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            if (logger.isDebugEnabled()) 
                logger.debug("Adding dead property " + prop + "to resource: " + evaluatedResource);
            evaluatedResource.addProperty(prop);
        }
        
        for (Iterator iter = alreadySetProperties.values().iterator(); iter.hasNext();) {
            Map map = (Map) iter.next();
            for (Iterator iterator = map.values().iterator(); iterator.hasNext();) {
                Property prop = (Property) iterator.next();

                if (logger.isDebugEnabled())
                    logger.debug("Adding property " + prop + " to resource " + evaluatedResource);
                evaluatedResource.addProperty(prop);
            }
        }
       
        // Check if resource has become a more specific or different branch of resource, requiring content evaluation
        if (!originalResource.isOfType(evaluatedResource.getResourceTypeDefinition())) {
            evaluatedResource = doContentReevaluation(evaluatedResource, principal);
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Returning evaluated resource: " + evaluatedResource
                         + " having properties: " + evaluatedResource.getProperties());
        }

        return evaluatedResource;
    }
    
    /**
     * Wrapper for doing content evaluation during properties modification event.
     * Maps out properties which MUST NOT be content evaluated.
     * 
     * This is an issue for "content evaluating" properties which are not based
     * entirely on the content. We do not have any mechanisms to detect these,
     * so currently the known list of top level properties is mapped out.
     * 
     * Beware when modelling properties :)
     */
    private ResourceImpl doContentReevaluation(ResourceImpl resource, Principal principal) {
        
        ResourceImpl newResource = contentModification(resource, principal);
        
        // Properties which cannot be contentEvaluated on a properties modification
        Set specialProps = PropertyType.NOT_REPRODUCABLE_CONTENT_PROPERTIES_SET;
        
        for (Iterator iterator = specialProps.iterator(); iterator.hasNext();) {
            String name = (String) iterator.next();
            newResource.addProperty(resource.getProperty(Namespace.DEFAULT_NAMESPACE, name));
        }
        
        return newResource;
    } 
    
    private boolean propertiesModification(
        Principal principal, ResourceImpl newResource, Map toEvaluateProps, 
        List evaluatedProps, Date time, Map alreadySetProperties, Map deletedProps, 
        PrimaryResourceTypeDefinition rt) {

        // Checking if resource type matches
        if (!checkAssertions(rt, newResource, principal)) 
            return false;

        newResource.setResourceType(rt.getName());

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
            propertiesToAdd.addAll(
                    evalPropertiesModification(principal, 
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

            boolean match = 
                propertiesModification(principal, 
                                       newResource,
                                       toEvaluateProps,
                                       evaluatedProps,
                                       time,
                                       alreadySetProperties,
                                       deletedProps,
                                       child);
            if (match)
                break;
        }

        return true;
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
    
    public ResourceImpl contentModification(ResourceImpl resource,
            Principal principal) {
        
        ResourceImpl newResource = newResource(resource);
        ContentImpl content = null;
        
        if (!resource.isCollection())
            content = new ContentImpl(resource.getURI(), this.contentStore,
                    this.contentRepresentationRegistry);

        ResourceTypeDefinition rt = contentModification(principal, newResource,
                resource, content, new Date(), this.resourceTypeTree.getRoot());
        
        if (logger.isDebugEnabled())
            logger.debug("Setting new resource type: '" + rt.getName()
                         + "' on resource " + resource);

        newResource.setResourceType(rt.getName());

        preserveDeadAndZombieProperties(resource, newResource);
        return newResource;
        
    }
    
    /**
     * XXX: This hard coded list must be replaced by standard prop handling methods..
     * @see org.vortikal.repositoryimpl.RepositoryResourceHelper#getFixedCopyProperties(org.vortikal.repository.Resource, org.vortikal.security.Principal, java.lang.String)
     */
    public PropertySet getFixedCopyProperties(Resource resource,
            Principal principal, String destUri)
            throws CloneNotSupportedException {
        PropertySetImpl fixedProps = new PropertySetImpl(destUri);
        java.util.Date now = new java.util.Date();

        Property owner = (Property) resource.getProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.OWNER_PROP_NAME)
                .clone();
        owner.setPrincipalValue(principal);
        fixedProps.addProperty(owner);

        Property creationTime = (Property) resource.getProperty(
                Namespace.DEFAULT_NAMESPACE,
                PropertyType.CREATIONTIME_PROP_NAME).clone();
        creationTime.setDateValue(now);
        fixedProps.addProperty(creationTime);

        Property lastModified = (Property) resource.getProperty(
                Namespace.DEFAULT_NAMESPACE,
                PropertyType.LASTMODIFIED_PROP_NAME).clone();
        lastModified.setDateValue(now);
        fixedProps.addProperty(lastModified);

        Property contentLastModified = (Property) resource.getProperty(
                Namespace.DEFAULT_NAMESPACE,
                PropertyType.CONTENTLASTMODIFIED_PROP_NAME).clone();
        contentLastModified.setDateValue(now);
        fixedProps.addProperty(contentLastModified);

        Property propertiesLastModified = (Property) resource.getProperty(
                Namespace.DEFAULT_NAMESPACE,
                PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME).clone();
        propertiesLastModified.setDateValue(now);
        fixedProps.addProperty(propertiesLastModified);

        Property createdBy = (Property) resource.getProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CREATEDBY_PROP_NAME)
                .clone();
        createdBy.setPrincipalValue(principal);
        fixedProps.addProperty(createdBy);

        Property modifiedBy = (Property) resource.getProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.MODIFIEDBY_PROP_NAME)
                .clone();
        modifiedBy.setPrincipalValue(principal);
        fixedProps.addProperty(modifiedBy);

        Property contentModifiedBy = (Property) resource.getProperty(
                Namespace.DEFAULT_NAMESPACE,
                PropertyType.CONTENTMODIFIEDBY_PROP_NAME).clone();
        contentModifiedBy.setPrincipalValue(principal);
        fixedProps.addProperty(contentModifiedBy);

        Property propertiesModifiedBy = (Property) resource.getProperty(
                Namespace.DEFAULT_NAMESPACE,
                PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME).clone();
        propertiesModifiedBy.setPrincipalValue(principal);
        fixedProps.addProperty(propertiesModifiedBy);

        return fixedProps;
    }

    private ResourceImpl newResource(ResourceImpl resource) {
        try {
            return resource.cloneWithoutProperties();
        } catch (CloneNotSupportedException e) {
            throw new InternalRepositoryException(
                    "Unable to clone resource '" + resource.getURI() + "'", e);
        }
    }

    private void preserveDeadAndZombieProperties(ResourceImpl resource, ResourceImpl newResource) {
        for (Iterator i = resource.getProperties().iterator(); i.hasNext();) {
            Property prop = (Property) i.next();

            if (prop.getDefinition() == null) {
                // Preserve dead properties
                newResource.addProperty(prop);
            } else if (newResource.getProperty(prop.getDefinition()) == null) {
                // Preserve zombie properties
                newResource.addProperty(prop);
            }
        }
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



    /** 
     * Checking that all resource type assertions match for resource 
     */
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

    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }


    
    
    public Resource evaluateChange(ResourceImpl originalResource, ResourceImpl suppliedResource,
                                   Principal principal, boolean isContentChange, Date time) throws IOException {

        EvaluationContext ctx = new EvaluationContext(originalResource, suppliedResource, principal);
        recursiveTreeEvaluation(ctx, isContentChange, this.resourceTypeTree.getRoot(), time);
        
        // Check dead and zombie
        ResourceImpl newResource = ctx.getNewResource();
        for (Iterator i = suppliedResource.getProperties().iterator(); i.hasNext();) {
            Property suppliedProp = (Property) i.next();
            Property newProp = newResource.getProperty(suppliedProp.getNamespace(), suppliedProp.getName());
            if (newProp == null) {
                if (suppliedProp.getDefinition() == null) {
                    // Dead property, preserve
                    newResource.addProperty(suppliedProp);
                } else if (newProp.getDefinition() != null) {
                    // Check zombie prop:
                }
            }
        }
        return ctx.getNewResource();
    }
    
    private boolean recursiveTreeEvaluation(EvaluationContext ctx, boolean isContentChange,
                                            PrimaryResourceTypeDefinition rt, Date time) throws IOException {

        // Check resource type assertions
        if (checkAssertions(rt, ctx.getNewResource(), ctx.getPrincipal())) {
            return false;
        }
        
        // Set resource type
        ctx.getNewResource().setResourceType(rt.getName());
        
        // For all prop defs, do evaluation
        PropertyTypeDefinition[] propertyDefinitions = rt.getPropertyTypeDefinitions();
        for (int i = 0; i < propertyDefinitions.length; i++) {
            PropertyTypeDefinition def = propertyDefinitions[i];
            evaluateManagedProperty(ctx, isContentChange, def, time);
        }

        // For all prop defs in mixin types, also do evaluation
        MixinResourceTypeDefinition[] mixinTypes = this.resourceTypeTree.getMixinTypes(rt);
        for (int i = 0; i < propertyDefinitions.length; i++) {
            MixinResourceTypeDefinition mixinDef = mixinTypes[i];
            PropertyTypeDefinition[] mixinPropDefs = mixinDef.getPropertyTypeDefinitions();
            for (int j = 0; j < mixinPropDefs.length; j++) {
                evaluateManagedProperty(ctx, isContentChange, mixinPropDefs[j], time);
            }
        }

        // Trigger child evaluation
        List childTypes = this.resourceTypeTree.getResourceTypeDefinitionChildren(rt);
        for (Iterator i = childTypes.iterator(); i.hasNext();) {
            PrimaryResourceTypeDefinition childDef = (PrimaryResourceTypeDefinition) i.next();
            if (!recursiveTreeEvaluation(ctx, isContentChange, childDef, time)) {
                continue;
            }
        }
        return true;
    }

    


    private void evaluateManagedProperty(EvaluationContext ctx, boolean isContentChange,
                                         PropertyTypeDefinition propDef, Date time) throws IOException {
        // For all properties, check if they are modified, deleted or created

        // 1. Deleted property - if it exists in original, but not in new resource
        //    - If dead, it will disappear
        //    - If mandatory or principal not authorized, throw exception
        //    - Otherwise mark as deleted prop
        // 2. Changed prop value - if the values in original and new resource differs
        //    - If dead, mark as dead prop
        //    - If principal not authorized, throw exception
        //    - Otherwise mark as allready set (not to be evaluated)
        // 3. Unchanged value
        //    - If dead, mark as dead
        //    - Otherwise mark as to be evaluated
        // 4. Added property
        //    - If dead, mark as dead
        //    - If principal not authorized, throw exception
        //    - Otherwise mark as allready set (not to be evaluated)


        ResourceImpl newResource = ctx.getNewResource();
        Property suppliedProp = ctx.getSuppliedResource().getProperty(propDef);
        Property originalProp = ctx.getOriginalResource().getProperty(propDef);

        Property evaluatedProp = getEvaluatedProperty(ctx, isContentChange, propDef, time);
        if (evaluatedProp != null) {
            // Set any (content or property modification) evaluated properties first:
            newResource.addProperty(evaluatedProp);
        }

        boolean includeProp = false;

        // XXX: this is as far as I got, the stuff below is NOT sane:

        if (originalProp != null && suppliedProp == null) {
            // Deleted property, check if mandatory:
            if (propDef.isMandatory()) {
                throw new ConstraintViolationException("Property defined by " + propDef
                                                       + " is mandatory for resource "
                                                       + newResource);
            }
            this.authorizationManager.tmpAuthorizeForPropStore(
                propDef.getProtectionLevel(), ctx.getPrincipal(), newResource.getURI());
            includeProp = false;

        } else if (originalProp == null && suppliedProp == null) {
            // Never set
            includeProp = false;

        } else if (originalProp == null && suppliedProp != null) {
            // Added
            this.authorizationManager.tmpAuthorizeForPropStore(
                propDef.getProtectionLevel(), ctx.getPrincipal(), newResource.getURI());
            includeProp = true;

        } else if (originalProp != null && suppliedProp != null && !originalProp.equals(suppliedProp)) {
            // Changed prop value
            this.authorizationManager.tmpAuthorizeForPropStore(
                propDef.getProtectionLevel(), ctx.getPrincipal(), newResource.getURI());
            includeProp = true;
        }

        if (includeProp) {
            newResource.addProperty(suppliedProp);
        }
    }
    


    private Property getEvaluatedProperty(EvaluationContext ctx, boolean isContentChange,
                                          PropertyTypeDefinition propDef, Date time) throws IOException {
        Property evaluatedProp = null;
        Resource suppliedResource = ctx.getSuppliedResource();
        if (isContentChange) {
            Content content = null;
            ContentModificationPropertyEvaluator evaluator =
                propDef.getContentModificationEvaluator();
            if (evaluator != null) {
                if (!ctx.getOriginalResource().isCollection()) {
                    content = new ContentImpl(suppliedResource.getURI(), this.contentStore,
                                              this.contentRepresentationRegistry);
                }
                evaluatedProp = this.propertyManager.createProperty(
                    suppliedResource.getResourceTypeDefinition().getNamespace(), propDef.getName());
            
                boolean evaluated =
                    evaluator.contentModification(ctx.getPrincipal(), evaluatedProp,
                                                  suppliedResource, content, time);
                if (!evaluated) {
                    evaluatedProp = null;
                }
            }
        } else {
            PropertiesModificationPropertyEvaluator evaluator = 
                propDef.getPropertiesModificationEvaluator();

            if (evaluator != null) {
                evaluatedProp = this.propertyManager.createProperty(
                    suppliedResource.getResourceTypeDefinition().getNamespace(), propDef.getName());
                boolean evaluated =
                    evaluator.propertiesModification(ctx.getPrincipal(), evaluatedProp,
                                                     suppliedResource, time);
                if (!evaluated) {
                    evaluatedProp = null;
                }
            }
        }
        return evaluatedProp;
    }

    
    private class EvaluationContext {
        private Resource originalResource;
        private Resource suppliedResource; 
        private Principal principal;
        private ResourceImpl newResource;

        public EvaluationContext(ResourceImpl originalResource,
                  ResourceImpl suppliedResource, Principal principal) throws InternalRepositoryException {
                
            this.originalResource = originalResource;
            this.suppliedResource = suppliedResource;
            this.principal = principal;

            // Create empty new resource: XXX: how do we create empty resources?
            try {
                newResource = originalResource.cloneWithoutProperties();
            } catch (CloneNotSupportedException e) {
                throw new InternalRepositoryException(
                        "Unable to clone resource '" + originalResource.getURI() + "'", e);
            }
        }
        public Resource getOriginalResource() {
            return this.originalResource;
        }

        public Resource getSuppliedResource() {
            return this.suppliedResource;
        }

        public ResourceImpl getNewResource() {
            return this.newResource;
        } 
        
        public Principal getPrincipal() {
            return this.principal;
        }
        
    }
    
}
