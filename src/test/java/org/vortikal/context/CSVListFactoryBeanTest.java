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

import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;

public class CSVListFactoryBeanTest {

    @Test
    public void singletonOrPrototype() throws Exception {
        CSVListFactoryBean bean = new CSVListFactoryBean();
        bean.setSingleton(true);
        bean.setCsvList("foo,bar");
        bean.afterPropertiesSet();
        
        assertTrue(bean.getObject() == bean.getObject());
        
        bean = new CSVListFactoryBean();
        bean.setSingleton(false);
        bean.setCsvList("foo,bar");
        bean.afterPropertiesSet();
        
        assertFalse(bean.getObject() == bean.getObject());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCSVListFactoryBean() throws Exception {
        
        // Test standard set with trimmed values
        CSVListFactoryBean bean = new CSVListFactoryBean();
        bean.setSingleton(true);
        bean.setCsvList("foo, bar ,mik ,mak  ,foo,  bar, foo, foo, foo bar mik mak  ");
        bean.afterPropertiesSet();
        
        assertEquals(List.class, bean.getObjectType());
        
        List csvList = (List) bean.getObject();
        assertEquals(9, csvList.size());
        assertEquals("foo", csvList.get(0));
        assertEquals("bar", csvList.get(1));
        assertEquals("mik", csvList.get(2));
        assertEquals("mak", csvList.get(3));
        assertEquals("foo", csvList.get(4));
        assertEquals("bar", csvList.get(5));
        assertEquals("foo", csvList.get(6));
        assertEquals("foo", csvList.get(7));
        assertEquals("foo bar mik mak", csvList.get(8));
        

        // Test with no set CSV list
        bean = new CSVListFactoryBean();
        bean.afterPropertiesSet();

        csvList = (List) bean.getObject();
        assertEquals(0, csvList.size());

        // Test with single element
        bean = new CSVListFactoryBean();
        bean.setCsvList("single-element");
        bean.afterPropertiesSet();

        csvList = (List) bean.getObject();
        assertEquals(1, csvList.size());
        assertEquals("single-element", csvList.get(0));
        
        // Test without trimming of values
        bean = new CSVListFactoryBean();
        bean.setTrim(false);
        bean.setCsvList("  foo bar ,  bar  ,  mik,mak ");
        bean.afterPropertiesSet();
        
        csvList = (List)bean.getObject();
        assertEquals(4, csvList.size());
        assertEquals("  foo bar ", csvList.get(0));
        assertEquals("  bar  ", csvList.get(1));
        assertEquals("  mik", csvList.get(2));
        assertEquals("mak ", csvList.get(3));

    }
    
}
