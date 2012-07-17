package org.vortikal.web.decorating.components;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.MultiHostSearcher;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.search.Listing;

public class CollectionListingHelper {

    private Set<String> applicableResourceTypes;

    public void checkListingsForEditLinks(Repository repo, String token, Principal principal, List<Listing> listings)
            throws Exception {

        // If not logged in, don't provide any edit-authorizations and stop
        // checks.
        if (principal == null) {
            return;
        }

        for (Listing listing : listings) {

            List<PropertySet> psList = listing.getFiles();
            boolean[] editLinkAuthorized = new boolean[psList.size()];

            int i = 0;
            for (PropertySet ps : psList) {

                boolean authorized = false;
                String rt = ps.getResourceType();

                // Attempt check only if resource is NOT from solr AND resource
                // type is applicable for editing
                if (ps.getPropertyByPrefix(null, MultiHostSearcher.MULTIHOST_RESOURCE_PROP_NAME) == null
                        && this.isApplicableResourceType(rt)) {
                    try {
                        Resource res = repo.retrieve(token, ps.getURI(), true);
                        authorized = checkResourceForEditLink(repo, res, principal);
                    } catch (Exception exception) {
                    }
                }

                editLinkAuthorized[i++] = authorized;
            }

            listing.setEditLinkAuthorized(editLinkAuthorized);
        }
    }

    private boolean isApplicableResourceType(String rt) {
        return this.applicableResourceTypes.contains(rt);
    }

    public boolean checkResourceForEditLink(Repository repo, Resource res, Principal principal) {
        if (res.getLock() != null && !res.getLock().getPrincipal().equals(principal)) {
            return false;
        }

        return repo.authorize(principal, res.getAcl(), Privilege.ALL)
                || repo.authorize(principal, res.getAcl(), Privilege.READ_WRITE);

    }

    @Required
    public void setApplicableResourceTypes(Set<String> applicableResourceTypes) {
        this.applicableResourceTypes = applicableResourceTypes;
    }

}
