/* Copyright (c) 2005, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.vortikal.web.service.Service;



/**
 * Factory bean looking up groups of objects based on the Categorizable interface
 * and Class type in the context.
 * 
 * Configurable bean properties:
 * 
 * <ul>
 * <li><code>category</code> - required tring describing the category of the services
 * <li><code>class</code> - required bean type. Must implement Categorizable.
 *  If the class implements ordered, the returned set will be sorted.
 */
public class CategoryResolvingFactoryBean implements ApplicationContextAware, FactoryBean, InitializingBean {

    private ApplicationContext applicationContext;
    private String category;
    private Class clazz;
    private boolean ordered;
    /**
     * Gets a list of {@link Service services} declared to belong to a
     * certain category.
     *
     * @param category the category in question
     * @return a list of {@link Service} objects belonging to the
     * category in question. If no such services exist, an empty list
     * is returned.
     */
    private List getObjectsOfCategory(String category) {
        // find all services, and sort out those of category 'category';
        Map matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            applicationContext, clazz, true, false);
    
        List objects = new ArrayList(matchingBeans.values());
        List list = new ArrayList(objects);
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Categorizable categorizable = (Categorizable) iter.next();
            if (categorizable.getCategories() == null 
                || !categorizable.getCategories().contains(category)) 
                objects.remove(categorizable);
        }
        
        if (ordered)
            Collections.sort(objects, new OrderComparator());
        
        return objects;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Object getObject() throws Exception {
        return getObjectsOfCategory(category).toArray(new Object[0]);
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

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public void afterPropertiesSet() throws Exception {
        if (this.category == null) 
            throw new BeanInitializationException("Property 'category' must be specified");
        if (this.clazz == null)
            throw new BeanInitializationException("Property 'clazz' must be specified");
        if (! Categorizable.class.isAssignableFrom(clazz))
            throw new BeanInitializationException("Property 'clazz' must be a class implementing Categorizable");

        ordered = Ordered.class.isAssignableFrom(this.clazz);
        
    }
}
