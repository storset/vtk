package org.vortikal.web.decorating.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;

public class ListResourceTitleComponent extends ViewRenderingDecoratorComponent {

    private String multipleResourceRefField;

    @Override
    public void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Resource resource = repository.retrieve(token, uri, true);

        Property resourceRefProp = resource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE,
                getMultipleResourceRefField());

        if (requestContext.isPlainServiceMode()) { // VTK-2460
            token = null;
        }
        
        List<RelatedDocument> relatedDocuments = new ArrayList<RelatedDocument>();
        if (resourceRefProp != null && resourceRefProp.getValues() != null) {
            for (Value x : resourceRefProp.getValues()) {
                try {
                    Path p = Path.fromString(x.getStringValue());
                    Resource currentResource = repository.retrieve(token, p, true);
                    relatedDocuments.add(new RelatedDocument(currentResource.getTitle(), p.toString()));
                } catch (Exception e) {
                    // ignore exceptions
                }
            }
        }
        model.put("realtedDocuments", relatedDocuments);
        model.put("viewName", this.getName());
    }

    public void setMultipleResourceRefField(String multipleResourceRefField) {
        this.multipleResourceRefField = multipleResourceRefField;
    }

    public String getMultipleResourceRefField() {
        return multipleResourceRefField;
    }

    public static class RelatedDocument implements Comparable<RelatedDocument> {
        private String title;
        private String url;

        RelatedDocument(String title, String url) {
            if (title == null)
                throw new IllegalArgumentException("title cannot be null");
            this.title = title;
            this.url = url;
        }

        public String getTitle() {
            return this.title;
        }

        public String getUrl() {
            return this.url;
        }

        public int compareTo(RelatedDocument o) {
            return this.title.compareTo(o.title);
        }
    }
}
