package org.vortikal.web.view.freemarker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/** JavaBean to configure FreeMarker for web usage, mirroring Spring's 
 *  <code>FreeMarkerConfigurer</code>, differing only in the way template paths are resolved.
 *  
 *  This bean look up beans implementing the <code>FreeMarkerTemplateLocation</code> interface,
 *  making a <code>MultiTemplateLoader</code> containing all the found template locations.
 *  
 *  Delegates the actual configuration creation to Spring's <code>FreemarkerConfigurationFactory</code> 
 *  (the super class of <code>FreeMarkerConfigurer</code>) 
 *  
 *  @see org.springframework.ui.freemarker.FreeMarkerConfigurer
 *  @see org.springframework.ui.freemarker.FreeMarkerConfigurationFactory
 *  @see freemarker.cache.MultiTemplateLoader
 *  @see FreeMarkerTemplateLocation
 */
public class MultiTemplateLocationsFreeMarkerConfigurer implements FreeMarkerConfig, 
    InitializingBean, ApplicationContextAware {

    private ApplicationContext applicationContext;
    
    private Resource configLocation;

    private Properties freemarkerSettings;

    private Map freemarkerVariables;

    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    private boolean preferFileSystemAccess = true;

    private Configuration configuration;
    

    
    public Configuration getConfiguration() {
        return this.configuration;
    }

    public void afterPropertiesSet() throws Exception {
        if (this.configuration == null) {
            this.configuration = createConfiguration();
        }
    }
    
    public Configuration createConfiguration() throws IOException, TemplateException {
        List loaders = new ArrayList();
        
        FreeMarkerConfigurationFactory factory = new FreeMarkerConfigurationFactory();
        
        factory.setPreferFileSystemAccess(this.preferFileSystemAccess);
        
        factory.setResourceLoader(this.resourceLoader);

        try {
            Map templateLocations = 
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(
                            applicationContext, FreeMarkerTemplateLocation.class, true, false);

            if (templateLocations.isEmpty()) 
                throw new BeanCreationException("Couldn't find any beans implementing " 
                        + FreeMarkerTemplateLocation.class.getName()
                        + " in the application context");
                
            for (Iterator iter = templateLocations.values().iterator(); iter.hasNext();) {
                FreeMarkerTemplateLocation location = (FreeMarkerTemplateLocation) iter.next();

                factory.setTemplateLoaderPath(location.getLocation());
                loaders.add(factory.createConfiguration().getTemplateLoader());
            }
            
            loaders.add(new ClassTemplateLoader(FreeMarkerConfigurer.class));
            
            TemplateLoader[] templateLoaders = (TemplateLoader[]) loaders.toArray(new TemplateLoader[0]);
            MultiTemplateLoader loader = new MultiTemplateLoader(templateLoaders);

            factory = new FreeMarkerConfigurationFactory();
            
            if (this.configLocation != null)
                factory.setConfigLocation(this.configLocation);
            
            if (this.freemarkerSettings != null)
                factory.setFreemarkerSettings(this.freemarkerSettings);
            
            if (this.freemarkerVariables != null)
                factory.setFreemarkerVariables(this.freemarkerVariables);
            
            Configuration configuration = factory.createConfiguration();
            configuration.setTemplateLoader(loader);

            return configuration;

        } catch (NoSuchBeanDefinitionException ex) {
            throw new ApplicationContextException(
                    "Must define a single FreeMarkerConfig bean in this web application context " +
                    "(may be inherited): FreeMarkerConfigurer is the usual implementation. " +
                    "This bean may be given any name.", ex);
        }
        
    
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    
    public void setConfigLocation(Resource resource) {   
        this.configLocation = resource;
    }

    public void setFreemarkerSettings(Properties settings) {
        this.freemarkerSettings = settings;
    }

    public void setFreemarkerVariables(Map variables) {
        this.freemarkerVariables = variables;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setPreferFileSystemAccess(boolean preferFileSystemAccess) {
        this.preferFileSystemAccess = preferFileSystemAccess;
    }

     
}
