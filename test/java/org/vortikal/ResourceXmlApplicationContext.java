package org.vortikal;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;

public class ResourceXmlApplicationContext extends AbstractXmlApplicationContext {

    private Resource resource;
    
    private ResourceXmlApplicationContext() {
        throw new IllegalArgumentException("This class can't be construced without a Resource as an argument");
    }
    
    public ResourceXmlApplicationContext(Resource resource) {
        System.out.println("InputStreamXmlApplicationContext constructor");
        this.resource = resource;
    }
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
        System.out.println("initBeanDefinitionReader starter");
        beanDefinitionReader.loadBeanDefinitions(resource);
    }

    /**
     * Not in use since the constructor get the Resource needed to set up this applicationContext
     */
    protected String[] getConfigLocations() {
        return null;
    }



}
