package org.vortikal.context;

import java.util.Set;

import junit.framework.TestCase;

public class CSVSetFactoryBeanTestCase extends TestCase {
    
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
