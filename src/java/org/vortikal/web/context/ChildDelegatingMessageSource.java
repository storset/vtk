package org.vortikal.web.context;

import java.util.ArrayList;
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
    private MessageSource[] children;
    
    public String getMessage(String code, Object[] args, String defaultMessage,
            Locale locale) {
        try {
            return getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            return defaultMessage;
        }
    }

    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        for (int i = 0; i < children.length; i++) {
            MessageSource messageSource = children[i];
            try {
                return messageSource.getMessage(code, args, locale);
            } catch (NoSuchMessageException e) {
            }
        }
        throw new NoSuchMessageException(code);
    }

    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        String[] codes = resolvable.getCodes();
        if (codes == null) {
            throw new NoSuchMessageException(null, locale);
        }
        for (int i = 0; i < codes.length; i++) {
            try {
                return getMessage(codes[i], resolvable.getArguments(), locale);
            } catch (NoSuchMessageException e) {}
        }
        if (resolvable.getDefaultMessage() != null) {
            return resolvable.getDefaultMessage();
        }
        throw new NoSuchMessageException(codes.length > 0 ? codes[codes.length - 1] : null, locale);
    }

    public void afterPropertiesSet() throws Exception {
        // find all services, and sort out those of category 'category';
        Map matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            this.applicationContext, MessageSource.class, true, false);

        matchingBeans.remove(this.beanName);
        
        this.children = (MessageSource[]) new ArrayList(matchingBeans.values()).toArray(new MessageSource[0]);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

}
