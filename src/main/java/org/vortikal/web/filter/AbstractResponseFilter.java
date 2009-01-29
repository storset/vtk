package org.vortikal.web.filter;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractResponseFilter implements ResponseFilter {

    private int order = Integer.MAX_VALUE;
    private Set<?> categories = new HashSet<String>();

    public void setOrder(int order) {
        this.order = order;
    }
    
    public int getOrder() {
        return this.order;
    }
    
    public void setCategories(Set<?> categories) {
        this.categories = categories;
    }
    
    public Set<?> getCategories() {
        return this.categories;
    }

}
