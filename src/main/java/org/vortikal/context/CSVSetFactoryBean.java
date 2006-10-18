package org.vortikal.context;

import java.util.HashSet;
import java.util.Set;

/**
 * Create a <code>java.util.Set</code> from a CSV list.
 *
 */
public class CSVSetFactoryBean extends AbstractCSVFactoryBean {

    /**
     * Create a new <code>Set</code> instance on every call in case the 
     * <code>FactoryBean</code> is not a singleton, but a prototype.
     */
    protected Object createInstance() throws Exception {
        Set csvSet = new HashSet();
        
        for (int i=0; i<super.elements.length; i++) {
            csvSet.add(super.elements[i]);
        }
        
        return csvSet;
    }

    public Class getObjectType() {
        return Set.class;
    }

}
