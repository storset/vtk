package org.vortikal.web.referencedataprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.OrderComparator;
import org.vortikal.web.service.Service;

public class ServiceCategoryResolver {

    public static List getServicesOfCategory(ApplicationContext context, String category) {
        // find all services, and sort out those of category 'category';
        Map matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
                context, Service.class, true, false);
    
        List tabServices = new ArrayList(matchingBeans.values());
        List list = new ArrayList(tabServices);
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Service service = (Service) iter.next();
            if (service.getCategory() == null 
                    || !service.getCategory().equals(category)) 
                tabServices.remove(service);
        }
        Collections.sort(tabServices, new OrderComparator());
        return tabServices;
    	}
}
