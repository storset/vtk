package org.vortikal.web.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.OrderComparator;
import org.vortikal.web.service.Service;



/**
 * Class for looking up groups of services based on categories.
 * 
 */
public class ServiceCategoryResolver {

    /**
     * Gets a list of {@link Service services} declared to belong to a
     * certain category.
     *
     * @param context the Spring application context, used for looking
     * up beans.
     * @param category the category in question
     * @return a list of {@link Service} objects belonging to the
     * category in question. If no such services exist, an empty list
     * is returned.
     */
    public static List getServicesOfCategory(ApplicationContext context, String category) {
        // find all services, and sort out those of category 'category';
        Map matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            context, Service.class, true, false);
    
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
}
