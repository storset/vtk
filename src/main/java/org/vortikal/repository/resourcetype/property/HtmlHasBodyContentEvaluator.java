package org.vortikal.repository.resourcetype.property;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlUtil;

public class HtmlHasBodyContentEvaluator implements PropertyEvaluator {

    private HtmlUtil htmlUtil;
    private PropertyTypeDefinition characterEncodingPropDef;

    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

        property.setBooleanValue(false);
        try {
            String encoding = ctx.getNewResource().getProperty(
                    this.characterEncodingPropDef).getStringValue();
            InputStream in = ctx.getContent().getContentInputStream();
            HtmlPage page = this.htmlUtil.parse(in, encoding);
            HtmlElement body = page.selectSingleElement("html.body");
            if (body != null) {
                String bodyContent = this.htmlUtil.flatten(body);
                if (!bodyContent.trim().equals("")) {
                    property.setBooleanValue(true);
                }
            }
        } catch (Exception e) {
            // Unable to parse HTML document, 
            // assume no body. 
        }
        return true;
    }
    
    @Required public void setHtmlUtil(HtmlUtil htmlUtil) {
        this.htmlUtil = htmlUtil;
    }
    
    @Required public void setCharacterEncodingPropDef(PropertyTypeDefinition characterEncodingPropDef) {
        this.characterEncodingPropDef = characterEncodingPropDef;
    }

}
