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
package org.vortikal.text.tl;

import java.util.Arrays;
import java.util.Locale;

import junit.framework.TestCase;

public class ExpressionTestCase extends TestCase {


    public void testBasicExpressions() {

        Object result;

        result = eval(new Argument[] {
                new Literal("\"literal-string\"")	
        });
        assertEquals("literal-string", result);

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
    }
    
    public void testComplexExpressions() {
    
        Object result = eval(new Argument[] {
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
        assertEquals(false, result);
    }

    private Object eval(Argument... args ) {
        Expression expr = new Expression(Arrays.asList(args));
        Context ctx = new Context(Locale.getDefault());
        return expr.evaluate(ctx);
    }
}
