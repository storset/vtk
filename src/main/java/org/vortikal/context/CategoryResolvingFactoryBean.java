/* Copyright (c) 2005, 2006, 2007, University of Oslo, Norway
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;

/**
 * Factory bean looking up groups of objects based on the
 * Categorizable interface and Class type in the context.
 * 
 * Configurable JavaBean properties:
 * 
 * <ul>
 *   <li><code>category</code> - required {@link String} describing
 *   the category of the objects
 *   <li><code>clazz</code> - required bean {@link Class type}. Must
 *    implement {@link Categorizable}.  If the class implements {@link
 *    Ordered}, the returned set will be sorted.
 *   <li><code>comparator</code> - an optional {@link Comparator},
 *   which, if specified, is used for sorting the result set instead
 *   of the default {@link OrderComparator}.
 * </ul>
 */
@SuppressWarnings("unchecked")
public class CategoryResolvingFactoryBean extends AbstractFactoryBean implements InitializingBean {
    
    private String category;
    private Class clazz;
    private boolean ordered;
    private Comparator comparator;
    
    /**
     * Gets a list of objects declared to belong to a certain category
     * (this.category). The return value is an array of object with
     * type <code>this.clazz</code>
     *
     * @return a list of objects belonging to the category in
     * question. If no such objects exist, an empty list is returned.
     */
    @Override
    protected Object createInstance() {
        Map<String, Categorizable> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            (ListableBeanFactory) getBeanFactory(), this.clazz, true, false);
    
        List<Categorizable> result = new ArrayList<Categorizable>();
        for (Categorizable categorizable: matchingBeans.values()) {
            if (categorizable == null) {
                continue;
            }
            if (categorizable.getCategories() != null
                && categorizable.getCategories().contains(this.category)) {
                result.add(categorizable);
            }
        }

        if (this.comparator != null) {
            Collections.sort(result, this.comparator);
        } else if (this.ordered) {
            Collections.sort(result, new OrderComparator());
        }

        Object array = Array.newInstance(this.clazz, result.size());
        int n = 0;
        for (Object o: result) {
            Array.set(array, n++, o);
        }
        return array;
    }
    

    @Override
    public Class getObjectType() {
        if (this.clazz == null) return null;
        
        return Array.newInstance(this.clazz, 0).getClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Required
    public void setCategory(String category) {
        this.category = category;
    }

    @Required
    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public void setComparator(Comparator comparator) {
        this.comparator = comparator;
    }
    

    @Override
    public void afterPropertiesSet() throws Exception {
        if (! Categorizable.class.isAssignableFrom(this.clazz))
            throw new BeanInitializationException(
                "Property 'clazz' must be a class implementing Categorizable");
        this.ordered = Ordered.class.isAssignableFrom(this.clazz);

        super.afterPropertiesSet();
    }
}
