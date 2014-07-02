/* Copyright (c) 2014, University of Oslo, Norway
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

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.vortikal.text.tl.Parser.Directive;

public class CaptureHandler implements DirectiveHandler {

    public String[] tokens() {
        return new String[] { "capture", "endcapture" };
    }
    
    @Override
    public void directive(Directive directive, TemplateContext context) {
        String name = directive.name();
        
        if ("capture".equals(name)) {
            context.push(new DirectiveState(directive));
            return;
        }
        if ("endcapture".equals(name)) {
            DirectiveState state = context.pop();
            if (state == null || !"capture".equals(state.directive().name())) {
                context.error("Misplaced directive: endcapture");
                return;
            }
            List<Token> args = state.directive().args();

            if (args.size() != 1) {
                context.error("Capture directive takes one argument");
                return;
            }
            Token arg1 = args.get(0);
            if (!(arg1 instanceof Symbol)) {
                context.error("Expected symbol: " + arg1.getRawValue());
                return;
            }
            final String variable = ((Symbol) arg1).getSymbol();
            final NodeList nodeList = state.nodes();
            
            context.add(new Node() {
                @Override
                public boolean render(Context ctx, Writer out) throws Exception {
                    StringWriter buffer = new StringWriter();
                    nodeList.render(ctx, buffer);
                    ctx.define(variable, buffer.getBuffer().toString(), true);
                    return true;
                }
            });

        }
    }
}