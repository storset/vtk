
package org.vortikal.web.service;

import java.util.Set;



public interface Categorizable {

    /**
     * Gets this service's category. Categories are used to classify
     * and group services together. 
     * 
     * @return the category of this service
     */
    public Set getCategories();

}