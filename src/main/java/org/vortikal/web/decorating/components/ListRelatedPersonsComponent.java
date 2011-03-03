package org.vortikal.web.decorating.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.store.PrincipalMetadataDAO;
import org.vortikal.security.Principal;
import org.vortikal.security.Principal.Type;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.service.Service;
import org.vortikal.web.servlet.ResourceAwareLocaleResolver;

// XXX: hard-coding "dynamic" resource type data from non-Java domain 
public class ListRelatedPersonsComponent extends ViewRenderingDecoratorComponent {

    private PropertyTypeDefinition titlePropDef;
    private ResourceTypeTree resourceTypeTree;
    private PrincipalFactory principalFactory;
    private ResourceAwareLocaleResolver localeResolver;
    private Service displayRelatedPersonsService;

    private PrincipalMetadataDAO principalMetadataDao;

    private String defPonterNumberOfParticipantsToDisplay;
    private String defPointerParticipantsUsernames;
    private String defPointerParticipants;
    
    private String participantNameKey;
    private String participantUrlKey;
    private String ldapPositionKey;
    private String ldapOfficeNumberKey;
    private String ldapEmailKey;
    
    private static final int PARAMETER_LIMIT_DEFAULT = 50;
    private static final String PARAMETER_LIMIT = "limit";
    private static final String PARAMETER_LIMIT_DESC = "Limit number of related persons listed";

    @Override
    public void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();

        Resource currentResource = repository.retrieve(token, uri, true);

        int limit = PARAMETER_LIMIT_DEFAULT;
        if (request.getRawParameter(PARAMETER_LIMIT) != null) {
            try {
                limit = Integer.parseInt(request.getStringParameter(PARAMETER_LIMIT));
                if (limit < 0)
                    limit = 0;
            } catch (NumberFormatException nfe) {
                throw new DecoratorComponentException("Paramter '" + PARAMETER_LIMIT + "' must be an integer");
            }
        }

        List<RelatedPerson> relatedPersons = getRelatedPersons(request.getServletRequest(), requestContext, currentResource,
                limit);

