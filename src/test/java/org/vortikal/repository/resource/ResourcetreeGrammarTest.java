package org.vortikal.repository.resource;

import java.io.IOException;
import java.net.URL;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.junit.Assert;
import org.junit.Test;

public class ResourcetreeGrammarTest {

    @Test
    public void testResourcetreeGrammer() throws IOException, RecognitionException {
        ResourcetreeParser parser = createParser("resourcetree.vrtx");
        ResourcetreeParser.resources_return resources = parser.resources();
        Assert.assertEquals(0, parser.getNumberOfSyntaxErrors());
        CommonTree resourcetree = (CommonTree) resources.tree;
        Assert.assertNotNull(resourcetree);
        Assert.assertTrue(resourcetree.getChildren().size() > 0);
    }

    private ResourcetreeParser createParser(String filename) throws IOException {
        String filepath = getResourceFilePath(filename);
        ResourcetreeLexer lexer = new ResourcetreeLexer(new ANTLRFileStream(filepath));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ResourcetreeParser parser = new ResourcetreeParser(tokens);
        return parser;
    }

    private String getResourceFilePath(String filename) {
        URL url = this.getClass().getResource(filename);
        String testfile = url.toString();
        testfile = testfile.substring(testfile.indexOf(":") + 1);
        return testfile;
    }

}
