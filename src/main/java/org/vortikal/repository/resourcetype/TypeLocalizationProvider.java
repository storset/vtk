package org.vortikal.repository.resourcetype;

import java.util.Locale;

public interface TypeLocalizationProvider {

    /**
     * Return a localized name of property type def.
     * <code>Locale</code>.
     * 
     * @param namespace
     * @param name
     * @param locale
     * @return The name of the property- or resource-type as a localized string.
     *         If the type is unknown or a translation is missing for
     *         the supplied locale, the canonical type name is
     *         returned.
     */
    public String getLocalizedPropertyName(PropertyTypeDefinition def,
                                           Locale locale);
    
    /**
     * 
     * @param def
     * @param locale
     * @return
     */
    public String getLocalizedResourceTypeName(ResourceTypeDefinition def,
                                               Locale locale);

}
