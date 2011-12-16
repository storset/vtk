/* Copyright (c) 2006 University of Oslo, Norway
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
package org.vortikal.web.service;

import java.util.Comparator;


/**
 * Comparator for sorting services based on the natural ordering of
 * a configurable service attribute.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>attributeName</code> - the name of the service
 *   attribute to sort on
 * </ul>
 *
 * @see Service#getAttribute
 */
public class ServiceAttributeComparator implements Comparator<Service> {

    private String attributeName;


    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int compare(Service service1, Service service2) {
        
        Object attr1 = service1.getAttribute(this.attributeName);
        Object attr2 = service2.getAttribute(this.attributeName);

        if (attr1.equals(attr2)) {
            return 0;
        }

        if (attr1 instanceof Comparable && attr2 instanceof Comparable) {
            Comparable c1 = (Comparable) attr1;
            Comparable c2 = (Comparable) attr2;
            return c1.compareTo(c2);
        }
        return 0;
    }

    
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(": serviceAttribute = ").append(this.attributeName);
        return sb.toString();
    }
    
}
