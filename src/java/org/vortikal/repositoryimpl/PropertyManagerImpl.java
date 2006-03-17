package org.vortikal.repositoryimpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
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
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.web.service.RepositoryAssertion;

/**
 * XXX: What to do about swapping old resource with new?
 * XXX: Add resource type to resource
 * XXX: Validation is missing
 * XXX: Validate logic!
 * XXX: catch or declare evaluation and authorization exceptions on a reasonable level
 * XXX: implement authorization and contentimpl
 */
public class PropertyManagerImpl implements InitializingBean, ApplicationContextAware {

    private RoleManager roleManager;
    private PrincipalManager principalManager;

    private ResourceTypeDefinition rootResourceTypeDefinition;
    
    // Currently maps a parent resource type def. to its children (arrays)
    private Map resourceTypeDefinitions;
    
    // Currently maps namespaceUris to maps which map property names to defs.
    private Map propertyTypeDefinitions;
    
    private ApplicationContext applicationContext;
    
    private ResourceTypeDefinition[] getResourceTypeDefinitionChildren(ResourceTypeDefinition rt) {
        return (ResourceTypeDefinition[])resourceTypeDefinitions.get(rt);
    }
    
    public void afterPropertiesSet() throws Exception {
        if (roleManager == null) {
            throw new BeanInitializationException("Property 'roleManager' not set.");
        } else if (principalManager == null) {
            throw new BeanInitializationException("Property 'principalManager' not set.");
        } else if (rootResourceTypeDefinition == null) {
            throw new BeanInitializationException("Property 'rootResourceTypeDefinition' not set.");
        }

        List resourceTypeDefinitionList = 
            new ArrayList(applicationContext.getBeansOfType(ResourceTypeDefinition.class, 
                false, false).values());

        
        this.propertyTypeDefinitions = new HashMap();
        this.resourceTypeDefinitions = new HashMap();
        for (Iterator i = resourceTypeDefinitionList.iterator(); i.hasNext();) {
            // Populate map of property type definitions
            ResourceTypeDefinition def = (ResourceTypeDefinition)i.next();
            PropertyTypeDefinition[] propDefs = def.getPropertyTypeDefinitions();
            String namespaceUri = def.getNamespace().getURI();
            Map propDefMap = new HashMap();
            this.propertyTypeDefinitions.put(namespaceUri, propDefMap);
            for (int u=0; u<propDefs.length; u++) {
                propDefMap.put(propDefs[u].getName(), propDefs[u]);
            }
            
            // Populate map of resourceTypeDefiniton parent -> children
            ResourceTypeDefinition parent = def.getParentTypeDefinition();
            ResourceTypeDefinition[] children = 
                    (ResourceTypeDefinition[]) this.resourceTypeDefinitions.get(parent);
            
            // Array append (or create if not exists for given parent)
            ResourceTypeDefinition[] newChildren = null;
            if (children == null) {
                children = new ResourceTypeDefinition[1];
                children[0] = def;
            } else {
                newChildren = new ResourceTypeDefinition[children.length+1];
                System.arraycopy(children, 0, newChildren, 0, children.length);
                newChildren[newChildren.length-1] = def;
            }
            this.resourceTypeDefinitions.put(parent, newChildren);
           
        }
    }        

    private boolean checkAssertions(ResourceTypeDefinition rt, Resource resource, Principal principal) {
        RepositoryAssertion[] assertions = rt.getAssertions();

        if (assertions != null) {
            for (int i = 0; i < assertions.length; i++) {
                if (!assertions[i].matches(resource, principal))
                    return false;
            }
        }
        return true;
    }

    public ResourceImpl create(Principal principal, String uri, boolean collection) throws Exception {
        // XXX: Add resource type to resource
        ResourceImpl newResource = new ResourceImpl(uri, this.principalManager, this);
        ResourceTypeDefinition rt = create(principal, newResource, new Date(), 
                collection, rootResourceTypeDefinition);
        return newResource;
    }

