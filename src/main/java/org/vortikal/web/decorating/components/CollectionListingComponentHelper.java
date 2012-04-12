package org.vortikal.web.decorating.components;

import java.util.List;

import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.search.Listing;

public class CollectionListingComponentHelper {

    // XXX FIXME!!!
    // Indices of return array must match indices of each property set in each
    // result set in each listing!!!
    public boolean[] isAuthorized(Repository repo, String token, Principal principal, int maxItems, List<Listing> ll)
            throws Exception {
        Resource res;
        int i = 0;
        boolean[] edit = new boolean[maxItems];
        for (Listing l : ll) {
            for (PropertySet ps : l.getFiles()) {
              res = repo.retrieve(token, ps.getURI(), false);
              edit[i++] = this.isAuthorized(repo, res, principal);                
            }

        }
        return edit;
    }

   public boolean isAuthorized(Repository repo, Resource res, Principal principal) {
     try {
       return repo.isAuthorized(res, RepositoryAction.READ_WRITE, principal, true);
     } catch(Exception e)  {
       return false;
     }
   }

}
