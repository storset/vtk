package org.vortikal.web;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Acl;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.web.service.Service;

public class ACLTooltipHelper {
   
    private Service permissionsService;
    private Repository repository;
    private org.springframework.web.servlet.support.RequestContext springRequestContext;
    
    public String generateTitle(Resource r, HttpServletRequest request) {
        return generateTitle(r, null, request);
    }

    public String generateTitle(Resource r, String name, HttpServletRequest request) {
        RequestContext requestContext = RequestContext.getRequestContext();
        
        Acl acl = r.getAcl();
        boolean authorizedToRead = authorizedTo(acl, requestContext.getPrincipal(), Privilege.READ);
        boolean authorizedToAdmin = authorizedTo(acl, requestContext.getPrincipal(), Privilege.ALL);
        
        StringBuilder title = new StringBuilder();
        this.springRequestContext = new org.springframework.web.servlet.support.RequestContext(request);

        title.append("<span id=&quot;title-wrapper&quot;>");
        if(name != null) {
            title.append("<strong id=&quot;title&quot;>" + name + "</strong>");
        }
        if (r.isInheritedAcl()) {
            title.append(" " + getLocalizedTitle(request, "report.list-resources.inherited-permissions", null));
            genEditOrViewButton(request, r, authorizedToAdmin, authorizedToRead, title);
            title.append("</span><span class=&quot;inherited-permissions&quot;>");
        } else {
            title.append(" " + getLocalizedTitle(request, "report.list-resources.own-permissions", null));
            genEditOrViewButton(request, r, authorizedToAdmin, authorizedToRead, title);
            title.append("</span>");
        }

        // Generate table with permissions
        String notAssigned = getLocalizedTitle(request, "permissions.not.assigned", null).toLowerCase();
        title.append("<table><tbody>");

        String[] aclFormatted = getAclFormatted(acl, request);

        String read = aclFormatted[0].isEmpty() ? notAssigned : aclFormatted[0];
        title.append("<tr><td>" + getLocalizedTitle(request, "permissions.privilege.read", null)
                + ":</td><td>" + read + "</td></tr>");

        String write = aclFormatted[1].isEmpty() ? notAssigned : aclFormatted[1];
        title.append("<tr><td>" + getLocalizedTitle(request, "permissions.privilege.read-write", null)
                + ":</td><td>" + write + "</td></tr>");

        String admin = aclFormatted[2].isEmpty() ? notAssigned : aclFormatted[2];
        title.append("<tr><td>"
                + getLocalizedTitle(request, "report.list-resources.admin-permission", null)
                + ":</td><td>" + admin + "</td></tr>");

        title.append("</tbody></table>");

        if (r.isInheritedAcl()) {
            title.append("</span>");
        }
        
        return title.toString();
    }
    
    public boolean authorizedTo(Acl acl, Principal principal, Privilege privilege) {
        return repository.authorize(principal, acl, privilege);
    }

    private void genEditOrViewButton(HttpServletRequest request, Resource r, boolean authorizedToAdmin,
            boolean authorizedToRead, StringBuilder title) {
        String uriService = this.permissionsService.constructURL(r.getURI()).getPathRepresentation();
        if (authorizedToAdmin) {
            title.append("&nbsp;&nbsp;<a class=&quot;vrtx-button-small&quot; href=&quot;" + uriService
                    + "&quot;><span>" + getLocalizedTitle(request, "report.list-resources.edit", null)
                    + "</span></a>");
        } else if (authorizedToRead) {
            title.append("&nbsp;&nbsp;<a class=&quot;vrtx-button-small&quot; href=&quot;" + uriService
                    + "&quot;><span>" + getLocalizedTitle(request, "report.list-resources.view", null)
                    + "</span></a>");
        }
    }

    private String[] getAclFormatted(Acl acl, HttpServletRequest request) {
        String[] aclFormatted = { "", "", "" }; // READ, READ_WRITE, ALL

        for (Privilege action : Privilege.values()) {
            String actionName = action.getName();
            Principal[] privilegedUsers = acl.listPrivilegedUsers(action);
            Principal[] privilegedGroups = acl.listPrivilegedGroups(action);
            Principal[] privilegedPseudoPrincipals = acl.listPrivilegedPseudoPrincipals(action);
            StringBuilder combined = new StringBuilder();
            int i = 0;
            int len = privilegedPseudoPrincipals.length + privilegedUsers.length + privilegedGroups.length;
            boolean all = false;

            for (Principal p : privilegedPseudoPrincipals) {
                String pseudo = this.getLocalizedTitle(request, "pseudoPrincipal." + p.getName(), null);
                if (p.getName() == PrincipalFactory.NAME_ALL) {
                    all = true;
                    combined.append(pseudo);
                }
                if ((len == 1 || i == len - 1) && !all) {
                    combined.append(pseudo);
                } else if (!all) {
                    combined.append(pseudo + ", ");
                }
                i++;
            }
            if (!all) {
                for (Principal p : privilegedUsers) {
                    if (len == 1 || i == len - 1) {
                        combined.append(p.getDescription());
                    } else {
                        combined.append(p.getDescription() + ", ");
                    }
                    i++;
                }
                for (Principal p : privilegedGroups) {
                    if (len == 1 || i == len - 1) {
                        combined.append(p.getDescription());
                    } else {
                        combined.append(p.getDescription() + ", ");
                    }
                    i++;
                }
            }
            if (actionName == "read") {
                aclFormatted[0] = combined.toString();
            } else if (actionName == "read-write") {
                aclFormatted[1] = combined.toString();
            } else if (actionName == "all") {
                aclFormatted[2] = combined.toString();
            }
        }
        return aclFormatted;
    }

    private String getLocalizedTitle(HttpServletRequest request, String key, Object[] params) {
        if (params != null) {
            return this.springRequestContext.getMessage(key, params);
        }
        return this.springRequestContext.getMessage(key);
    }
    

    @Required
    public void setPermissionsService(Service permissionsService) {
        this.permissionsService = permissionsService;
    }
    
    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
}
