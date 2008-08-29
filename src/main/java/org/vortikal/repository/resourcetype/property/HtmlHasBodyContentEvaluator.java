package org.vortikal.repository.resourcetype.property;

import java.io.InputStream;
import java.util.Date;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.htmlparser.HtmlUtil;

public class HtmlHasBodyContentEvaluator implements
        ContentModificationPropertyEvaluator {

    private HtmlUtil htmlUtil;
    private PropertyTypeDefinition characterEncodingPropDef;

    public boolean contentModification(Principal principal, Property property,
            PropertySet ancestorPropertySet, Content content, Date time)
            throws PropertyEvaluationException {

        property.setBooleanValue(false);
        try {
            String encoding = ancestorPropertySet.getProperty(
                    this.characterEncodingPropDef).getStringValue();
            InputStream in = content.getContentInputStream();
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
