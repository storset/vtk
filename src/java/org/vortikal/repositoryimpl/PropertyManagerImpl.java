package org.vortikal.repositoryimpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryOperations;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.util.repository.MimeHelper;
import org.vortikal.web.service.Assertion;
import org.vortikal.web.service.RepositoryAssertion;

public class PropertyManagerImpl implements InitializingBean {

    private RoleManager roleManager;
    private PermissionsManager permissionsManager;
    private PrincipalManager principalManager;

    private ResourceTypeDefinition rootResourceTypeDefinition;
    private Map resourceTypeDefinitions;
    
    private ResourceTypeDefinition[] getResourceTypeDefinitionChildren(ResourceTypeDefinition rt) {
        return (ResourceTypeDefinition[])resourceTypeDefinitions.get(rt);
    }
    
    private ResourceTypeDefinition evaluateProperties(Principal principal, 
            Resource newResource, Resource oldResource, 
            String operation, ResourceTypeDefinition rt) throws Exception {


        // Checking if resourceType matches
        
        RepositoryAssertion[] assertions = rt.getAssertions();

        if (assertions != null) {
            for (int i = 0; i < assertions.length; i++) {
// Tmp-removed differing resources               if (!assertions[i].matches(newResource, p)) return null;
            }
        }

        // Get authorization for principal on resource
        Authorization authorization = new Authorization(p, oldResource);

        // Evaluating resource type properties

        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();
        List properties = new ArrayList();
        for (int i = 0; i < def.length; i++) {
            PropertyTypeDefinition propertyDef = def[i];

            Property oldProp = oldResource.getProperty(rt.getNamespace().getURI(), propertyDef.getName());

            if (oldProp != newProp) {
                authorization.authorize(propertyDef.getProtectionLevel());
            }

            Value value = propertyDef.getPropertyEvaluator().extractFromProperties(operation, p, newResource, oldProp.getValue());
            if (value != null) {
                String namespaceUri = (rt.getNamespace() == null) ? null : rt.getNamespace().getURI();
                Property property = createProperty(namespaceUri, propertyDef.getName(), value);
                properties.add(property);
            }
        }

        for (Iterator iter = properties.iterator(); iter.hasNext();) {
            Property property = (Property) iter.next();
            newResource.addProperty(property);
        }
        
        
        ResourceTypeDefinition[] children = getResourceTypeDefinitionChildren(rt);
        for (int i = 0; i < children.length; i++) {
            ResourceTypeDefinition resourceType = evaluateProperties(p, newResource, oldResource, operation, children[i]);
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }
    
    public Resource create(Principal principal, String uri, boolean collection) throws Exception {
        Resource r = new Resource(uri);
        
        evaluateProperties(principal, r, null, RepositoryOperations.CREATE, rootResourceTypeDefinition);
        
        r.setACL(new ACL());
        r.setInheritedACL(true);

        return r;
    }

    public void storeProperties(Resource resource, Principal principal,
            org.vortikal.repository.Resource dto)
throws AuthenticationException, AuthorizationException, 
ResourceLockedException, IllegalOperationException, IOException {

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
    
    public void collectionContentModified(Resource resource, Principal principal) {
        Date now = new Date();
        // Update timestamps:
//        resource.setContentLastModified(now);
//        resource.setPropertiesLastModified(now);

        // Update principal info:
//        resource.setContentModifiedBy(principal.getQualifiedName());
//        resource.setPropertiesModifiedBy(principal.getQualifiedName());
   
    }
    
    public void resourceContentModification(Resource resource, 
            Principal principal, InputStream inputStream) {

        // Update timestamps:
//        resource.setContentLastModified(new Date());
//        resource.setContentModifiedBy(principal.getQualifiedName());
    }
    
    
    

    public Property createProperty(String namespaceUri, String name, Object value) {
        Property prop = new Property();
        prop.setNamespace(namespaceUri);
        prop.setName(name);
        
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

    public void afterPropertiesSet() throws Exception {
        // TODO Auto-generated method stub
        
    }

    public void authorize(Principal principal, Resource resource, 
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

}