package org.vortikal.web.decorating.components;

import java.util.List;

import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.search.Listing;

public class CollectionListingComponentHelper {
    
    public boolean[] isAuthorized(Repository r, String token, Principal principal, int maxItems, List<Listing> ll) throws Exception {
        Resource res;
        String rt;
        PropertySet ps;
        boolean[] edit = new boolean[maxItems];
        int i = 0;
        for (Listing l : ll) {
            for (; i < l.getFiles().size(); i++) {
                ps = l.getFiles().get(i);
                rt = ps.getResourceType();
                if (rt.equals("doc") || rt.equals("ppt") || rt.equals("xls")) {
                    res = r.retrieve(token, ps.getURI(), false);
                    edit[i] = r.isAuthorized(res, RepositoryAction.READ_WRITE, principal, true);
                } else
                    edit[i] = false;
            }

        }
        return edit;
    }
}