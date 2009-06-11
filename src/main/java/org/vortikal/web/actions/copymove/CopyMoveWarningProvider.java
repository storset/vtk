package org.vortikal.web.actions.copymove;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Acl;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.PropertySelect;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.query.ACLInheritedFromQuery;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.CategorizableReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;


public class CopyMoveWarningProvider implements CategorizableReferenceDataProvider {

    private Repository repository;
    private Service confirmationService;
    private Searcher searcher;
    private Set<?> categories;
    
    
    @SuppressWarnings("unchecked")
    public void referenceData(Map model, HttpServletRequest request)
            throws Exception {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        RequestContext requestContext = RequestContext.getRequestContext();

        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        CopyMoveSessionBean sessionBean = (CopyMoveSessionBean) session.getAttribute(
                CopyMoveToSelectedFolderController.COPYMOVE_SESSION_ATTRIBUTE);
        if (sessionBean == null) {
            return;
        }
        
        Path destinationUri = requestContext.getCurrentCollection();
        Path sourceParentUri = findSourceParentUri(sessionBean);
        if (sourceParentUri == null) {
            return;
        }
        if (sourceParentUri.equals(destinationUri)) {
            // Copying/moving within same folder
            return;
        }

        // A warning is triggered when these conditions are met:
        // 1. sourceParentUri does not have read:all or read-processed:all
        // 2. destParentUri has read:all or read-processed:all
        // 2. exists((uri in (filesToBeCopied)/*) and inherits-from sourceParentUri)
        
        Resource srcAclResource = findNearestAcl(token, sourceParentUri);
        Acl srcAcl = srcAclResource.getAcl();
        if (srcAcl.containsEntry(RepositoryAction.READ, PrincipalFactory.ALL)
                || srcAcl.containsEntry(RepositoryAction.READ_PROCESSED, PrincipalFactory.ALL)) {
            return;
        }

        Resource destAclResource = findNearestAcl(token, destinationUri);
        Acl destAcl = destAclResource.getAcl();
        if (!(destAcl.containsEntry(RepositoryAction.READ, PrincipalFactory.ALL)
                || destAcl.containsEntry(RepositoryAction.READ_PROCESSED, PrincipalFactory.ALL))) {
            return;
        }
        
        if (srcAclResource.getURI().equals(destAclResource.getURI())) {
            return;
        }

        OrQuery orQuery = new OrQuery();
        for (String uri : sessionBean.getFilesToBeCopied()) {
            orQuery.add(new UriPrefixQuery(uri));
        }
        ACLInheritedFromQuery aclQuery = 
            new ACLInheritedFromQuery(srcAclResource.getURI().toString());
        
        AndQuery andQuery = new AndQuery();
        andQuery.add(orQuery);
        andQuery.add(aclQuery);
        
        Search search = new Search();
        search.setSorting(null); 
        search.setQuery(andQuery);
        search.setLimit(1);
        search.setPropertySelect(new PropertySelect() {
            public boolean isIncludedProperty(
                    PropertyTypeDefinition propertyDefinition) {
                return false;
            }});
        ResultSet rs = this.searcher.execute(token, search);
        if (rs.getSize() == 0) {
            return;
        }
        URL url = this.confirmationService.constructURL(destinationUri);
        model.put("resourcesDisclosed", Boolean.TRUE);
        model.put("warningDialogURL", url);
        model.put("action", sessionBean.getAction());
    }

    
    private Resource findNearestAcl(String token, Path uri) throws Exception {
        Resource resource = this.repository.retrieve(token, uri, false);
        if (resource.isInheritedAcl()) {
            return findNearestAcl(token, uri.getParent());
        }
        return resource;
    }

    private Path findSourceParentUri(CopyMoveSessionBean sessionBean) {
        List<String> filesToBeCopied = sessionBean.getFilesToBeCopied();
        if (filesToBeCopied == null || filesToBeCopied.isEmpty()) {
            return null;
        }
        return Path.fromString(filesToBeCopied.get(0)).getParent();
    }


    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setConfirmationService(Service confirmationService) {
        this.confirmationService = confirmationService;
    }

    @Required
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

    public void setCategories(Set<?> categories) {
        this.categories = categories;
    }

    public Set<?> getCategories() {
        if (this.categories == null) {
            return Collections.EMPTY_SET;
        }
        return Collections.unmodifiableSet(this.categories);
    }
}
