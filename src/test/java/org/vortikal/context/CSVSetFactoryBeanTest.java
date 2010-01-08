/* Copyright (c) 2006, University of Oslo, Norway
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

import java.util.Set;

import junit.framework.TestCase;

public class CSVSetFactoryBeanTest extends TestCase {
    
    public void testSingletonOrPrototype() throws Exception {
        CSVSetFactoryBean bean = new CSVSetFactoryBean();
        bean.setSingleton(true);
        bean.setCsvList("foo,bar");
        bean.afterPropertiesSet();
        
        assertTrue(bean.getObject() == bean.getObject());
        
        bean = new CSVSetFactoryBean();
        bean.setSingleton(false);
        bean.setCsvList("foo,bar");
        bean.afterPropertiesSet();
        
        assertFalse(bean.getObject() == bean.getObject());
    }
    
    @SuppressWarnings("unchecked")
    public void testCSVSetFactoryBean() throws Exception {
        
        // Test standard set with trimmed values
        CSVSetFactoryBean bean = new CSVSetFactoryBean();
        bean.setSingleton(true);
        bean.setCsvList("foo, bar ,mik ,mak  ,foo,  bar, foo, foo, foo bar mik mak  ");
        bean.afterPropertiesSet();
        
        assertEquals(Set.class, bean.getObjectType());
        
        Set csvSet = (Set) bean.getObject();
        assertEquals(5, csvSet.size());
        assertTrue(csvSet.contains("foo"));
        assertTrue(csvSet.contains("bar"));
        assertTrue(csvSet.contains("mik"));
        assertTrue(csvSet.contains("mak"));
        assertTrue(csvSet.contains("foo bar mik mak"));

        // Test with no set CSV list
        bean = new CSVSetFactoryBean();
        bean.afterPropertiesSet();

        csvSet = (Set) bean.getObject();
        assertEquals(0, csvSet.size());

        // Test with single element
        bean = new CSVSetFactoryBean();
        bean.setCsvList("single-element");
        bean.afterPropertiesSet();

        csvSet = (Set) bean.getObject();
        assertEquals(1, csvSet.size());
        
        // Test without trimming of values
        bean = new CSVSetFactoryBean();
        bean.setTrim(false);
        bean.setCsvList("  foo bar ,  bar  ,  mik,mak ");
        bean.afterPropertiesSet();
        
        csvSet = (Set)bean.getObject();
        assertTrue(csvSet.contains("  foo bar "));
        assertTrue(csvSet.contains("  bar  "));
        assertTrue(csvSet.contains("  mik"));
        assertTrue(csvSet.contains("mak "));

    }

}
