/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.text.tl.expr;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import junit.framework.TestCase;

import org.vortikal.text.tl.Argument;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.Literal;
import org.vortikal.text.tl.Symbol;

public class ExpressionTest extends TestCase {

    private Set<Function> functions = new HashSet<Function>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Symbol s = new Symbol("concat");
        this.functions.add(new Concat(s));
        s = new Symbol("emptyfunc");
        this.functions.add(new Function(s, 0) {
            public Object eval(Context ctx, Object... args) {
                return "result";
            }});
    }

    public void testBasicExpressions() {
        Object result;
        result = eval(new Argument[] {
                new Literal("\"literal-string\"")	
        });
        assertEquals("literal-string", result);

        result = eval(new Argument[] {
                new Symbol("null")
        });
        assertNull(result);

        result = eval(new Argument[] {
                new Symbol("null"),
                new Symbol("="),
                new Symbol("null")
        });
        assertEquals(true, result);

        result = eval(new Argument[] {
                new Literal("true")
        });
        assertEquals(true, result);

        result = eval(new Argument[] {
                new Symbol("!"),
                new Literal("true")
        });
        assertEquals(false, result);

        result = eval(new Argument[] {
                new Literal("774")	
        });
        assertEquals(774, result);

        result = eval(new Argument[] {
                new Literal("-774")	
        });
        assertEquals(-774, result);

        result = eval(new Argument[] {
                new Literal("2"),
                new Symbol("+"),
                new Literal("2")
        });
        assertEquals(4, result);

        result = eval(new Argument[] {
                new Literal("2"),
                new Symbol("-"),
                new Literal("2")
        });
        assertEquals(0, result);

        result = eval(new Argument[] {
                new Literal("2"),
                new Symbol("-"),
                new Literal("-2")
        });
        assertEquals(4, result);

        result = eval(new Argument[] {
                new Literal("2"),
                new Symbol("*"),
                new Literal("2")
        });
        assertEquals(4, result);

        result = eval(new Argument[] {
                new Literal("2"),
                new Symbol("/"),
                new Literal("2")
        });
        assertEquals(1, result);

        result = eval(new Argument[] {
                new Literal("2.2"),
                new Symbol("/"),
                new Literal("2")
        });
        assertEquals(1.1F, result);
    }

    public void testComplexExpressions() {
        Object result;
        result = eval(new Argument[] {
                new Literal("2"),
                new Symbol("+"),
                new Literal("2"),
                new Symbol("*"),
                new Literal("2")
        });
        assertEquals(6, result);

        result = eval(new Argument[] {
                new Literal("2"),
                new Symbol("+"),
                new Literal("3"),
                new Symbol("+"),
                new Literal("4"),
                new Symbol("/"),
                new Literal("2")
        });
        assertEquals(7, result);

        result = eval(new Argument[] {
                new Symbol("("),
                new Literal("2"),
                new Symbol("+"),
                new Literal("4"),
                new Symbol(")"),
                new Symbol("*"),
                new Literal("2")
        });
        assertEquals(12, result);

        result = eval(new Argument[] {
                new Symbol("("),
                new Literal("2"),
                new Symbol("+"),
                new Literal("4"),
                new Symbol(")"),
                new Symbol("="),
                new Literal("6")
        });
        assertEquals(true, result);

        result = eval(new Argument[] {
                new Literal("2"),
                new Symbol("+"),
                new Literal("4"),
                new Symbol("="),
                new Literal("3"),
                new Symbol("||"),
                new Literal("2"),
                new Symbol("*"),
                new Literal("2"),
                new Symbol("="),
                new Literal("4")
        });
        assertEquals(true, result);

        result = eval(new Argument[] {
                new Literal("2"),
                new Symbol("+"),
                new Literal("4"),
                new Symbol("="),
                new Literal("3"),
                new Symbol("&&"),
                new Literal("2"),
                new Symbol("*"),
                new Literal("2"),
                new Symbol("="),
                new Literal("4")
        });
    }

    public void testFunctions() {
        Context ctx = new Context(Locale.getDefault());
        ctx.define("a", "a", true);
        ctx.define("b", "b", true);
        ctx.define("c", "c", true);

        Object result;
        result = eval(new Argument[] {
                new Symbol("concat"),
                new Symbol("("),
                new Literal("'a'"),
                new Symbol(","),
                new Symbol("concat"),
                new Symbol("("),
                new Literal("'b'"),
                new Symbol(","),
                new Literal("'c'"),
                new Symbol(")"),
                new Symbol(")")
        });
        assertEquals("abc", result);


        result = eval(ctx, new Argument[] {
                new Symbol("concat"),
                new Symbol("("),
                new Symbol("a"),
                new Symbol(","),
                new Symbol("concat"),
                new Symbol("("),
                new Symbol("b"),
                new Symbol(","),
                new Symbol("c"),
                new Symbol(")"),
                new Symbol(")")
        });
        assertEquals("abc", result);

        result = eval(new Argument[] {
                new Symbol("concat"),
                new Symbol("("),
                new Symbol("concat"),
                new Symbol("("),
                new Literal("'a'"),
                new Symbol(","),
                new Literal("'b'"),
                new Symbol(")"),
                new Symbol(","),
                new Literal("'c'"),
                new Symbol(")")
        });
        assertEquals("abc", result);

        result = eval(ctx, new Argument[] {
                new Symbol("concat"),
                new Symbol("("),
                new Symbol("a"),
                new Symbol(","),
                new Symbol("concat"),
                new Symbol("("),
                new Symbol("concat"),
                new Symbol("("),
                new Symbol("b"),
                new Symbol(","),
                new Symbol("c"),
                new Symbol(")"),
                new Symbol(","),
                new Symbol("a"),
                new Symbol(")"),
                new Symbol(")")
        });
        assertEquals("abca", result);

        result = eval(ctx, new Argument[] {
                new Symbol("emptyfunc"),
                new Symbol("("),
                new Symbol(")")
        });

        result = eval(ctx, new Argument[] {
                new Symbol("concat"),
                new Symbol("("),
                new Symbol("emptyfunc"),
                new Symbol("("),
                new Symbol(")"),
                new Symbol(","),
                new Literal("'ing'"),
                new Symbol(")")
        });
        assertEquals("resulting", result);
    }

    public void testUndefinedFunction() throws Exception {
        try {
            eval(new Argument[] {
                    new Symbol("bazza"),
                    new Symbol("("),
                    new Symbol("a"),
                    new Symbol(","),
                    new Symbol("b"),
                    new Symbol(","),
                    new Symbol("c"),
                    new Symbol(")")
            });
            fail("Should not succeed");
        } catch (RuntimeException e) {
            // Expected
        }
    }

    public void testTooManyArguments() throws Exception {
        try {
            eval(new Argument[] {
                    new Symbol("concat"),
                    new Symbol("("),
                    new Symbol("a"),
                    new Symbol(","),
                    new Symbol("b"),
                    new Symbol(","),
                    new Symbol("c"),
                    new Symbol(")")
            });
            fail("Should not succeed");
        } catch (RuntimeException e) {
            // Expected
        }
    }

    public void testMalformedExpressions() throws Exception {
        try {
            eval(new Argument[] {
                    new Symbol(","),
                    new Symbol("("),
                    new Literal("2"),
                    new Symbol("+")
            });
            fail("Should not succeed");
        } catch (RuntimeException e) {
            // Expected
        }

        try {
            eval(new Argument[] {
                    new Symbol("concat"),
                    new Literal("2"),
                    new Symbol(","),
                    new Literal("2")
            });
            fail("Should not succeed");
        } catch (RuntimeException e) {
            // Expected
        }

        try {
            eval(new Argument[] {
                    new Symbol("concat"),
                    new Symbol("("),
                    new Literal("'a'"),
                    new Symbol(","),
                    new Symbol("concat"),
                    new Symbol("("),
                    new Literal("'b'"),
                    new Symbol(","),
                    new Literal("'c'"),
                    new Symbol(")")
            });
            fail("Should not succeed");
        } catch (RuntimeException e) {
            // Expected
        }
    }

    private Object eval(Argument... args) {
        Context ctx = new Context(Locale.getDefault());
        return eval(ctx, args);
    }

    private Object eval(Context ctx, Argument... args) {
        Expression expr = new Expression(this.functions, Arrays.asList(args));
        return expr.evaluate(ctx);
    }
}
