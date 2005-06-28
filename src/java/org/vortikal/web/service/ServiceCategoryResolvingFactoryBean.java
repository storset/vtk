package org.vortikal.web.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.vortikal.web.service.Service;



/**
 * Factory bean looking up groups of services based on category.
 * 
 * Configurable bean property:
 * 
 * <ul>
 * <li><code>category</code> - String describing the category of the services
 * 
 * TODO: generalize
 */
public class ServiceCategoryResolvingFactoryBean implements ApplicationContextAware, FactoryBean {

    private ApplicationContext applicationContext;
    private String category;
    
    /**
     * Gets a list of {@link Service services} declared to belong to a
     * certain category.
     *
     * @param category the category in question
     * @return a list of {@link Service} objects belonging to the
     * category in question. If no such services exist, an empty list
     * is returned.
     */
    private List getServicesOfCategory(String category) {
        // find all services, and sort out those of category 'category';
        Map matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            applicationContext, Service.class, true, false);
    
        List services = new ArrayList(matchingBeans.values());
        List list = new ArrayList(services);
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Service service = (Service) iter.next();
            if (service.getCategories() == null 
                || !service.getCategories().contains(category)) 
                services.remove(service);
        }
        Collections.sort(services, new OrderComparator());
        return services;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Object getObject() throws Exception {
        return getServicesOfCategory(category).toArray(new Service[0]);
    }

    public Class getObjectType() {
        return new Service[0].getClass();
    }

    public boolean isSingleton() {
        return true;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
