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
package org.vortikal.resourcemanagement.view.tl;

import java.util.List;

import net.sf.json.JSONObject;

import org.vortikal.text.JSONUtil;
import org.vortikal.text.tl.Argument;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DefineNodeFactory;
import org.vortikal.text.tl.Symbol;

public class JSONAttributeHandler implements DefineNodeFactory.ValueProvider {

    // Supported constructions:
    // object "expression"
    // object expression (from variable)
    public Object create(List<Argument> tokens, Context ctx) throws Exception {
        if (tokens.size() != 2) {
            throw new Exception("Wrong number of arguments");
        }

        final Argument arg1 = tokens.get(0);
        Object object;
        if (!(arg1 instanceof Symbol)) {
            throw new Exception("First argument must be a symbol");
        }
        object = arg1.getValue(ctx);

        final Argument arg2 = tokens.get(1);
        String expression = arg2.getValue(ctx).toString();

        if (!(object instanceof JSONObject)) {
            throw new Exception("Cannot apply expression '" 
                    + expression + "' on object: not JSON data: "
                    + object.getClass());
        }

        JSONObject json = (JSONObject) object;
        return JSONUtil.select(json, expression);
    }
}
