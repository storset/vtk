package org.vortikal.repositoryimpl.queryparser;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

public class QueryParserTest extends AbstractDependencyInjectionSpringContextTests {

    private static Log logger = LogFactory.getLog(QueryParserTest.class);
    
    private Parser parser;
    
    static {
        BasicConfigurator.configure();
    }
    
    public void testPropertyQuery() {
        QueryNode node = parser.parse("a=b");

        assertEquals("Query", node.getNodeName());
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals(1, children.size());
        
        QueryNode child = (QueryNode)children.get(0);
        assertEquals("PropertyQuery", child.getNodeName());
        children = child.getChildren();
        assertNotNull(children);
        assertEquals(3, children.size());

        child = (QueryNode)children.get(0);
        assertEquals("Name", child.getNodeName());
        assertEquals("a", child.getValue());
        
        child = (QueryNode)children.get(1);
        assertEquals("Comparator", child.getNodeName());
        assertEquals("=", child.getValue());
        
        child = (QueryNode)children.get(2);
        assertEquals("Value", child.getNodeName());
        assertEquals("b", child.getValue());
    }

    public void testQuery() {
        QueryNode node = parser.parse("(f:a!=\\ \\=122d3h AND (a=a || ee:e = value))");

        assertEquals("Query", node.getNodeName());
        
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals(1, children.size());
        
        QueryNode child = (QueryNode)children.get(0);
        assertEquals("AndQuery", child.getNodeName());
        
        children = child.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());

        // First prop query:
        
        child = (QueryNode)children.get(0);
        propertyQuery(child, "f", "a", "!=", " =122d3h");

        // Or query:
        
        child = (QueryNode)children.get(1);
        orQuery(child);
        
        List children2 = child.getChildren();
        assertNotNull(children2);
        assertEquals(2, children2.size());
        
        QueryNode child2 = (QueryNode)children2.get(0);
        propertyQuery(child2, null, "a", "=", "a");
        
        child2 = (QueryNode)children2.get(1);
        propertyQuery(child2, "ee", "e", "=", "value");

    }
    protected String[] getConfigLocations() {
        return new String[] {"classpath:org/vortikal/repositoryimpl/queryparser/context.xml"};
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }    
   
    private void orQuery(QueryNode node) {
        assertEquals("OrQuery", node.getNodeName());
    }

    private void propertyQuery(QueryNode node, String prefix, String name, String comparator, String value) {
        assertEquals("PropertyQuery", node.getNodeName());
        
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals((prefix == null) ? 3 : 4, children.size());
        
        int i = 0;
        QueryNode child = (QueryNode)children.get(i);

        if (prefix != null) {
            assertEquals("Prefix", child.getNodeName());
            assertEquals(prefix, child.getValue());
            i++;
        }
        
        child = (QueryNode)children.get(i);
        assertEquals("Name", child.getNodeName());
        assertEquals(name, child.getValue());
        i++;
        
        child = (QueryNode)children.get(i);
        assertEquals("Comparator", child.getNodeName());
        assertEquals(comparator, child.getValue());
        i++;
        
        child = (QueryNode)children.get(i);
        assertEquals("Value", child.getNodeName());
        assertEquals(value, child.getValue());
        
    }
}
