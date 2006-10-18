package org.vortikal.context;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Abstract superclass for CSV style factory beans. Parses a comma separated string
 * into an array of the individual elements. Optionally trims each string element.
 * 
 * If no CSV list is set by calling {@link #setCsvList(String)}, 
 * the array of elements will be of length 0 (not <code>null</code>).
 *
 */
public abstract class AbstractCSVFactoryBean extends AbstractFactoryBean {

    protected String[] elements = new String[0];
    
    private boolean trim = true;
    
    public void setCsvList(String csvList) {
        if (csvList != null) {
            this.elements = csvList.split(",");
            
            if (this.trim) {
                for (int i = 0; i < this.elements.length; i++) {
                    this.elements[i] = this.elements[i].trim();
                } 
            }
        } 
    }

    public void setTrim(boolean trim) {
        this.trim = trim;
    }

    protected abstract Object createInstance() throws Exception;

    public abstract Class getObjectType();

}
