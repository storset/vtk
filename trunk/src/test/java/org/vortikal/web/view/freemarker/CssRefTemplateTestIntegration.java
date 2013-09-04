package org.vortikal.web.view.freemarker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import freemarker.template.TemplateException;


public class CssRefTemplateTestIntegration extends FreeMarkerTemplateTestIntegration {
    
    protected void setTemplateAndContext() {
        setTemplateLocation("layouts/css-ref.ftl");
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("url", "/path/to/css");
        setContext(context);
    }

    public void test() throws IOException, TemplateException {
        String result = runTemplate();
        assertNotNull("Result of templatetransformation is null", result);
        assertFalse("Result of templatetransformation is empty", StringUtils.isEmpty(result));
        String expected = "<link rel=\"stylesheet\" type=\"text/css\" href=\"/path/to/css\" />";
        assertTrue("Result does not contain expected result", result.contains(expected));
    }

}
