package org.vortikal.web.decorating.components;

import java.util.List;

import org.vortikal.repository.MultiHostSearcher;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.search.Listing;

public class CollectionListingComponentHelper {

    // XXX FIXME!!!
    // Indices of return array must match indices of each property set in each
    // result set in each listing!!!
    public boolean[] checkListingsForEditLinks(Repository repo, String token, Principal principal, int maxItems,
            List<Listing> ll) throws Exception {
        Resource res;
        int i = 0;
        boolean[] edit = new boolean[maxItems];

        // If not logged in, don't provide any edit-authorizations and stop
        // checks.
        if (principal == null) {
            return edit;
        }

        for (Listing l : ll) {
            for (PropertySet ps : l.getFiles()) {

                boolean authorized = false;
                String rt = ps.getResourceType();

                // Attempt check only if resource is NOT from solr AND resource type is doc*|ppt*|xls*
                if (ps.getPropertyByPrefix(null, MultiHostSearcher.MULTIHOST_RESOURCE_PROP_NAME) == null
                        && (rt.equals("doc") || rt.equals("ppt") || rt.equals("xls"))) {
                    try {
                        res = repo.retrieve(token, ps.getURI(), true);
                        authorized = checkResourceForEditLink(repo, res, principal);
                    } catch (Exception exception) {
                    }
                }

                edit[i++] = authorized;
            }
        }

        return edit;
    }

    public boolean checkResourceForEditLink(Repository repo, Resource res, Principal principal) {
        if (res.getLock() != null && !res.getLock().getPrincipal().equals(principal)) {
            return false;
        }

        return repo.authorize(principal, res.getAcl(), Privilege.ALL)
                || repo.authorize(principal, res.getAcl(), Privilege.READ_WRITE);

    }
}
