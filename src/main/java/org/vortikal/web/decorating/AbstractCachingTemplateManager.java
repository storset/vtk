package org.vortikal.web.decorating;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Required;

public abstract class AbstractCachingTemplateManager implements TemplateManager {
    private TemplateFactory templateFactory;
    
    private Map<String, Template> templatesMap = new ConcurrentHashMap<String, Template>();
    
    @Required public void setTemplateFactory(TemplateFactory templateFactory) {
        this.templateFactory = templateFactory;
    }

    public final Template getTemplate(String name) throws Exception {
        if (name == null) throw new IllegalArgumentException("Name cannot be null");

        if (this.templatesMap.containsKey(name)) {
            return this.templatesMap.get(name);
        }

        TemplateSource templateSource = resolve(name);
        Template template = this.templateFactory.newTemplate(templateSource);
        this.templatesMap.put(name, template);
        return template;
    }
    
    protected abstract TemplateSource resolve(String name);
}
