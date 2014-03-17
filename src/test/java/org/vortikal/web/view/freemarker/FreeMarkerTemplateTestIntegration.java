package org.vortikal.web.view.freemarker;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.Before;

// TODO rename class to *Test
public abstract class FreeMarkerTemplateTestIntegration {
    
    private Configuration cfg;
    private String templateLocation;
    private Map<String, Object> context;
    
    @Before
    public void setUp() throws Exception {
        cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(new File("src/main/ftl"));
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        setTemplateAndContext();
    }
    
    /**
     * Must be implemented by subclass -> set the template and context to test
     */
    protected abstract void setTemplateAndContext();
    
    protected String runTemplate() throws IOException, TemplateException {
        Template template = cfg.getTemplate(templateLocation);
        StringWriter writer = new StringWriter();
        template.process(context, writer);
        return writer.toString();
    }
    
    public void setTemplateLocation(String templateLocation) {
        this.templateLocation = templateLocation;
    }
    
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

}
