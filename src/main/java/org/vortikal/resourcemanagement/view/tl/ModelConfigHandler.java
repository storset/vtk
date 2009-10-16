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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.vortikal.resourcemanagement.view.StructuredResourceDisplayController;
import org.vortikal.text.tl.Argument;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DefineNodeFactory;

public class ModelConfigHandler implements DefineNodeFactory.ValueProvider {

    @SuppressWarnings("unchecked")
    public Object create(List<Argument> tokens, Context ctx) throws Exception {
        if (tokens.size() < 1) {
            throw new RuntimeException("Expected at least one argument");
        }
        List<String> keys = new ArrayList<String>();
        for (Argument arg : tokens) {
            String key = arg.getValue(ctx).toString();
            keys.add(key);
        }
        Object o = ctx.get(StructuredResourceDisplayController.MVC_MODEL_KEY);
        if (o == null) {
            throw new RuntimeException("Unable to locate resource: no model: " 
                    + StructuredResourceDisplayController.MVC_MODEL_KEY);
        }
        Map<String, Object> model = (Map<String, Object>) o;
        if (!model.containsKey("config")) {
            throw new RuntimeException("No 'config' element present in model");
        }

        Map<String, Object> config = (Map<String, Object>) model.get("config");
        Map<String, Object> map = config;
        for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
            String key = iterator.next();
            Object obj = map.get(key);
            if (obj == null) {
                throw new RuntimeException("Unable to find value for " + keys);
            }
            if (iterator.hasNext()) {
                if (!(obj instanceof Map<?, ?>)) {
                    throw new RuntimeException("Unable to find value for " + keys);
                }
                map = (Map) obj;
            } else {
                return obj;
            }
        }
        throw new RuntimeException("Unable to find value for " + keys);
    }
}
