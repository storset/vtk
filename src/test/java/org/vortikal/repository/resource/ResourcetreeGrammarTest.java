package org.vortikal.repository.resource;

import java.io.IOException;
import java.net.URL;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.junit.Assert;
import org.junit.Test;

public class ResourcetreeGrammarTest {

    @Test
    public void testResourcetreeGrammer() throws IOException, RecognitionException {
        ResourcetreeParser parser = createParser("resourcetree.vrtx");
        parser.resources();
        Assert.assertEquals(0, parser.getNumberOfSyntaxErrors());
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
