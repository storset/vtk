/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.repository.search.preprocessor;

import junit.framework.TestCase;

import org.vortikal.repository.search.QueryException;

public class OutputEscapingExpressionEvaluatorWrapperTest extends TestCase {

    public void testEscapeStringValue() {
        
        OutputEscapingExpressionEvaluatorWrapper evaluator 
            = new OutputEscapingExpressionEvaluatorWrapper();
        evaluator.setWrappedEvaluator(new DummyExpressionEvaluator());
  
        assertEquals("/foo\\ bar/baz", 
                evaluator.evaluate("/foo bar/baz"));
        
        assertEquals("/foo\\ bar/\\<baz\\>", 
                evaluator.evaluate("/foo bar/<baz>"));
        
        assertEquals("/foo\\ bar/\\<baz\\>/backslash\\\\", 
                evaluator.evaluate("/foo bar/<baz>/backslash\\"));
        
        assertEquals("/foo/bar/baz", 
                evaluator.evaluate("/foo/bar/baz"));

        assertEquals("", 
                evaluator.evaluate(""));
        
        assertEquals("/foo/folder\\ \\(2\\)", 
                evaluator.evaluate("/foo/folder (2)"));
        
        assertEquals("\\\\",
                evaluator.evaluate("\\"));
        
        assertEquals("X",
                evaluator.evaluate("X"));
        
    }
    
}

class DummyExpressionEvaluator implements ExpressionEvaluator {

    public String evaluate(String token) throws QueryException {
        return token;
    }

    public boolean matches(String token) {
        return true;
    }
    
}