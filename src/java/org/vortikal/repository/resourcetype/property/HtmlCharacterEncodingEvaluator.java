package org.vortikal.repository.resourcetype.property;

import java.util.Date;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.security.Principal;
import org.vortikal.util.text.HtmlUtil;

public class HtmlCharacterEncodingEvaluator implements CreatePropertyEvaluator,
        ContentModificationPropertyEvaluator {

    public static final String DEFAULT_CHARSET = "ISO-8859-1";

    private String defaultCharset = DEFAULT_CHARSET;
    
    public boolean create(Principal principal, Property property, 
            PropertySet ancestorPropertySet, boolean isCollection, Date time)
    throws PropertyEvaluationException {
        property.setStringValue(defaultCharset);
        return true;
    }
    
    public boolean contentModification(Principal principal, Property property,
            PropertySet ancestorPropertySet, Content content, Date time)
            throws PropertyEvaluationException {
        byte[] bytes = null;
        
        try {
            bytes =(byte[])content.getContentRepresentation(byte[].class);
        } catch (Exception e) {
            throw new PropertyEvaluationException(
                    "Unable to get byte[] representation of content", e);
        }
        
        String characterEncoding =
            HtmlUtil.getCharacterEncodingFromBody(bytes);

        if (characterEncoding == null)
            characterEncoding = this.defaultCharset;

        property.setStringValue(characterEncoding);
        return true;
    }


    
    
    public String getDefaultCharset() {
        return defaultCharset;
    }

    public void setDefaultCharset(String defaultCharset) {
        this.defaultCharset = defaultCharset;
    }

}