    private ResourceTypeDefinition create(Principal principal, 
            ResourceImpl newResource, Date time, boolean isCollection, 
            ResourceTypeDefinition rt) throws Exception {

        // Checking if resource type matches
        if (!checkAssertions(rt, newResource, principal)) return null;

        // Evaluating resource type properties
        List newProps = new ArrayList();
        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();
        for (int i = 0; i < def.length; i++) {
            PropertyTypeDefinition propertyDef = def[i];
            
            CreatePropertyEvaluator evaluator = propertyDef.getCreateEvaluator();
            if (evaluator != null) {
                Property prop = createProperty(rt.getNamespace().getURI(), propertyDef.getName());
                if (evaluator.create(principal, prop, newResource, isCollection, time)) {
                    newProps.add(prop);
                }
                
            }
        }
        for (Iterator iter = newProps.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }

        // Checking child resource types by delegating
        ResourceTypeDefinition[] children = getResourceTypeDefinitionChildren(rt);
        for (int i = 0; i < children.length; i++) {
            ResourceTypeDefinition resourceType = create(principal, newResource, time, isCollection, children[i]);
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }
    
    private void addToPropsMap(Map parent, Property property) {
        Map map = (Map) parent.get(property.getNamespace());
        if (map == null) {
            map = new HashMap();
            parent.put(property.getNamespace(), map);
        }
        map.put(property.getName(), property);

    }
    
    public void storeProperties(ResourceImpl resource, Principal principal,
            Resource dto) throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IllegalOperationException, IOException {
        // For all properties, check if they are modified, deleted or created
        Map allreadySetProperties = new HashMap();
        List deadProperties = new ArrayList();
        Authorization authorization = new Authorization(principal, resource.getAcl(), this.roleManager);
        for (Iterator iter = resource.getProperties().iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            Property userProp = dto.getProperty(prop.getNamespace(), prop.getName());

            if (userProp == null) {
                // Deleted
                if (prop.getDefinition() == null) {
                    // Dead - ok
                } else {
                    if (prop.getDefinition().isMandatory()) {
                        throw new ConstraintViolationException("Property is mandatory: " + prop);
                    }
                    // check if allowed
                    authorization.authorize(prop.getDefinition().getProtectionLevel());
                    addToPropsMap(allreadySetProperties, userProp);
                }
            } else if (!prop.equals(userProp)) {
                // Changed value
                if (prop.getDefinition() == null) {
                    // Dead
                    deadProperties.add(userProp);
                } else {
                    // check if allowed
                    authorization.authorize(prop.getDefinition().getProtectionLevel());
                    addToPropsMap(allreadySetProperties, userProp);
                }
            } else {
                // Unchanged - to be evaluated
            }
        }
        for (Iterator iter = dto.getProperties().iterator(); iter.hasNext();) {
            Property userProp = (Property) iter.next();
            Property prop = resource.getProperty(userProp.getNamespace(), userProp.getName());

            if (prop == null) {
                // Added
                if (userProp.getDefinition() == null) {
                    // Dead
                    deadProperties.add(userProp);
                } else {
                    // check if allowed
                    authorization.authorize(prop.getDefinition().getProtectionLevel());
                    addToPropsMap(allreadySetProperties, userProp);
                }
            }
        }
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), 
                this.principalManager, this);
        newResource.setID(resource.getID());
        newResource.setACL(resource.getAcl());
        newResource.setLock(resource.getLock());
        
        // Evaluate resource tree, for all live props not overridden, evaluate
        ResourceTypeDefinition rt = propertiesModification(principal, newResource, dto,
                new Date(), allreadySetProperties, rootResourceTypeDefinition);

