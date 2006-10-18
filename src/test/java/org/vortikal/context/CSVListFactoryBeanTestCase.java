package org.vortikal.context;

import java.util.List;

import junit.framework.TestCase;

public class CSVListFactoryBeanTestCase extends TestCase {

    public void testSingletonOrPrototype() throws Exception {
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
