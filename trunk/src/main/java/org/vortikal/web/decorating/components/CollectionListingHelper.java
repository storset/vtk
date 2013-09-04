package org.vortikal.web.decorating.components;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.MultiHostSearcher;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.util.repository.DocumentPrincipalMetadataRetriever;

public class CollectionListingHelper {

    private Set<String> applicableResourceTypes;
    private DocumentPrincipalMetadataRetriever documentPrincipalMetadataRetriever;

    /**
     * Check if a given principal has edit privileges on a property set
     * 
     * @param repo
     *            The backend used to fetch the actual resource and
     *            corresponding acl
     * 
     * @param propSet
     *            The property set to check
     * 
     * @param token
     *            The security token used to retrieve the resource
     * 
     * @param principal
     *            The principal for whom we perform the check
     * 
     */
    public boolean checkResourceForEditLink(Repository repo, PropertySet propSet, String token, Principal principal)
            throws Exception {

        if (token == null || principal == null) {
            return false;
        }
        String rt = propSet.getResourceType();

        // Attempt check only if resource is NOT from solr AND resource
        // type is applicable for editing
        if (propSet.getPropertyByPrefix(null, MultiHostSearcher.MULTIHOST_RESOURCE_PROP_NAME) == null
                && isApplicableResourceType(rt)) {
            try {
                Resource res = repo.retrieve(token, propSet.getURI(), true);
                return checkResourceForEditLink(repo, res, principal);
            } catch (Exception exception) {
            }
        }

        return false;
    }

    /**
     * Check if a given principal has edit privileges on a resource
     * 
     * @param repo
     *            The backend used to perform check
     * 
     * @param resource
     *            The resource to check
     * 
     * @param principal
     *            The principal for whom we perform the check
     * 
     */
    public boolean checkResourceForEditLink(Repository repo, Resource resource, Principal principal) {
        if (resource.getLock() != null && !resource.getLock().getPrincipal().equals(principal)) {
            return false;
        }

        return repo.authorize(principal, resource.getAcl(), Privilege.ALL)
                || repo.authorize(principal, resource.getAcl(), Privilege.READ_WRITE);

    }

    /**
     * For a set of resources (property sets), get links to principal documents
     * (if they exist) for each principal of the type given by the supplied
     * principal type.
     * 
     * @param propertySets
     *            The set of resources to fetch principal objects from
     * 
     * @param preferredLocale
     *            The preferred locale to use when searching for principal
     *            documents
     * 
     * @param principalTypePropDef
     *            The principal type provided by the property set to search for,
     *            e.g. last modified of owner.
     * 
     */
    public Map<String, Principal> getPrincipalDocumentLinks(Set<PropertySet> propertySets, Locale preferredLocale,
            PropertyTypeDefinition principalTypePropDef) {

        Set<String> uids = new HashSet<String>();
        for (PropertySet ps : propertySets) {

            Property principalTypeProp = principalTypePropDef != null ? ps.getProperty(principalTypePropDef) : ps
                    .getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.MODIFIEDBY_PROP_NAME);

            if (principalTypeProp != null) {
                uids.add(principalTypeProp.getPrincipalValue().getName());
            }

        }

        return documentPrincipalMetadataRetriever.getPrincipalDocumentsMapByUid(uids, preferredLocale);
    }

    private boolean isApplicableResourceType(String rt) {
        return this.applicableResourceTypes.contains(rt);
    }

    @Required
    public void setApplicableResourceTypes(Set<String> applicableResourceTypes) {
        this.applicableResourceTypes = applicableResourceTypes;
    }

    @Required
    public void setDocumentPrincipalMetadataRetriever(
            DocumentPrincipalMetadataRetriever documentPrincipalMetadataRetriever) {
        this.documentPrincipalMetadataRetriever = documentPrincipalMetadataRetriever;
    }

}
