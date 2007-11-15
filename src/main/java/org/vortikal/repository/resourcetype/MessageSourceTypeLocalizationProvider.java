package org.vortikal.repository.resourcetype;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.MessageSource;

public class MessageSourceTypeLocalizationProvider implements  
                                            TypeLocalizationProvider  {

    public static final String PROPERTY_TYPE_NAME_KEY_PREFIX = "proptype.name.";
    public static final String RESOURCE_TYPE_NAME_KEY_PREFIX = "resourcetype.name.";
    
    private MessageSource messageSource;

    public String getLocalizedPropertyName(PropertyTypeDefinition def,
            Locale locale) {

        String key = null;
        String prefix = def.getNamespace().getPrefix();
        String name = def.getName();
        if (prefix != null){
            key = PROPERTY_TYPE_NAME_KEY_PREFIX + prefix + ":" + name;
        } else {
            key = PROPERTY_TYPE_NAME_KEY_PREFIX + name;
        }
        
        return this.messageSource.getMessage(key, null, name, locale);
    }

    public String getLocalizedResourceTypeName(ResourceTypeDefinition def,
            Locale locale) {

        String key = null;
        String prefix = def.getNamespace().getPrefix();
        String name = def.getName();
        if (prefix != null){
            key = RESOURCE_TYPE_NAME_KEY_PREFIX + prefix + ":" + name;
        } else {
            key = RESOURCE_TYPE_NAME_KEY_PREFIX + name;
        }
        
        return this.messageSource.getMessage(key, null, name, locale);
    }
    
    @Required
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

}
