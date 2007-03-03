package org.vortikal.web.view.decorating.components;

import java.io.Writer;

import org.springframework.beans.factory.BeanInitializationException;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;

public class ResourcePropertiesDecoratorCompontent extends AbstractDecoratorComponent {
    private static final String URL_IDENTIFIER = "url";
    private static final String NAME_IDENTIFIER = "name";
    private static final String TYPE_IDENTIFIER = "type";
    private static final String URI_IDENTIFIER = "uri";

    private Repository repository;
    private boolean forProcessing = true;
    private ValueFormatter valueFormatter;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void render(DecoratorRequest request, DecoratorResponse response) throws Exception {
        String token = SecurityContext.getSecurityContext().getToken();
        String uri = RequestContext.getRequestContext().getResourceURI();
        
        Resource resource = repository.retrieve(token, uri, this.forProcessing );

        String id = request.getStringParameter("id");
        String result = null;
        
        if (id == null || id.trim().equals("")) {
            return;
        }
        
        if (URI_IDENTIFIER.equals(id)) {
            result = uri;
        } else if (NAME_IDENTIFIER.equals(id)) {
            result = resource.getName();
        } else if (TYPE_IDENTIFIER.equals(id)) {
            result = resource.getResourceType();
        } else if (URL_IDENTIFIER.equals(id)) {
            return;
        } else {
            String namespace = null;
            String name = null;

            int i = id.indexOf(":");
            if (i < 0) {
                name = id;
            } else if (i == 0 || i == id.length() - 1) {
                // XXX: throw something 
                return;
            } else {
                namespace = id.substring(0, i - 1);
                name = id.substring(i + 1);
            }

            Property prop = resource.getProperty(Namespace.getNamespaceFromPrefix(namespace), name);
        
            if (prop == null) {
                return;
            }
            Value value = prop.getValue();
            result = this.valueFormatter.valueToString(value, null, request.getLocale());
        }   
        
        Writer writer = response.getWriter();
        try {
            writer.write(result);
        } finally {
            writer.close();
        }
    }

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        
        if (this.repository == null) {
            throw new BeanInitializationException("JavaBean property 'repository' not set");
        }
        if (this.valueFormatter == null) {
            throw new BeanInitializationException(
                "JavaBean property 'valueFormatter' not set");
        }
    }

    public void setForProcessing(boolean forProcessing) {
        this.forProcessing = forProcessing;
    }

    public void setValueFormatter(ValueFormatter valueFormatter) {
        this.valueFormatter = valueFormatter;
    }

    
}
