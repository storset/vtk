package org.vortikal.repository.resource;

import java.io.IOException;
import java.net.URL;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Lexer;
import org.junit.Test;

public class ResourceGrammarTest {
        
    @Test
    public void testResourceGrammer() throws IOException {
        URL url = this.getClass().getResource("resource-types.txt");
        String testfile = url.toString();
        testfile = testfile.substring(testfile.indexOf(":") + 1);
        Lexer lexer = new ResourceLexer();
        lexer.setCharStream(new ANTLRFileStream(testfile));
        CommonTokenStream tokens = new CommonTokenStream();
        tokens.setTokenSource(lexer);
        tokens.LT(1); // force load
        ResourceParser parser = new ResourceParser(tokens);
    }

}
