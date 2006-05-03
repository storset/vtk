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
    
    public void testSimplePropertyQuery() {
        QueryNode node = parser.parse("a=b");

        assertEquals("Query", node.getNodeName());
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals(1, children.size());
        
        QueryNode child = (QueryNode)children.get(0);
        propertyValueQuery(child, null, "a", "=", "b");
    }

    public void testExistsQuery() {
        QueryNode node = parser.parse("p:r exists");
        
        assertEquals("Query", node.getNodeName());
        
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals(1, children.size());
        
        QueryNode child = (QueryNode)children.get(0);
        existsQuery(child, "p", "r", "exists");
    }

    public void testUriQuery() {
        QueryNode node = parser.parse("uri = /uri/* AND uri != /uri/lala");
        
        assertEquals("Query", node.getNodeName());
        
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals(1, children.size());
        
        QueryNode child = (QueryNode)children.get(0);
        assertEquals("AndQuery", child.getNodeName());
        
        children = child.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());

        // First uri query:
        
        child = (QueryNode)children.get(0);
        uriQuery(child, "=", "/uri/*");

        // Second uri query:
        child = (QueryNode)children.get(1);
        uriQuery(child, "!=", "/uri/lala");
        
    }
    
    public void testNameOrTypeQuery() {
        QueryNode node = parser.parse("name = la\\ * OR type != \"file\"");
        
        assertEquals("Query", node.getNodeName());
        
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals(1, children.size());
        
        QueryNode child = (QueryNode)children.get(0);
        orQuery(child);
        
        children = child.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());

        // First name query:
        
        child = (QueryNode)children.get(0);
        nameQuery(child, "=", "la *");

        // Second type query:
        child = (QueryNode)children.get(1);
        typeQuery(child, "!=", "file");
        
    }

    public void testQuery() {
        QueryNode node = parser.parse("(f:a!=\\ \\=122d3h AND (a=a || ee:e = value))");

        assertEquals("Query", node.getNodeName());
        
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals(1, children.size());
        
        QueryNode child = (QueryNode)children.get(0);
        andQuery(child);
        
        children = child.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());

        // First prop query:
        
        child = (QueryNode)children.get(0);
        propertyValueQuery(child, "f", "a", "!=", " =122d3h");

        // Or query:
        
        child = (QueryNode)children.get(1);
        orQuery(child);
        
        List children2 = child.getChildren();
        assertNotNull(children2);
        assertEquals(2, children2.size());
        
        QueryNode child2 = (QueryNode)children2.get(0);
        propertyValueQuery(child2, null, "a", "=", "a");
        
        child2 = (QueryNode)children2.get(1);
        propertyValueQuery(child2, "ee", "e", "=", "value");

    }

    
    protected String[] getConfigLocations() {
        return new String[] {"classpath:org/vortikal/repositoryimpl/queryparser/context.xml"};
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }    
   
    private void orQuery(QueryNode node) {
        assertNotNull(node);
        assertEquals("OrQuery", node.getNodeName());
    }

    private void andQuery(QueryNode node) {
        assertNotNull(node);
        assertEquals("AndQuery", node.getNodeName());
    }

    private void propertyValueQuery(QueryNode node, String prefix, String name, String comparator, String value) {
        assertEquals("PropertyValueQuery", node.getNodeName());
        
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

    private void uriQuery(QueryNode node, String comparator, String value) {
        assertEquals("UriQuery", node.getNodeName());
        
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());
        
        int i = 0;
        QueryNode child = (QueryNode)children.get(i);

        child = (QueryNode)children.get(i);
        assertEquals("UriComparator", child.getNodeName());
        assertEquals(comparator, child.getValue());
        i++;
        
        child = (QueryNode)children.get(i);
        assertEquals("Value", child.getNodeName());
        assertEquals(value, child.getValue());
    }

    private void nameQuery(QueryNode node, String comparator, String value) {
        assertEquals("NameQuery", node.getNodeName());
        
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());
        
        int i = 0;
        QueryNode child = (QueryNode)children.get(i);

        child = (QueryNode)children.get(i);
        assertEquals("Comparator", child.getNodeName());
        assertEquals(comparator, child.getValue());
        i++;
        
        child = (QueryNode)children.get(i);
        assertEquals("Value", child.getNodeName());
        assertEquals(value, child.getValue());
    }

    private void typeQuery(QueryNode node, String comparator, String value) {
        assertEquals("TypeQuery", node.getNodeName());
        
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());
        
        int i = 0;
        QueryNode child = (QueryNode)children.get(i);

        child = (QueryNode)children.get(i);
        assertEquals("Comparator", child.getNodeName());
        assertEquals(comparator, child.getValue());
        i++;
        
        child = (QueryNode)children.get(i);
        assertEquals("Value", child.getNodeName());
        assertEquals(value, child.getValue());
    }

    private void existsQuery(QueryNode node, String prefix, String name, 
            String comparator) {
        assertEquals("PropertyExistsQuery", node.getNodeName());
        
        List children = node.getChildren();
        assertNotNull(children);
        assertEquals((prefix == null) ? 2 : 3, children.size());
        
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
        assertEquals("Exists", child.getNodeName());
        assertEquals(comparator, child.getValue());
        i++;
        
    }


}
