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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertiesModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.MixinResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.web.service.RepositoryAssertion;


/**
 * XXX: Validation is missing
 * XXX: Validate all logic!
 * XXX: catch or declare evaluation and authorization exceptions on a reasonable level
 */
public class PropertyManagerImpl implements InitializingBean, ApplicationContextAware {

    private Log logger = LogFactory.getLog(this.getClass());

    private RoleManager roleManager;
    private PrincipalManager principalManager;
    private AuthorizationManager authorizationManager;

    private ValueFactory valueFactory;
    
    private PrimaryResourceTypeDefinition rootResourceTypeDefinition;
    private boolean lazyInit = false;
    private boolean init = false;
    
    Collection resourceTypeDefinitionBeans;
    
    // Currently maps a parent resource type def. to its children (arrays)
    private Map resourceTypeDefinitions;
    
    // Currently maps namespaceUris to maps which map property names to defs.
    private Map propertyTypeDefinitions;
    
    private Map mixinTypeDefinitions;

    private ApplicationContext applicationContext;
    
    private PrimaryResourceTypeDefinition[] getResourceTypeDefinitionChildren(
        PrimaryResourceTypeDefinition rt) {

        PrimaryResourceTypeDefinition[] children =
            (PrimaryResourceTypeDefinition[])resourceTypeDefinitions.get(rt);
        if (children == null)
            return new PrimaryResourceTypeDefinition[0];
        return children;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (roleManager == null) {
            throw new BeanInitializationException("Property 'roleManager' not set.");
        } else if (principalManager == null) {
            throw new BeanInitializationException("Property 'principalManager' not set.");
        } else if (authorizationManager == null) {
            throw new BeanInitializationException("Property 'authorizationManager' not set.");
        } else if (rootResourceTypeDefinition == null) {
            throw new BeanInitializationException("Property 'rootResourceTypeDefinition' not set.");
        } else if (valueFactory == null) {
            throw new BeanInitializationException("Property 'valueFactory' not set.");
        }

        resourceTypeDefinitionBeans = 
            BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, 
                    PrimaryResourceTypeDefinition.class, false, false).values();
        this.propertyTypeDefinitions = new HashMap();
        this.resourceTypeDefinitions = new HashMap();
        this.mixinTypeDefinitions = new HashMap();

