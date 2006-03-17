package org.vortikal.repositoryimpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Property;
import org.vortikal.repository.RepositoryOperations;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.web.service.RepositoryAssertion;

public class PropertyManagerImpl implements InitializingBean, ApplicationContextAware {

    private RoleManager roleManager;
    private PermissionsManager permissionsManager;
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
        // XXX: validate bean properties
        // XXX: this.resourceTypeDefinitions is not populated !
        
        List resourceTypeDefinitions = 
            new ArrayList(applicationContext.getBeansOfType(ResourceTypeDefinition.class, 
                false, false).values());

        // Populate map of property type definitions
        this.propertyTypeDefinitions = new HashMap();
        for (Iterator i = resourceTypeDefinitions.iterator(); i.hasNext();) {
            ResourceTypeDefinition def = (ResourceTypeDefinition)i.next();
            PropertyTypeDefinition[] propDefs = def.getPropertyTypeDefinitions();
            String namespaceUri = def.getNamespace().getURI();
            Map propDefMap = new HashMap();
            this.propertyTypeDefinitions.put(namespaceUri, propDefMap);
            for (int u=0; u<propDefs.length; u++) {
                propDefMap.put(propDefs[u].getName(), propDefs[u]);
            }
        }
    }        

    private ResourceTypeDefinition evaluateProperties(Principal principal, List properties,
            ResourceImpl newResource, ResourceImpl oldResource, 
            String operation, ResourceTypeDefinition rt) throws Exception {


        // Checking if resourceType matches
        
        RepositoryAssertion[] assertions = rt.getAssertions();

        if (assertions != null) {
            for (int i = 0; i < assertions.length; i++) {
// Tmp-removed differing resources               if (!assertions[i].matches(newResource, p)) return null;
            }
        }

        // Get authorization for principal on resource
        Authorization authorization = 
            new Authorization(principal, oldResource.getAcl());

        // Evaluating resource type properties
        List newProps = new ArrayList();
        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();
        for (int i = 0; i < def.length; i++) {
            PropertyTypeDefinition propertyDef = def[i];

            Property oldProp = oldResource.getProperty(rt.getNamespace().getURI(), propertyDef.getName());
            Property newProp = newResource.getProperty(rt.getNamespace().getURI(), propertyDef.getName());
            
            
            if (oldProp != newProp) {
                authorization.authorize(propertyDef.getProtectionLevel());
            }
            
            Value value = propertyDef.getPropertyEvaluator().
                evaluateProperties(operation, principal, newResource, newProp.getValue(), 
                        oldProp.getValue());

            if (value != null) {
                String namespaceUri = (rt.getNamespace() == null) ? null : rt.getNamespace().getURI();
                Property property = createProperty(namespaceUri, propertyDef.getName(), value);
                newProps.add(property);
            }
        }

        properties.addAll(newProps);
        
        ResourceTypeDefinition[] children = getResourceTypeDefinitionChildren(rt);
        for (int i = 0; i < children.length; i++) {
            ResourceTypeDefinition resourceType = evaluateProperties(principal, properties, newResource, oldResource, operation, children[i]);
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }
    
    public ResourceImpl create(Principal principal, String uri, boolean collection) throws Exception {
        // Evaluate resource tree, supplying date
        ResourceImpl r = new ResourceImpl(uri, this.principalManager, this);
        List properties = new ArrayList();
        evaluateProperties(principal, properties, r, null, RepositoryOperations.CREATE, rootResourceTypeDefinition);
        return r;
    }

    public void storeProperties(ResourceImpl resource, Principal principal,
            Resource dto) throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IllegalOperationException, IOException {
        // For all properties, check if they are modified, deleted or created
        // if user isn't allowed, throw exception
        // Otherwise set properties
        // Evaluate resource tree, for all live props not overridden, evaluate
        // For live props changed by user, DON'T evaluate
        
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
    
    public void collectionContentModification(ResourceImpl resource, Principal principal) {
        // evaluate resource tree, supplying date
        
        Date now = new Date();
        // Update timestamps:
//        resource.setContentLastModified(now);
//        resource.setPropertiesLastModified(now);

        // Update principal info:
//        resource.setContentModifiedBy(principal.getQualifiedName());
//        resource.setPropertiesModifiedBy(principal.getQualifiedName());
   
    }
    
    public void fileContentModification(ResourceImpl resource, 
            Principal principal, InputStream inputStream) {
        // evaluate resource tree, supplying date
        
        
        // Update timestamps:
//        resource.setContentLastModified(new Date());
//        resource.setContentModifiedBy(principal.getQualifiedName());
    }
    
    
    public Property createProperty(String namespaceUri, String name) {

        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespaceUri);
        prop.setName(name);
        
        // XXX: huge risk of nullpointer exception
        PropertyTypeDefinition propDef = (PropertyTypeDefinition)
            ((Map)propertyTypeDefinitions.get(namespaceUri)).get(name);
        prop.setPropertyTypeDefinition(propDef);
        
        
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
        prop.setPropertyTypeDefinition(propDef);
        
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
    
    public void setPermissionsManager(PermissionsManager permissionsManager) {
        this.permissionsManager = permissionsManager;
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


    public void authorize(Principal principal, ResourceImpl resource, 
            int protectionLevel) throws AuthorizationException {

        boolean owner = principal.getQualifiedName().equals(resource.getOwner());
        boolean root = this.roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT);
        boolean admin = false;
        
//        if (protectionLevel == PropertyType.PROTECTION_LEVEL_OWNER_EDITABLE ||
//                protectionLevel == PropertyType.PROTECTION_LEVEL_ROOT_EDITABLE) {
//            if (this.roleManager.hasRole(principal.getQualifiedName(),
//                    RoleManager.ROOT)) {
//                return;
//            }
//                    
//            if (!principal.getQualifiedName().equals(resource.getOwner())) {
//                throw new AuthorizationException("Principal "
//                        + principal.getQualifiedName()
//                        + " is not allowed to set owner of " + "resource "
//                        + resource.getURI());
//            }
//        }
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

}