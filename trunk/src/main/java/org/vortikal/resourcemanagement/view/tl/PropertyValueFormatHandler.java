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

import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.repository.resourcetype.ValueFormatterRegistry;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.ValNodeFactory;

public class PropertyValueFormatHandler implements ValNodeFactory.ValueFormatHandler {

    private ValueFormatterRegistry valueFormatterRegistry;
    
    public PropertyValueFormatHandler(
            ValueFormatterRegistry valueFormatterRegistry) {
        this.valueFormatterRegistry = valueFormatterRegistry;
    }

    public Object handleValue(Object val, String format, Context ctx) {
        if (val instanceof Value[]) {
            StringBuilder sb = new StringBuilder();
            Value[] values = (Value[]) val;
            for (int i = 0; i < values.length; i++) {
                ValueFormatter vf = this.valueFormatterRegistry.getValueFormatter(values[i].getType());
                sb.append(vf.valueToString(values[i], format, ctx.getLocale()));
                if (i < values.length - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        } else {
            Value value;
            if (val instanceof Property) {
                value = ((Property) val).getValue();
            } else if (val instanceof Value) {
                value = (Value) val;
            } else {
                throw new RuntimeException("Unknown type: " + val.getClass());
            }
            ValueFormatter vf = this.valueFormatterRegistry.getValueFormatter(value.getType());
            if (vf == null) {
                throw new RuntimeException("Unable to find value formatter for value " + value);
            }
            return vf.valueToString(value, format, ctx.getLocale());
        }
    }
}