        if (!lazyInit) init();
    }

    private synchronized void init() {
        if (init) return;

        for (Iterator i = resourceTypeDefinitionBeans.iterator(); i.hasNext();) {
            PrimaryResourceTypeDefinition def = (PrimaryResourceTypeDefinition)i.next();
            
            // Populate map of property type definitions
            PropertyTypeDefinition[] propDefs = def.getPropertyTypeDefinitions();
            Namespace namespace = def.getNamespace();
            Map propDefMap = (Map)this.propertyTypeDefinitions.get(namespace);
            
            if (propDefMap == null) {
                propDefMap = new HashMap();
                this.propertyTypeDefinitions.put(namespace, propDefMap);
            }
            for (int u=0; u<propDefs.length; u++) {
                propDefMap.put(propDefs[u].getName(), propDefs[u]);
            }
            
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
                newChildren = new PrimaryResourceTypeDefinition[children.length+1];
                System.arraycopy(children, 0, newChildren, 0, children.length);
                newChildren[newChildren.length-1] = def;
            }
            this.resourceTypeDefinitions.put(parent, newChildren);            
            this.mixinTypeDefinitions.put(def, getMixinTypes(def));

        }

        init = true;
    }
    


    private boolean checkAssertions(ResourceTypeDefinition rt,
                                    Resource resource, Principal principal) {
        RepositoryAssertion[] assertions = rt.getAssertions();

        if (assertions != null) {
            for (int i = 0; i < assertions.length; i++) {
                if (!assertions[i].matches(resource, principal))
                    return false;
            }
        }
        return true;
    }



    public ResourceImpl create(Principal principal, String uri, boolean collection) {
        if (!init) init();

        ResourceImpl newResource = new ResourceImpl(uri, this);
        PrimaryResourceTypeDefinition rt = create(principal, newResource, new Date(), 
                collection, rootResourceTypeDefinition);

        if (logger.isDebugEnabled()) {
            logger.debug("Found resource type definition: " 
                    + rt + " for resource created at '" + uri + "'");
        }
        
        newResource.setResourceType(rt.getName());
        
        if (collection) {
            newResource.setChildURIs(new String[]{});
        }
        
        return newResource;
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
            (MixinResourceTypeDefinition[]) this.mixinTypeDefinitions.get(rt);
        
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
        PrimaryResourceTypeDefinition[] children = getResourceTypeDefinitionChildren(rt);
        
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
                            PropertyTypeDefinition[] def, List newProps) {
        for (int i = 0; i < def.length; i++) {
            PropertyTypeDefinition propertyDef = def[i];
            
            CreatePropertyEvaluator evaluator = propertyDef.getCreateEvaluator();
            if (evaluator != null) {
                Property prop = createProperty(rt.getNamespace(), propertyDef.getName());
                if (evaluator.create(principal, prop, newResource, isCollection, time)) {
                    newProps.add(prop);
                }
            }
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
    
    public ResourceImpl storeProperties(ResourceImpl resource, Principal principal,
                                        Resource dto)
        throws AuthenticationException, AuthorizationException,
        CloneNotSupportedException {

        if (!init) init();

        String uri = resource.getURI();
        
        // For all properties, check if they are modified, deleted or created
        Map allreadySetProperties = new HashMap();
        
        List deadProperties = new ArrayList();
        
        // Looping over already existing properties
        for (Iterator iter = resource.getProperties().iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            Property userProp = dto.getProperty(prop.getNamespace(), prop.getName());

            if (userProp == null) {
                // Deleted
                if (prop.getDefinition() == null) {
                    // Dead - ok
                } else {
                    if (prop.getDefinition().isMandatory()) {
                        throw new ConstraintViolationException(
                            "Property is mandatory: " + prop);
                    }
                    // check if allowed
                    try {
                        authorize(prop.getDefinition().getProtectionLevel(), principal, uri);
                    } catch (Exception e) {
                        throw new ConstraintViolationException(
                            "Not authorized to delete property " + prop, e);
                    }
                    // It will be removed

                    // XXX: if user explicitly wants to delete
                    // property and prop has evaluator, it will be
                    // evaluated. Is this desired behaviour?
                }
            } else if (!prop.equals(userProp)) {
                // Changed value
                if (prop.getDefinition() == null) {
                    // Dead
                    deadProperties.add(userProp);
                } else {
                    // check if allowed
                    try {
                        authorize(prop.getDefinition().getProtectionLevel(), principal, uri);
                    } catch (Exception e) {
                        throw new ConstraintViolationException(
                            "Not authorized to edit property " + prop, e);
                    }
                    
                    addToPropsMap(allreadySetProperties, userProp);
                }
            } else {
                if (prop.getDefinition() == null) {
                    // Dead and un-changed.
                    deadProperties.add(userProp);
                }
                // Unchanged - to be evaluated
            }
        }
        
        for (Iterator iter = dto.getProperties().iterator(); iter.hasNext();) {
            Property userProp = (Property) iter.next();
            Property prop = resource.getProperty(userProp.getNamespace(),
                                                 userProp.getName());
            
            if (prop == null) {
                // Added
                if (userProp.getDefinition() == null) {
                    // Dead
                    deadProperties.add(userProp);
                } else {
                    // check if allowed
                    try {
                        authorize(userProp.getDefinition().getProtectionLevel(), principal, uri);
                    } catch (Exception e) {
                        throw new ConstraintViolationException(
                            "Not authorized to edit property " + prop, e);
                    }
                    addToPropsMap(allreadySetProperties, userProp);
                }
            } 
        }
        
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), this);
        newResource.setID(resource.getID());
        newResource.setACL((Acl)resource.getAcl().clone());

        if (resource.getLock() != null)
            newResource.setLock((Lock)resource.getLock().clone());
        
        // Evaluate resource tree, for all live props not overridden, evaluate
        ResourceTypeDefinition rt = propertiesModification(principal, newResource, dto,
                new Date(), allreadySetProperties, rootResourceTypeDefinition);

        for (Iterator iter = deadProperties.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            if (logger.isDebugEnabled()) {
                logger.debug("Adding dead property to new resource: " + prop);
            }
            
            newResource.addProperty(prop);
        }
        for (Iterator iter = allreadySetProperties.values().iterator(); iter.hasNext();) {
            Map map = (Map) iter.next();
            for (Iterator iterator = map.values().iterator(); iterator.hasNext();) {
                Property prop = (Property) iterator.next();

                newResource.addProperty(prop);
            }
        }
        
        return newResource;
    }
    

    private PrimaryResourceTypeDefinition propertiesModification(Principal principal, 
            ResourceImpl newResource, Resource dto, Date time, Map allreadySetProperties, 
            PrimaryResourceTypeDefinition rt) {

        // Checking if resource type matches
        if (!checkAssertions(rt, newResource, principal)) return null;

        // Evaluating primary resource type properties
        List newProps = new ArrayList();
        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();
        evalPropertiesModification(principal, newResource, dto, time,
                                   allreadySetProperties,rt, def, newProps);
        
        // Evaluating mixin resource type properties
        MixinResourceTypeDefinition[] mixinTypes =
            (MixinResourceTypeDefinition[]) this.mixinTypeDefinitions.get(rt);

        for (int i = 0; i < mixinTypes.length; i++) {
            PropertyTypeDefinition[] mixinDef = mixinTypes[i].getPropertyTypeDefinitions();
            evalPropertiesModification(principal, newResource, dto, time,
                                       allreadySetProperties, mixinTypes[i],
                                       mixinDef, newProps);
        }

        // Check validator...
//        for (Iterator iter = newProps.iterator(); iter.hasNext();) {
//            Property prop = (Property) iter.next();
//            PropertyValidator validator = prop.getDefinition().getValidator();
//            if (validator != null)
//                validator.validate(principal, newResource, prop);
//        }
        
        for (Iterator iter = newProps.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }

        // Checking child resource types by delegating
        PrimaryResourceTypeDefinition[] children = getResourceTypeDefinitionChildren(rt);
        for (int i = 0; i < children.length; i++) {
            PrimaryResourceTypeDefinition resourceType = 
                propertiesModification(principal, newResource, dto, time,
                                       allreadySetProperties, children[i]);
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }
    

    private void evalPropertiesModification(
        Principal principal, ResourceImpl newResource, Resource dto,
        Date time, Map allreadySetProperties, ResourceTypeDefinition rt,
        PropertyTypeDefinition[] def, List newProps) {

        for (int i = 0; i < def.length; i++) {
            PropertyTypeDefinition propertyDef = def[i];
            
            // If property allready set, don't evaluate
            Map propsMap = (Map)allreadySetProperties.get(rt.getNamespace());
            if (propsMap != null) {
                Property p = (Property) propsMap.get(propertyDef.getName());
                if (p != null) {
                    newProps.add(p);
                    propsMap.remove(propertyDef.getName());
                    continue;
                }
            }

            // Not set, evaluate
            Property prop = dto.getProperty(rt.getNamespace(), propertyDef.getName());
            PropertiesModificationPropertyEvaluator evaluator =
                propertyDef.getPropertiesModificationEvaluator();

            if (evaluator != null) {
                if (prop == null) 
                    prop = createProperty(rt.getNamespace(), propertyDef.getName());
                if (evaluator.propertiesModification(principal, prop, newResource, time)) {
                    newProps.add(prop);
                }
                
            } else if (prop != null) {
                newProps.add(prop);
            }
        }
    }
    



    public ResourceImpl collectionContentModification(ResourceImpl resource, 
            Principal principal) {
        if (!init) init();
        
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), this);
        newResource.setID(resource.getID());
        newResource.setACL(resource.getAcl());
        newResource.setLock(resource.getLock());
        ResourceTypeDefinition rt = contentModification(principal, newResource, 
                resource, null, new Date(), rootResourceTypeDefinition);
        return newResource;
    }


    public ResourceImpl fileContentModification(ResourceImpl resource, 
            Principal principal, InputStream inputStream) {
        if (!init) init();
        
        // XXX: What to do about swapping old resource with new?
        // XXX: Add resource type to resource
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), this);
        newResource.setID(resource.getID());
        newResource.setACL(resource.getAcl());
        newResource.setLock(resource.getLock());
        ResourceTypeDefinition rt = contentModification(principal, newResource, resource,
                new ContentImpl(inputStream), new Date(), rootResourceTypeDefinition);
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
            (MixinResourceTypeDefinition[]) this.mixinTypeDefinitions.get(rt);
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
        PrimaryResourceTypeDefinition[] children = getResourceTypeDefinitionChildren(rt);
        for (int i = 0; i < children.length; i++) {
            ResourceTypeDefinition resourceType = 
                contentModification(principal, newResource,
                                    original, content, time, children[i]);
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }

    
    private void evalContentModification(Principal principal, ResourceImpl newResource,
                                         Resource original, Content content, Date time,
                                         ResourceTypeDefinition rt,
                                         PropertyTypeDefinition[] def, List newProps) {

        for (int i = 0; i < def.length; i++) {
            PropertyTypeDefinition propertyDef = def[i];
            
            Property prop = original.getProperty(rt.getNamespace(), propertyDef.getName());
            ContentModificationPropertyEvaluator evaluator =
                propertyDef.getContentModificationEvaluator();

            if (evaluator != null) {
                if (prop == null) 
                    prop = createProperty(rt.getNamespace(), propertyDef.getName());
                if (evaluator.contentModification(principal, prop, newResource, content, time)) {
                    newProps.add(prop);
                }
            } else if (prop != null) {
                newProps.add(prop);
            }
        }
    }


    public Property createProperty(Namespace namespace, String name) {

        if (!init) init();

        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespace);
        prop.setName(name);
        
        // XXX: probably not desired behavior
        prop.setDefinition(findPropertyTypeDefinition(namespace, name));
        
        if (logger.isDebugEnabled()) {
            logger.debug("Created property without initial value: " + prop);
        }
        
        return prop;
    }


    public Property createProperty(Namespace namespace, String name, Object value) 
        throws ValueFormatException {
        if (!init) init();

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
                throw new ValueFormatException("Supplied value not of any supported type.");
            }
            prop.setStringValue((String) value);
        } 
        
        
        if (logger.isDebugEnabled()) {
            logger.debug("Created property: " + prop);
        }

        return prop;
    }
    

    public Property createProperty(Namespace namespace, String name, 
                                   String[] stringValues, int type) 
        throws ValueFormatException {
        if (!init) init();
        
        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespace);
        prop.setName(name);
        
        PropertyTypeDefinition def = findPropertyTypeDefinition(namespace, name);
        prop.setDefinition(def);
        
        if (def != null && def.isMultiple()) {
            Value[] values = valueFactory.createValues(stringValues, type);
            prop.setValues(values);

            if (logger.isDebugEnabled()) {
                logger.debug("Created multi-value property: " + prop);
            }
        } else {
            // Not multi-value, stringValues must be of length 1, otherwise there are
            // inconsistency problems between database and config.
            if (stringValues.length > 1) {
                logger.error("Cannot convert multiple values to a single-value prop"
                             + " for property " + prop);
                throw new ValueFormatException(
                    "Cannot convert multiple values to a single-value prop"
                    + " for property " + prop);
            }
            
            Value value = valueFactory.createValue(stringValues[0], type);
            prop.setValue(value);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Created property: " + prop);
            }
        }
        
        return prop;
        
    }
    

    private PropertyTypeDefinition findPropertyTypeDefinition(Namespace namespace, 
                                                          String name) {
        PropertyTypeDefinition propDef = null;
        Map map = (Map)propertyTypeDefinitions.get(namespace);
        if (map != null) {
            propDef = (PropertyTypeDefinition) map.get(name);
        }
        
        if (logger.isDebugEnabled()) {
            if (propDef != null) {
                logger.debug("Found property definition : " + propDef
                        + " for property " + namespace.getPrefix() + ":" + name);
            } else {
                logger.debug("No definition found for property " +
                        namespace.getPrefix() + ":" + name);
            }
        }
        
        return propDef;
    }
    

    private void authorize(String action, Principal principal, String uri) 
        throws AuthenticationException, AuthorizationException, 
        ResourceLockedException, IOException{

        if (AuthorizationManager.WRITE.equals(action)) {
            this.authorizationManager.authorizeWrite(uri, principal);
        } else if (AuthorizationManager.WRITE_ACL.equals(action)) {
            this.authorizationManager.authorizeWriteAcl(uri, principal);
        } else if (AuthorizationManager.REPOSITORY_ADMIN_ROLE_ACTION.equals(action)) {
            this.authorizationManager.authorizePropertyEditAdminRole(uri, principal);
        } else if (AuthorizationManager.REPOSITORY_ROOT_ROLE_ACTION.equals(action)) {
            this.authorizationManager.authorizePropertyEditRootRole(uri, principal);
        } else {
            throw new AuthorizationException();
        }
    }
    
    private MixinResourceTypeDefinition[] getMixinTypes(ResourceTypeDefinition rt) {
        List mixinTypes = new ArrayList();
        MixinResourceTypeDefinition[] directMixins = rt.getMixinTypeDefinitions();
        if (directMixins != null) {
            for (int i = 0; i < directMixins.length; i++) {
                mixinTypes.add(directMixins[i]);
                MixinResourceTypeDefinition[] indirectMixins =
                    directMixins[i].getMixinTypeDefinitions();
                if (indirectMixins != null) {
                    mixinTypes.addAll(java.util.Arrays.asList(getMixinTypes(indirectMixins[i])));
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

    public void setRootResourceTypeDefinition(
            PrimaryResourceTypeDefinition rootResourceTypeDefinition) {
        this.rootResourceTypeDefinition = rootResourceTypeDefinition;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

}
