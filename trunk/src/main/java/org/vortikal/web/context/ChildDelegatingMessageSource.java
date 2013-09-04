/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.web.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

/**
 * Message source delegating to all other message sources found in the application context.
 * Use with caution, since circular resolving will occur if other message source instances delegates
 * back to this one.
 * Additionally, the children is not ordered, so no guarantee is made for duplicate message codes.
 *
 */
public class ChildDelegatingMessageSource 
    implements MessageSource, InitializingBean, ApplicationContextAware, BeanNameAware {

    private String beanName;
    private ApplicationContext applicationContext;
    private List<MessageSource> children;
    private Map<String, Locale> localeTranslationMap = new HashMap<String, Locale>();
    
    public String getMessage(String code, Object[] args, String defaultMessage,
            Locale locale) {
        try {
            locale = mapLocale(locale);
            return getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            return defaultMessage;
        }
    }

    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        for (MessageSource messageSource: this.children) {
            try {
                locale = mapLocale(locale);
                return messageSource.getMessage(code, args, locale);
            } catch (NoSuchMessageException e) {
            }
        }
        throw new NoSuchMessageException(code);
    }

    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        String[] codes = resolvable.getCodes();
        locale = mapLocale(locale);
        if (codes == null) {
            throw new NoSuchMessageException(null, locale);
        }
        for (String code: codes) {
            try {
                return getMessage(code, resolvable.getArguments(), locale);
            } catch (NoSuchMessageException e) {}
        }
        if (resolvable.getDefaultMessage() != null) {
            return resolvable.getDefaultMessage();
        }
        throw new NoSuchMessageException(codes.length > 0 ? codes[codes.length - 1] : null, locale);
    }

    public void afterPropertiesSet() throws Exception {
        // find all services, and sort out those of category 'category';
        Map<String, MessageSource> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            this.applicationContext, MessageSource.class, true, false);

        matchingBeans.remove(this.beanName);
        
        this.children = new ArrayList<MessageSource>(matchingBeans.values());
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public void setLocaleTranslationMap(Map<String, Locale> localeTranslationMap) {
        this.localeTranslationMap = localeTranslationMap;
    }

    private Locale mapLocale(Locale locale) {
        if (this.localeTranslationMap != null && this.localeTranslationMap.containsKey(locale.toString())) {
            locale = this.localeTranslationMap.get(locale.toString());
        }
        return locale;
    }
    
}