        for (Iterator iter = deadProperties.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }
        for (Iterator iter = allreadySetProperties.values().iterator(); iter.hasNext();) {
            Map map = (Map) iter.next();
            for (Iterator iterator = map.values().iterator(); iterator
                    .hasNext();) {
                Property prop = (Property) iterator.next();
                newResource.addProperty(prop);
            }
        }
        
//    if (!resource.getOwner().equals(dto.getOwner().getQualifiedName())) {
//        /* Attempt to take ownership, only the owner of a parent
//         * resource may do that, so do it in a secure manner: */
//        setResourceOwner(resource, principal, dto.getOwner());
//    }
//
//    if (dto.getOverrideLiveProperties()) {
//        resource.setPropertiesLastModified(dto.getPropertiesLastModified());
//        resource.setContentLastModified(dto.getContentLastModified());
//        resource.setCreationTime(dto.getCreationTime());
//
//    } else {
//        resource.setPropertiesLastModified(new Date());
//        resource.setPropertiesModifiedBy(principal.getQualifiedName());
//    }
//    
//    if (!resource.isCollection()) {
//
//        resource.setContentType(dto.getContentType());
//        resource.setCharacterEncoding(null);
//
//        resource.setContentLocale(null);
//        if (dto.getContentLocale() != null)
//            resource.setContentLocale(dto.getContentLocale().toString());
//
//        if ((resource.getContentType() != null)
//            && ContentTypeHelper.isTextContentType(resource.getContentType()) &&
//            (dto.getCharacterEncoding() != null)) {
//            try {
//                /* Force checking of encoding */
//                new String(new byte[0], dto.getCharacterEncoding());
//
//                resource.setCharacterEncoding(dto.getCharacterEncoding());
//            } catch (java.io.UnsupportedEncodingException e) {
//                // FIXME: Ignore unsupported character encodings?
//            }
//        }
//
//    }
//
//    resource.setDisplayName(dto.getDisplayName());
//    resource.setProperties(Arrays.asList(dto.getProperties()));
    
}
    
    private ResourceTypeDefinition propertiesModification(Principal principal, 
            ResourceImpl newResource, Resource dto, Date time, Map allreadySetProperties, 
            ResourceTypeDefinition rt) {

        // Checking if resource type matches
        if (!checkAssertions(rt, newResource, principal)) return null;

        // Evaluating resource type properties
        List newProps = new ArrayList();
        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();
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
            Property prop = dto.getProperty(rt.getNamespace().getURI(), propertyDef.getName());
            PropertiesModificationPropertyEvaluator evaluator = propertyDef.getPropertiesModificationEvaluator();
            if (evaluator != null) {
                if (prop == null) 
                    prop = createProperty(rt.getNamespace().getURI(), propertyDef.getName());
                if (evaluator.propertiesModification(principal, prop, newResource, time)) {
                    newProps.add(prop);
                }
                
            } else if (prop != null) {
                newProps.add(prop);
            }
        }
        for (Iterator iter = newProps.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }

        // Checking child resource types by delegating
        ResourceTypeDefinition[] children = getResourceTypeDefinitionChildren(rt);
        for (int i = 0; i < children.length; i++) {
            ResourceTypeDefinition resourceType = 
                propertiesModification(principal, newResource, dto, time, allreadySetProperties, children[i]);
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }
    
    public ResourceImpl collectionContentModification(ResourceImpl resource, 
            Principal principal) {
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), 
                this.principalManager, this);
        newResource.setID(resource.getID());
        newResource.setACL(resource.getAcl());
        newResource.setLock(resource.getLock());
        ResourceTypeDefinition rt = contentModification(principal, newResource, 
                resource, null, new Date(), rootResourceTypeDefinition);
        return newResource;
    }


    public ResourceImpl fileContentModification(ResourceImpl resource, 
            Principal principal, InputStream inputStream) {
        // XXX: What to do about swapping old resource with new?
        // XXX: Add resource type to resource
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), 
                this.principalManager, this);
        newResource.setID(resource.getID());
        newResource.setACL(resource.getAcl());
        newResource.setLock(resource.getLock());
        ResourceTypeDefinition rt = contentModification(principal, newResource, resource,
                new ContentImpl(inputStream), new Date(), rootResourceTypeDefinition);
        return newResource;
    }
    
    
    private ResourceTypeDefinition contentModification(Principal principal, 
            ResourceImpl newResource, Resource original, Content content, Date time, ResourceTypeDefinition rt) {

        // Checking if resource type matches
        if (!checkAssertions(rt, newResource, principal)) return null;

        // Evaluating resource type properties
        List newProps = new ArrayList();
        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();
        for (int i = 0; i < def.length; i++) {
            PropertyTypeDefinition propertyDef = def[i];
            
            Property prop = original.getProperty(rt.getNamespace().getURI(), propertyDef.getName());
            ContentModificationPropertyEvaluator evaluator = propertyDef.getContentModificationEvaluator();
            if (evaluator != null) {
                if (prop == null) 
                    prop = createProperty(rt.getNamespace().getURI(), propertyDef.getName());
                if (evaluator.contentModification(principal, prop, newResource, content, time)) {
                    newProps.add(prop);
                }
            } else if (prop != null) {
                newProps.add(prop);
            }
        }
        for (Iterator iter = newProps.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }

        // Checking child resource types by delegating
        ResourceTypeDefinition[] children = getResourceTypeDefinitionChildren(rt);
        for (int i = 0; i < children.length; i++) {
            ResourceTypeDefinition resourceType = 
                contentModification(principal, newResource, original, content, time, children[i]);
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }

    
    
    
    
    
    public Property createProperty(String namespaceUri, String name) {

        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespaceUri);
        prop.setName(name);
        
        // XXX: huge risk of nullpointer exception
        PropertyTypeDefinition propDef = (PropertyTypeDefinition)
            ((Map)propertyTypeDefinitions.get(namespaceUri)).get(name);
        prop.setDefinition(propDef);
        
        
        return prop;
    }

    public Property createProperty(String namespaceUri, String name, Object value) 
        throws ValueFormatException {
        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespaceUri);
        prop.setName(name);
        
        // XXX: huge risk of nullpointer exception
        PropertyTypeDefinition propDef = (PropertyTypeDefinition)
            ((Map)propertyTypeDefinitions.get(namespaceUri)).get(name);
        prop.setDefinition(propDef);
        
        // XXX: complete this
        if (value instanceof Date) {
            Date date = (Date) value;
            prop.setDateValue(date);
        } else if (value instanceof Boolean) {
            Boolean bool = (Boolean) value;
            prop.setBooleanValue(bool.booleanValue());
        } else {
            prop.setStringValue((String) value);
        }
        return prop;
    }
    
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public void setRootResourceTypeDefinition(
            ResourceTypeDefinition rootResourceTypeDefinition) {
        this.rootResourceTypeDefinition = rootResourceTypeDefinition;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

}