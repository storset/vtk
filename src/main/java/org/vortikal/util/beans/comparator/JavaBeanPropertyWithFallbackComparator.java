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

package org.vortikal.util.beans.comparator;


import java.util.Comparator;

import org.springframework.beans.BeanWrapperImpl;

/**
 * A java bean property comparator on a bean property with a fallback property 
 * in case of a null value for the first.
 * 
 * FIXME: Currently beans are required not to have <code>null</code> property values,
 * instead a toString value of "" is used.
 */
public class JavaBeanPropertyWithFallbackComparator implements Comparator {
    private String propertyName  = null;
    private String fallbackPropertyName = null;

    public JavaBeanPropertyWithFallbackComparator(String propertyName,
                                  String fallbackPropertyName) {
        this.propertyName  = propertyName;
        this.fallbackPropertyName = fallbackPropertyName;
    }

    public int compare(Object o1, Object o2) {
        BeanWrapperImpl wrapper1 = new BeanWrapperImpl(o1);
        BeanWrapperImpl wrapper2 = new BeanWrapperImpl(o2);

        Object propertyValue1 = wrapper1.getPropertyValue(this.propertyName);
//        if (propertyValue1 == null)
//            propertyValue1 = wrapper1.getPropertyValue(fallbackPropertyName);
        if (propertyValue1.toString().equals(""))
            propertyValue1 = wrapper1.getPropertyValue(this.fallbackPropertyName);

        Object propertyValue2 = wrapper2.getPropertyValue(this.propertyName);
//        if (propertyValue2 == null)
//            propertyValue2 = wrapper2.getPropertyValue(fallbackPropertyName);
        if (propertyValue2.toString().equals(""))
            propertyValue2 = wrapper2.getPropertyValue(this.fallbackPropertyName);

        if (!(propertyValue1 instanceof Comparable)) {
            throw new IllegalArgumentException(
                    "Cannot compare object " + o1 + " and " + o2 + ". Field '"
                    + this.propertyName + "' (class " 
                    + propertyValue1.getClass().getName() + ") does not implement "
                    + Comparable.class.getName());
        }

        return ((Comparable) propertyValue1).compareTo(propertyValue2);
    }
}