        model.put("relatedPersons", relatedPersons);
        model.put("showAllPersons", displayRelatedPersonsService.constructLink(uri));
        model.put("numberOfParticipantsToDisplay", getShowNumberOfParticipants(currentResource));
        model.put("name",getName());
    }

    private int getShowNumberOfParticipants(Resource resource) {
        if (getDefPonterNumberOfParticipantsToDisplay() == null) {
                return PARAMETER_LIMIT_DEFAULT;
        }
        PropertyTypeDefinition propDef = resourceTypeTree
                .getPropertyDefinitionByPointer(getDefPonterNumberOfParticipantsToDisplay());
        Property prop = resource.getProperty(propDef);
        if (prop != null && prop.getValue() != null)
            return Integer.parseInt(prop.getStringValue());
        return propDef.getDefaultValue().getIntValue();
    }

    public List<RelatedPerson> getRelatedPersons(HttpServletRequest request, RequestContext requestContext, Resource currentResource,
            int limit) {
        Locale currentResourceLocale = this.localeResolver.resolveResourceLocale(request, currentResource.getURI());

        Property participantsUsernamesProp = currentResource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE,
                defPointerParticipantsUsernames);

        List<RelatedPerson> relatedPersons = new ArrayList<RelatedPerson>();

        if (participantsUsernamesProp != null) {
            Value[] participantUsernames = participantsUsernamesProp.getValues();
            relatedPersons = getRelatedPersonsFromUsernames(request, requestContext, currentResourceLocale, participantUsernames);
        }

        // External persons added manually
        Property externalPersonsProp = currentResource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE,
                defPointerParticipants);
        if (externalPersonsProp != null) {
            Value externalPerticipants[] = externalPersonsProp.getValues();
            for (int i = 0; i < externalPerticipants.length; i++) {
                JSONObject externalParticipant = JSONObject.fromObject(externalPerticipants[i].getStringValue());
                String participantName = null;
                if (externalParticipant.containsKey(participantNameKey)) {
                    participantName = externalParticipant.getString(participantNameKey);
                }
                String participantUrl = null;
                if (externalParticipant.containsKey(getParticipantUrlKey())) {
                    participantUrl = externalParticipant.getString(getParticipantUrlKey());
                }
                if (StringUtils.isBlank(participantName)) {
                    continue; // No name for external user => skip.
                }

                RelatedPerson p = new RelatedPerson(null, participantName, participantUrl);
                relatedPersons.add(p);
            }
        }

        // ..and apply listing limit
        if (relatedPersons.size() > limit) {
            relatedPersons = relatedPersons.subList(0, limit);
        }
        return relatedPersons;
    }

    private List<RelatedPerson> getRelatedPersonsFromUsernames(HttpServletRequest request, RequestContext requestContext, Locale locale,
            Value[] participantUsernames) {

        // Three sources of related persons that need to be consolidated:
        // 1. Person documents on host
        // 2. Principal info for usernames without person documents
        // 3. External persons manually added to project

        Set<String> usernamesWithDocument = new HashSet<String>();
        List<RelatedPerson> relatedPersons = getRelatedPersonsFromDocuments(request, requestContext, locale,
                participantUsernames);
        for (RelatedPerson p : relatedPersons) {
            usernamesWithDocument.add(p.username);
        }

        // Supplement with basic principal data for users with no person
        // document
        for (int i = 0; i < participantUsernames.length; i++) {
            String username = participantUsernames[i].getStringValue();
            if (!usernamesWithDocument.contains(username)) {
                // Missing document for username, populate with data from
                // principal manager
                try {
                    Principal principal = principalFactory.getPrincipal(username, Type.USER);

                    // sorting depends on the fact that users in relatedPersons
                    // is in the same order as participantUsernames
                    String position = (String) principal.getMetadata().getValue(ldapPositionKey);
                    String officeNumber = (String) principal.getMetadata().getValue(ldapOfficeNumberKey);
                    String email = (String) principal.getMetadata().getValue(ldapEmailKey);

                    relatedPersons.add(i, new RelatedPerson(username, principal.getDescription(), principal.getURL(),
                            position, officeNumber, email, null));
                } catch (Exception ip) {
                } // Skip invalid
            }
        }

        return relatedPersons;
    }

    private List<RelatedPerson> getRelatedPersonsFromDocuments(HttpServletRequest request, 
            RequestContext requestContext, Locale locale,
            Value[] participantUsernames) {
        PropertyTypeDefinition usernamePropDef = resourceTypeTree.getPropertyTypeDefinition(
                Namespace.STRUCTURED_RESOURCE_NAMESPACE, "username");
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        // Set up search
        AndQuery mainQuery = new AndQuery();
        mainQuery.add(new TypeTermQuery("person", TermOperator.IN));

        OrQuery participantsQuery = new OrQuery();
        for (Value value : participantUsernames) {
            participantsQuery.add(new PropertyTermQuery(usernamePropDef, value.getStringValue(), TermOperator.EQ));
        }
        mainQuery.add(participantsQuery);

        ConfigurablePropertySelect select = new ConfigurablePropertySelect();
        select.addPropertyDefinition(this.titlePropDef);
        select.addPropertyDefinition(usernamePropDef);

        Search search = new Search();
        search.setQuery(mainQuery);
        search.setSorting(null);
        search.setLimit(1000);
        search.setPropertySelect(select);

        // Find all person documents for set of usernames, no sorting necessary.
        ResultSet rs = repository.search(token, search);

        // Map single username to list of docs for that user.
        Map<String, List<PropertySet>> usernameDocs = new HashMap<String, List<PropertySet>>();

        for (PropertySet resource : rs.getAllResults()) {
            String username = resource.getProperty(usernamePropDef).getStringValue();
            List<PropertySet> docs = usernameDocs.get(username);
            if (docs == null) {
                docs = new ArrayList<PropertySet>();
                usernameDocs.put(username, docs);
            }
            docs.add(resource);
        }

        // For earch user, select "correct" person doc and add data to list of
        // related persons
        List<RelatedPerson> retval = new ArrayList<RelatedPerson>();

        for (int i = 0; i < participantUsernames.length; i++) {
            PropertySet personDoc = null;
            List<PropertySet> docs = usernameDocs.get(participantUsernames[i].getStringValue());
            if (docs != null) {
                personDoc = selectPersonDocument(request, locale, docs);
            }
            if (personDoc != null) {
                String title = personDoc.getProperty(this.titlePropDef).getStringValue();
                String url = personDoc.getURI().toString();

                PropertyTypeDefinition resourcePosition = resourceTypeTree
                        .getPropertyDefinitionByPointer("resource:position");
                PropertyTypeDefinition resourceEmail = resourceTypeTree
                        .getPropertyDefinitionByPointer("resource:email");
                PropertyTypeDefinition resourcePhone = resourceTypeTree
                        .getPropertyDefinitionByPointer("resource:phone");
                PropertyTypeDefinition resourceImage = resourceTypeTree
                        .getPropertyDefinitionByPointer("resource:picture");

                Resource currentResource = null;
                try {
                    currentResource = repository.retrieve(token, personDoc.getURI(), false);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                Property phoneProperty = currentResource.getProperty(resourcePhone);
                Property emailProperty = currentResource.getProperty(resourceEmail);
                Property positionProperty = currentResource.getProperty(resourcePosition);
                Property imageProperty = currentResource.getProperty(resourceImage);

                String email = null;
                if (emailProperty != null && emailProperty.getValues() != null)
                    email = emailProperty.getFormattedValue();

                String phone = null;
                if (phoneProperty != null && phoneProperty.getValues() != null)
                    phone = phoneProperty.getFormattedValue();

                String position = null;
                if (positionProperty != null && positionProperty.getValue() != null)
                    position = positionProperty.getStringValue();

                String image = null;
                if (imageProperty != null && imageProperty.getValue() != null)
                    image = imageProperty.getStringValue();

                RelatedPerson p = new RelatedPerson(participantUsernames[i].getStringValue(), title, url, position,
                        phone, email, image);
                retval.add(p);
            }
        }

        return retval;
    }

    // Choose *one* of N docs to show for a single person.
    // Differentiating only between English and non-English locale because nn
    // (nynorsk) locale
    // doesn't directly match bokmål locale when using Locale.equals(Object).
    private PropertySet selectPersonDocument(HttpServletRequest request, Locale desiredLocale, List<PropertySet> docs) {

        boolean englishLocaleDesired = desiredLocale.getLanguage().contains("en");
        for (PropertySet resource : docs) {
            Locale locale = this.localeResolver.resolveResourceLocale(request, resource.getURI());
            if (englishLocaleDesired) {
                if (locale.getLanguage().contains("en"))
                    return resource; // Got one with correct locale, select that
                // one.
            } else {
                if (!locale.getLanguage().contains("en")) {
                    return resource;
                }
            }
        }

        // No doc with desired locale found, just return first one found.
        if (docs.size() > 0) {
            return docs.get(0);
        } else
            throw new IllegalArgumentException("Empty list of docs");
    }

    protected String getDescriptionInternal() {
        return "Inserts a list of related persons for a project document";
    }

    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PARAMETER_LIMIT, PARAMETER_LIMIT_DESC);
        return map;
    }

    @Required
    public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

    @Required
    public void setLocaleResolver(ResourceAwareLocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    public void setPrincipalMetadataDao(PrincipalMetadataDAO principalMetadataDao) {
        this.principalMetadataDao = principalMetadataDao;
    }

    public PrincipalMetadataDAO getPrincipalMetadataDao() {
        return principalMetadataDao;
    }

    public void setDisplayRelatedPersonsService(Service displayRelatedPersonsService) {
        this.displayRelatedPersonsService = displayRelatedPersonsService;
    }

    public Service getDisplayRelatedPersonsService() {
        return displayRelatedPersonsService;
    }

    // public void setDefaultNumberOfParticipantsToDisplay(int
    // defaultNumberOfParticipantsToDisplay) {
    // this.defaultNumberOfParticipantsToDisplay =
    // defaultNumberOfParticipantsToDisplay;
    // }
    //
    // public int getDefaultNumberOfParticipantsToDisplay() {
    // return defaultNumberOfParticipantsToDisplay;
    // }

        public void setDefPonterNumberOfParticipantsToDisplay(String defPonterNumberOfParticipantsToDisplay) {
            this.defPonterNumberOfParticipantsToDisplay = defPonterNumberOfParticipantsToDisplay;
        }
    
        public String getDefPonterNumberOfParticipantsToDisplay() {
            return defPonterNumberOfParticipantsToDisplay;
        }

    public void setDefPointerParticipantsUsernames(String defPointerParticipantsUsernames) {
        this.defPointerParticipantsUsernames = defPointerParticipantsUsernames;
    }

    public String getDefPointerParticipantsUsernames() {
        return defPointerParticipantsUsernames;
    }

    public void setDefPointerParticipants(String defPointerParticipants) {
        this.defPointerParticipants = defPointerParticipants;
    }

    public String getDefPointerParticipants() {
        return defPointerParticipants;
    }

    public void setParticipantNameKey(String participantNameKey) {
        this.participantNameKey = participantNameKey;
    }

    public String getParticipantNameKey() {
        return participantNameKey;
    }

    public void setLdapPositionKey(String ldapPositionKey) {
        this.ldapPositionKey = ldapPositionKey;
    }

    public String getLdapPositionKey() {
        return ldapPositionKey;
    }

    public void setLdapEmailKey(String ldapEmailKey) {
        this.ldapEmailKey = ldapEmailKey;
    }

    public String getLdapEmailKey() {
        return ldapEmailKey;
    }

    public void setParticipantUrlKey(String participantUrlKey) {
        this.participantUrlKey = participantUrlKey;
    }

    public String getParticipantUrlKey() {
        return participantUrlKey;
    }

    public void setLdapOfficeNumberKey(String ldapOfficeNumberKey) {
        this.ldapOfficeNumberKey = ldapOfficeNumberKey;
    }

    public String getLdapOfficeNumberKey() {
        return ldapOfficeNumberKey;
    }

    public static class RelatedPerson implements Comparable<RelatedPerson> {
        private String username;
        private String name;
        private String url;
        private String position;
        private String phonenumber;
        private String email;
        private String image;

        RelatedPerson(String username, String name, String url, Object position, Object phonenumber, Object email,
                String image) {
            if (name == null)
                throw new IllegalArgumentException("name cannot be null");
            this.username = username;
            this.name = name;
            this.url = url;
            if (position != null)
                this.position = position.toString();
            if (phonenumber != null)
                this.phonenumber = phonenumber.toString();
            if (email != null)
                this.email = email.toString();
            this.image = image;
        }

        RelatedPerson(String username, String name, String url) {
            if (name == null)
                throw new IllegalArgumentException("name cannot be null");
            this.username = username;
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return this.name;
        }

        public String getUrl() {
            return this.url;
        }

        public String getUsername() {
            return this.username;
        }

        public String getPosition() {
            return position;
        }

        public String getPhonenumber() {
            return phonenumber;
        }

        public String getEmail() {
            return email;
        }

        public String getImage() {
            return image;
        }

        public int compareTo(RelatedPerson o) {
            return this.name.compareTo(o.name);
        }
    }

}
