/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.text.tl;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;

public class ParserTest extends TestCase {

    private Map<String, DirectiveNodeFactory> directiveHandlers;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Map<String, DirectiveNodeFactory> directiveHandlers = new HashMap<String, DirectiveNodeFactory>();
        directiveHandlers.put("if", new IfNodeFactory());
        ValNodeFactory val = new ValNodeFactory();
        directiveHandlers.put("val", val);
        directiveHandlers.put("list", new ListNodeFactory());
        DefineNodeFactory def = new DefineNodeFactory();
        directiveHandlers.put("def", def);
        this.directiveHandlers = directiveHandlers;
    }
    
    public void testBasicSyntax() throws Exception {
        
        Context ctx = new Context(Locale.getDefault());

        String result = parseAndRender("[def x 22][val x]", ctx);
        assertEquals("22", result);
        
        result = parseAndRender("[]", ctx);
        assertEquals("[]", result);
        
        try {
            result = parseAndRender("[def x", ctx);
            throw new RuntimeException("Error: parser allows unterminated directive");
        } catch (Exception e) {
            // Expected
        }

        result = parseAndRender("[def x \"22\"][val x]", ctx);
        assertEquals("22", result);
        
        result = parseAndRender(" [def x 2][val x]", ctx);
        assertEquals(" 2", result);

        result = parseAndRender("\r\n[def x 2]\r[val x]", ctx);
        assertEquals("\n\n2", result);
        
        result = parseAndRender("\r\n[!--comment--]", ctx);
        assertEquals("\n", result);
        
        result = parseAndRender("\r\n[def x-x \"22\"]\r\n[def x-y x-x]\r\n[val x-y]", ctx);
        assertEquals("\n\n\n22", result);

        result = parseAndRender("[def x \"[ab\\\"c\\\"]\"][val x unescaped]", ctx);
        assertEquals("[ab\"c\"]", result);
        
        String template = 
            "[def foo \"bar]\"]"
            + "\n"
            + "[!-- comment\n [] --]" 
            + "\nsome text[ mixed #--with\n brackets and quotes ' '\n"
            + "[#-- [raw node] \n [[[[[[]]]]]] \n -- \n -- [this is not a directive]-->--]" 
            + "\n";

        NodeList nodeList = parse(template);
        List<Node> nodes = nodeList.getNodes();
        assertEquals(6, nodes.size());

        StringWriter writer = new StringWriter();
        nodes.get(0).render(ctx, writer);
        assertEquals("", writer.toString());
        
        writer = new StringWriter();
        nodes.get(1).render(ctx, writer);
        assertEquals("\n", writer.toString());
        
        writer = new StringWriter();
        nodes.get(2).render(ctx, writer);
        assertEquals("", writer.toString());

        writer = new StringWriter();
        nodes.get(3).render(ctx, writer);
        assertEquals("\nsome text[ mixed #--with\n brackets and quotes ' '\n", writer.toString());
        
        writer = new StringWriter();
        nodes.get(4).render(ctx, writer);
        assertEquals(" [raw node] \n [[[[[[]]]]]] \n -- \n -- [this is not a directive]-->", writer.toString());
        
        writer = new StringWriter();
        nodes.get(5).render(ctx, writer);
        assertEquals("\n", writer.toString());

        nodeList = parse("[#--<!--[if IE]>conditional comment<![endif]-->--]");
        nodes = nodeList.getNodes();
        assertEquals(1, nodes.size());
        writer = new StringWriter();
        nodes.get(0).render(ctx, writer);
        assertEquals("<!--[if IE]>conditional comment<![endif]-->", writer.toString());
    }
    
    public void testIf() throws Exception {
        Context ctx = new Context(Locale.getDefault());
        String result = parseAndRender("[if true]yes[else]no[endif]", ctx);
        assertEquals("yes", result);

        ctx.define("var1", true, true);
        result = parseAndRender("[if var1]yes[else]no[endif]", ctx);
        assertEquals("yes", result);

        ctx.define("var1", false, true);
        result = parseAndRender("[if var1]yes[else]no[endif]", ctx);
        assertEquals("no", result);

        ctx.define("var1", false, true);
        ctx.define("var2", true, true);
        result = parseAndRender("[if var1]var1[elseif var2]var2[endif]", ctx);
        assertEquals("var2", result);
        
        ctx.define("var1", false, true);
        ctx.define("var2", false, true);
        result = parseAndRender("[if var1]var1[elseif var2]var2[else]none[endif]", ctx);
        assertEquals("none", result);

        ctx.define("var1", false, true);
        ctx.define("var2", true, true);
        result = parseAndRender("[if var1]var1[elseif var2][if var1]var1[elseif var2]var2[endif][else]none[endif]", ctx);
        assertEquals("var2", result);
    }
    public void testDirectiveArgs() {
        List<Argument> args = parseDirective("[test arg1 \"arg2\" 100]");
        assertEquals(args.size(), 3);
        assertEquals(new Symbol("arg1"), args.get(0));
        assertEquals(new Literal("\"arg2\""), args.get(1));
        assertEquals(new Literal("100"), args.get(2));

        args = parseDirective("[test concat('foo', 'bar')]");
        assertEquals(6, args.size());
        assertEquals(new Symbol("concat"), args.get(0));
        assertEquals(new Symbol("("), args.get(1));
        assertEquals(new Literal("\"foo\""), args.get(2));
        assertEquals(new Symbol(","), args.get(3));
        assertEquals(new Literal("'bar'"), args.get(4));
        assertEquals(new Symbol(")"), args.get(5));
    }
    
    private String parseAndRender(String template, Context ctx) throws Exception {
        NodeList result = parse(template);
        StringWriter out = new StringWriter();
        result.render(ctx, out);
        return out.toString();
    }
    
    private NodeList parse(String template) throws RuntimeException {
        Reader reader = new StringReader(template);
        Parser parser = new Parser(reader, this.directiveHandlers);
        try {
            ParseResult result = parser.parse();
            return result.getNodeList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Argument> parseDirective(String template) {
        Map<String, DirectiveNodeFactory> directives = new HashMap<String, DirectiveNodeFactory>();
        List<Argument> tokens = new ArrayList<Argument>();
        
        TestNodeFactory nf = new TestNodeFactory(tokens);
        directives.put("*", nf);
        Reader reader = new StringReader(template);
        Parser parser = new Parser(reader, directives);
        try {
            parser.parse();
            return tokens;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class TestNodeFactory implements DirectiveNodeFactory {
        private List<Argument> tokenOutput;
        public TestNodeFactory(List<Argument> tokenOutput) {
            this.tokenOutput = tokenOutput;
        }
        @Override
        public Node create(DirectiveParseContext ctx) throws Exception {
            this.tokenOutput.addAll(ctx.getArguments());
            return new Node() {
                @Override
                public void render(Context ctx, Writer out) throws Exception {
                }
            };
        }
    }

}
