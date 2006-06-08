package org.vortikal.security.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.GroupStore;
import org.vortikal.security.Principal;

public class DomainGroupStore implements GroupStore {

    private int order = Integer.MAX_VALUE;
    
    private List knownDomains = new ArrayList();
    private String groupName = "alle";
    
    public boolean validateGroup(Principal group)
            throws AuthenticationProcessingException {
        if (groupName.equals(group.getUnqualifiedName())
                && knownDomains.contains(group.getDomain()))
            return true;
        return false;
    }

    public boolean isMember(Principal principal, Principal group) {
        if (validateGroup(group)) {
            String pDomain = principal.getDomain();
            String gDomain = group.getDomain();
            if ((pDomain == null && gDomain == null) || pDomain.equals(gDomain)) 
                return true;
        }
        
        return false;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setKnownDomains(String[] knownDomains) {
        this.knownDomains = Arrays.asList(knownDomains);
    }

}
