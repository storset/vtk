package org.vortikal;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Abstract testcase which get the configuration for the applicationContext from a String which can be usefull f.x.
 * for use with {@link no.uio.usit.miyagi.Miyagi}
 * 
 * @author kajh
 * 
 */
public abstract class AbstractDependencyInjectionSpringStringContextTests extends
        AbstractDependencyInjectionSpringContextTests {

    private static Logger logger = Logger
            .getLogger(AbstractDependencyInjectionSpringStringContextTests.class);

    static {
        BasicConfigurator.configure();
        Category.getRoot().setLevel(Level.DEBUG);
    }
    
    /**
     * 
     * @return The configuraton for the applicationContext as a String
     */
    protected abstract String getConfigAsString();

    /** 
     * Not in use. We use {@link #getConfigAsString} to get hold of the configuration
     */
    protected String[] getConfigLocations() {
        return null;
    }
    
    protected ConfigurableApplicationContext getContext(Object key) {
        Resource configResource = new ByteArrayResource(getConfigAsString().getBytes());
        ConfigurableApplicationContext context = new ResourceXmlApplicationContext(configResource);
        context.refresh();
        return context;
    }

}
