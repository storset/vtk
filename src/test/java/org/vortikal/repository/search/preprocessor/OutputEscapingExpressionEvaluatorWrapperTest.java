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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import static org.junit.Assert.*;
import org.junit.Test;


public class OutputEscapingExpressionEvaluatorWrapperTest {

    @Test
    public void escapeStringValue() {
        
        Mockery context = new Mockery();
        final ExpressionEvaluator dummy = context.mock(ExpressionEvaluator.class);
        context.checking(new Expectations() {
            {
                allowing(dummy).evaluate(with(any(String.class)));
                will(new CustomAction("echo 1st arg"){
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        return (String)invocation.getParameter(0);
                    }
                });
                allowing(dummy).matches(with(any(String.class)));
                will(returnValue(true));
            } 
        });
        
        OutputEscapingExpressionEvaluatorWrapper evaluator 
            = new OutputEscapingExpressionEvaluatorWrapper();
        evaluator.setWrappedEvaluator(dummy);
  
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