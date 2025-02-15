/* Copyright (c) 2007, University of Oslo, Norway
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
package vtk.beans.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;

/**
 * FactoryBean that exposes a list of target beans of a specified class existing
 * in the application context.
 * <p>
 * Note: If used to populate a bean of the specified class, the bean will have a
 * recursive reference to itself
 * 
 * @see #setTargetBeansClass
 */
public class BeansListingFactoryBean<T> implements FactoryBean,
        ApplicationContextAware {

    private Class<T> targetBeansClass;

    private ApplicationContext applicationContext;

    /**
     * Set the class of the target beans.
     * <p>
     * This property is required.
     * 
     * @param targetBeansClass
     *            the class of the target beans
     */
    @Required
    public void setTargetBeansClass(Class<T> targetBeansClass) {
        this.targetBeansClass = targetBeansClass;
    }

    @Required
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;

    }

    @SuppressWarnings("unchecked")
    public List<T> getObject() throws BeansException {
        Map<?, T> matchingBeans = BeanFactoryUtils
                .beansOfTypeIncludingAncestors(this.applicationContext,
                        targetBeansClass, true, false);

        List<T> beans = new ArrayList<T>(matchingBeans.values());
        Collections.sort(beans, new OrderComparator());
        return beans;
    }

    @SuppressWarnings("unchecked")
    public Class getObjectType() {
        return List.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public boolean isPrototype() {
        return false;
    }

    public boolean isEagerInit() {
        return false;
    }

}
