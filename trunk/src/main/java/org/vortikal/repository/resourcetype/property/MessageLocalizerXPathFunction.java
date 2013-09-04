/* Copyright (c) 2007, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.repository.resourcetype.property;

import java.util.List;
import java.util.Locale;

import org.jaxen.Context;
import org.jaxen.FunctionCallException;
import org.jaxen.Navigator;
import org.jaxen.function.StringFunction;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.util.repository.LocaleHelper;
import org.vortikal.xml.xpath.AbstractXPathFunction;


public class MessageLocalizerXPathFunction extends AbstractXPathFunction
  implements InitializingBean {

    private MessageSource messageSource;
    private Locale defaultLocale;
    private PropertyTypeDefinition localePropertyDefinition;
    
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }
    
    public void setLocalePropertyDefinition(PropertyTypeDefinition localePropertyDefinition) {
        this.localePropertyDefinition = localePropertyDefinition;
    }

    public void afterPropertiesSet() {
        if (this.messageSource == null) {
            throw new BeanInitializationException("JavaBean property 'messageSource' not set");
        }
        if (this.defaultLocale == null) {
            throw new BeanInitializationException("JavaBean property 'defaultLocale' not set");
        }
        if (this.localePropertyDefinition == null) {
            throw new BeanInitializationException("JavaBean property 'localePropertyDefinition' not set");
        }

    }

    @SuppressWarnings("rawtypes")
    public Object call(Context context, List args) throws FunctionCallException {

        if (args.isEmpty()) {
            throw new FunctionCallException("This function takes one argument");
        }

        try {
            Navigator navigator = context.getNavigator();
            String value = StringFunction.evaluate(args.get(0), navigator);

            Locale locale = this.defaultLocale;
            PropertySet resource = (PropertySet) context.getVariableValue("vrtx", null, "resource");
            if (resource != null) {
                Property localeProperty = resource.getProperty(this.localePropertyDefinition);
                if (localeProperty != null) {
                    locale = LocaleHelper.getLocale(localeProperty.getStringValue());
                }
            }
            String localized = this.messageSource.getMessage(value, new Object[0], value, locale);
            return localized;

        } catch (Throwable t) {
            throw new FunctionCallException(t);
        }

    }

}

